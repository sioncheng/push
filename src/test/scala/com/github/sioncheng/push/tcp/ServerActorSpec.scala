package com.github.sioncheng.push.tcp

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.tcp.Messages.{ServerStatus, ServerStatusQuery, ServerStatusRes}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec, WordSpecLike}

class ServerActorSpec() extends TestKit(ActorSystem("ServerActorSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll{


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Server Actor" must {
    "bind to local address" in {
      val props = Props(classOf[ServerActor], "0.0.0.0", 8080);
      val server = system.actorOf(props)
      Thread.sleep(1000)
      server ! ServerStatusQuery
      expectMsg(ServerStatusRes(ServerStatus("/0:0:0:0:0:0:0:0:8080")))
    }
  }
}
