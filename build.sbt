name := "push"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.apache.hbase" % "hbase-client" % "1.3.1",
  "com.typesafe.akka" %% "akka-actor" % "2.4.20",
  "com.typesafe.akka" %% "akka-stream" % "2.4.20",
  "com.typesafe.akka" %% "akka-http" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.11",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.11" % Test,
  "io.spray" %%  "spray-json" % "1.3.3",
  "org.scalatest" %% "scalatest" % "3.0.4" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.20" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.20" % Test
)