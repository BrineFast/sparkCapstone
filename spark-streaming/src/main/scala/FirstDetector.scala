import ToolsConfig._
import org.apache.spark.streaming
import com.redislabs.provider.redis.toRedisContext
import net.liftweb.json._
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{HasOffsetRanges, KafkaUtils, OffsetRange}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent


/**
 * DStream implementation
 */
object FirstDetector {

  case class Logs(ip: String, time: Long, eventType: String)

  case class EventDetails(ip: String, clickRate: Long, transitionsRate: Long, requestsCount: Long) {
    override def toString: String = s"ip: $ip, clickRate: $clickRate, transitionsRate: $transitionsRate, requestsCount: $requestsCount"
  }

  implicit val extractionFormat: DefaultFormats.type = DefaultFormats

  /**
   * If a click is made, the number of clicks and the number of requests increases,
   * otherwise the number of transitions and requests increases
   *
   * @param log
   * @return
   */
  def checkEventType(log: Logs): EventDetails = {
    if (log.eventType.equals("click"))
      EventDetails(log.ip, 1, 0, 1)
    else
      EventDetails(log.ip, 0, 1, 1)
  }

  /**
   * Magick 0-0
   *
   * @param args
   */
  def main(args: Array[String]): Unit = {

    val streamingContext = new StreamingContext(spark.sparkContext, streaming.Seconds(batchInterval))

    val kafkaStreaming = KafkaUtils.createDirectStream[String, String](
      streamingContext,
      PreferConsistent,
      Subscribe[String, String](Array(kafkaTopic), kafkaProps))

    //Kafka streams to rdd
    var offsetRanges = Array[OffsetRange]()
    val rdd = kafkaStreaming.transform { rdd =>
      offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
      rdd
    }

    //Extracting events from the json
    val eventsInfoStream = rdd.filter(_.value().nonEmpty)
      .map(json => parse(json.value()).extract[Logs]) //Extracting logs
      .map(data => (data.ip, checkEventType(data))) //Moving logs to map[ip, Event]
      //All actions from a single ip that fall into this window are summed up
      .reduceByKeyAndWindow((sumEvents: EventDetails, event: EventDetails) =>
        EventDetails(sumEvents.ip,
          sumEvents.clickRate + event.clickRate,
          sumEvents.transitionsRate + event.transitionsRate,
          sumEvents.requestsCount + event.requestsCount),
        streaming.Seconds(windowSize), streaming.Seconds(windowSize)).cache()

    //Send statistics to redis
    eventsInfoStream
      .map(_._2.toString)
      .foreachRDD(rdd =>
        spark.sparkContext.toRedisSET(rdd, redisNameStats, redisStatsTimeToDrop))

    //Requests are checked and then sent bots ip to the ban list on 10 minutes
    eventsInfoStream
      .filter(data => data._2.requestsCount > requestsCount)
      .map(_._2.toString)
      .foreachRDD(rdd =>
        spark.sparkContext.toRedisSET(rdd, redisNameBots, redisTimeToDrop)
      )

    streamingContext.start()
    streamingContext.awaitTermination()
  }
}
