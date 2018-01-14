name := "push"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.apache.hadoop" % "hadoop-mapred" % "0.22.0",
  "org.apache.hadoop" % "hadoop-common" % "2.6.0",
  "org.apache.hbase" % "hbase-common" % "1.3.1",
  "org.apache.hbase" % "hbase-client" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.20",
  "com.typesafe.akka" %% "akka-stream" % "2.4.20",
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  "org.elasticsearch" % "elasticsearch" % "5.6.5",
  "org.elasticsearch.client" % "transport" % "5.6.5",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test,
  "io.spray" %%  "spray-json" % "1.3.3",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.20" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.20" % Test
)