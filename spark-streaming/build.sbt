name := "spark"

version := "0.1"

scalaVersion := "2.12.7"

resolvers += "Bintray Maven Repository" at "https://dl.bintray.com/spark-packages/maven"
resolvers += "Bintray Maven Repository" at "https://mvnrepository.com/artifact/com.redislabs/spark-redis"

  libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "2.4.7",
    "org.apache.spark" %% "spark-sql" % "2.4.7",
    "org.apache.spark" %% "spark-streaming" % "2.4.7",
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % "2.4.7",
    "com.datastax.spark" %% "spark-cassandra-connector" % "2.5.1",
    "net.liftweb" %% "lift-json" % "3.4.2",
    "net.debasishg" %% "redisclient" % "3.30",
    "com.typesafe" % "config" % "1.2.1",
    "com.redislabs" % "spark-redis_2.12" % "2.4.2",
    "org.apache.spark" % "spark-sql-kafka-0-10_2.12" % "2.4.7",
    "org.scalactic" %% "scalactic" % "3.2.2",
    "com.novocode" % "junit-interface" % "0.11" % Test
)

testOptions in Test += Tests.Argument(TestFrameworks.JUnit, "-a", "-v", "-s")