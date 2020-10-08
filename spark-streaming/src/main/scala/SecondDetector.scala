import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.UUID

import FirstDetector._
import ToolsConfig._
import com.redis.RedisClientPool
import net.liftweb.json.DefaultFormats
import org.apache.spark.sql.cassandra._
import org.apache.spark.sql.{DataFrame, ForeachWriter, Row, SaveMode}
import org.apache.spark.sql.functions.{count, from_json, sum, window}
import org.apache.spark.sql.types.{StringType, StructField, StructType, TimestampType}
import org.apache.spark.streaming
import org.apache.spark.streaming.StreamingContext
import spark.implicits._

object SecondDetector {


  implicit val formats = new DefaultFormats {
    override def dateFormatter: SimpleDateFormat = new SimpleDateFormat()
  }

  case class LogsWithTimestamp(ip: String, eventType: String, time: Timestamp)

  case class EventDetailsWithTimestamp(ip: String, clickRate: Long,
                                       transitionsRate: Long, requestsCount: Long,
                                       time: Timestamp) {
    override def toString: String =
      s"ip: $ip, clickRate: $clickRate, transitionsRate: $transitionsRate, requestsCount: $requestsCount"
  }


  /**
   * If a click is made, the number of clicks and the number of requests increases,
   * otherwise the number of transitions and requests increases
   *
   * @param log
   * @return
   */
  def checkEventType(log: LogsWithTimestamp): EventDetailsWithTimestamp = {
    if (log.eventType.equals("click"))
      EventDetailsWithTimestamp(log.ip, 1, 0, 1, log.time)
    else
      EventDetailsWithTimestamp(log.ip, 0, 1, 1, log.time)
  }

  def main(args: Array[String]): Unit = {

    val streamingContext = new StreamingContext(spark.sparkContext, streaming.Seconds(batchInterval))

    //Receive events
    val kafkaStreaming =
      spark.readStream
        .format("kafka")
        .option("kafka.bootstrap.servers", kafkaHost)
        .option("subscribe", kafkaTopic)
        .load()
        .select("value") //value column from kafka
        .as[String]
        .filter(!_.isEmpty)
        .select(from_json($"value".cast(StringType),
          StructType(Seq(
            StructField("ip", StringType, true),
            StructField("eventType", StringType, true),
            StructField("time", TimestampType, true)
          ))).as("parsed_json"))
        .select("parsed_json.*")
        .as[LogsWithTimestamp]

    val stream = kafkaStreaming
      .map(checkEventType)
      .withWatermark("time", s"${watermark} seconds")
      //Grouping by ip and then aggregation by number of actions
      .groupBy($"ip", window($"time",
        s"${windowSize} seconds",
        s"${windowSize} seconds"))
      .agg(
        count("*").as("requestsCount"),
        sum("clickRate").as("clickRate"),
        sum("transitionsRate").as("transitionsRate")
      )

    //Spark logs stream
    stream.writeStream
      .outputMode("append")
      .format("console")
      .option("truncate", value = false)
      .queryName("rate-console")
      .start

    object RedisConnection {
      lazy val pool: RedisClientPool = new RedisClientPool(redisHost, redisPort)
    }

//    stream
//      .writeStream
//      .outputMode("append")
//      .foreachBatch{
//        (batch, id: Long) =>
//          batch
//            .write
//            .cassandraFormat("sStreaming", "detector")
//            .mode(SaveMode.Append)
//            .save()
//      }
//      .start

    stream
      .as[EventDetails]
      .writeStream
      .outputMode("append")
      .foreach(new ForeachWriter[EventDetails] {
        override def open(partitionId: Long, version: Long): Boolean = true

        override def close(errorOrNull: Throwable): Unit = {}

        override def process(value: EventDetails): Unit = {
          RedisConnection.pool.withClient {
            client => {
              client.set(s"structuredStats${value.ip}",
                value.toString, onlyIfExists = false, com.redis.Seconds(redisStatsTimeToDrop))
            }
          }
        }
      })
      .start

    stream
      .as[EventDetails]
      .filter(data => data.requestsCount > requestsCount)
      .writeStream
      .outputMode("append")
      .foreach(new ForeachWriter[EventDetails] {
        override def open(partitionId: Long, version: Long): Boolean = true

        override def close(errorOrNull: Throwable): Unit = {}

        override def process(value: EventDetails): Unit = {
          RedisConnection.pool.withClient {
            client => {
              client.set(s"structuredBots${value.ip}", value.toString,
                onlyIfExists = false, com.redis.Seconds(redisStatsTimeToDrop))
            }
          }

        }
      })
      .start.awaitTermination

    streamingContext.start()
    streamingContext.awaitTermination()


  }

}
