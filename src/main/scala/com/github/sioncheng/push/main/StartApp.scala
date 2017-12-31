package com.github.sioncheng.push.main

import akka.actor.{ActorSystem, Props}
import com.github.sioncheng.push.conf.HBaseStorageConfig
import com.github.sioncheng.push.http.RestService
import com.github.sioncheng.push.storage.HBaseClient
import com.github.sioncheng.push.tcp.{ClientManager, NotificationManager, NotificationServer}


object StartApp extends App {

  println("push start")

  val system = ActorSystem("push-server")


  val hBaseConfiguration = HBaseStorageConfig("172.16.25.129", 2181)
  val hbaseClient = system.actorOf(Props(classOf[HBaseClient], hBaseConfiguration))
  val notificationManager = system.actorOf(Props(classOf[NotificationManager], hbaseClient))

  val clientManager = system.actorOf(Props(classOf[ClientManager], notificationManager))

  val restService = system.actorOf(Props(classOf[RestService], "0.0.0.0", 8080, clientManager, hbaseClient))

  val server = system.actorOf(Props(classOf[NotificationServer], "0.0.0.0", 8181, clientManager))

  scala.io.StdIn.readLine()

  system.terminate()
}