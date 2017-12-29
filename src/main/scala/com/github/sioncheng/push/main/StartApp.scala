package com.github.sioncheng.push.main

import akka.actor.{ActorSystem, Props}
import com.github.sioncheng.push.http.RestService
import com.github.sioncheng.push.tcp.{ClientManager, NotificationServer}


object StartApp extends App {

  println("push start")

  val system = ActorSystem("push-server")

  val clientManager = system.actorOf(Props(classOf[ClientManager]))

  val restService = system.actorOf(Props(classOf[RestService], "0.0.0.0", 8080, clientManager))

  val server = system.actorOf(Props(classOf[NotificationServer], "0.0.0.0", 8181, clientManager))

  scala.io.StdIn.readLine()

  system.terminate()
}