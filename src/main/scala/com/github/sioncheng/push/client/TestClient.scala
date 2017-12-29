package com.github.sioncheng.push.client

import akka.actor.{ActorSystem, Props}


object TestClient extends App {

  println("test client")

  println("enter server host")
  val host = scala.io.StdIn.readLine()
  println("enter server port")
  val port = scala.io.StdIn.readLine()

  val system = ActorSystem("TestClient")
  val client = system.actorOf(Props(classOf[NotificationClient], host, Integer.parseInt(port), "321234567890"))


  println("...")
  scala.io.StdIn.readLine()

  system.terminate()

}
