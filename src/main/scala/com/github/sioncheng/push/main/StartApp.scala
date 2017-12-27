package com.github.sioncheng.push.main

import akka.actor.{ActorSystem, Props}
import com.github.sioncheng.push.http.RestService


object StartApp extends App {

  println("push start")

  val system = ActorSystem("push-server")

  val restService = system.actorOf(Props(classOf[RestService], "localhost", 8080, null))

  scala.io.StdIn.readLine()

}