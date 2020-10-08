import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.SparkSession

object ToolsConfig {


  val kafkaHost = "localhost:9092"
  val kafkaTopic = "bots"
  val batchInterval = 10

  val redisHost = "localhost"
  val redisPort = 6379
  val redisTimeToDrop = 600 //sec
  val redisStatsTimeToDrop = 1200 //sec
  val redisNameBots = "dsBots"
  val redisNameStats = "dsStats"

  val clickToTransitions = 2L
  val requestsCount = 20L
  val windowSize = 10 //sec
  val watermark = 10 //sec

  val spark = SparkSession.builder()
    .appName("spark-streaming")
    .master("local[*]")
    .config("spark.driver.memory", "2g")
    .config("redis.host", redisHost)
    .config("redis.port", redisPort)
    .config("spark.cassandra.connection.host", redisHost)
    .getOrCreate()

  val kafkaProps: Map[String, Object] = Map[String, Object](
    "bootstrap.servers" -> kafkaHost,
    "key.deserializer" -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[StringDeserializer],
    "group.id" -> "bots-group",
    "auto.offset.reset" -> "latest"
  )


}