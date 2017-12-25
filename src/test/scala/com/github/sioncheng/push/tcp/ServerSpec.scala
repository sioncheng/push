package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.tcp.Messages._
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.{JsObject, JsString}


class TestClient(clientListener: ActorRef) extends Actor {

  import context.system

  IO(Tcp) ! Connect(new InetSocketAddress("localhost", 8080))

  var connection: ActorRef = null

  val commandParser = new CommandParser

  override def receive: Receive = {
    case _ @ Connected(remote, local) =>
      println(remote, local)
      connection = sender()
      connection ! Register(self)

    case command: CommandObject =>
      connection ! Write(Protocol.serializeCommand(command))

    case Received(data) =>
      println(s"test client received $data")
      val commands = commandParser.parseCommand(data)
      clientListener ! commands
  }
}

class ClientManagerTester(specActor: ActorRef) extends Actor {

  import context.system

  val clientManager = system.actorOf(Props[ClientManager])

  override def receive: Receive = {
    case ch : ClientHandlerInfo =>
      println(s"got client handler info $ch in client manager tester")
      specActor ! ch
    case x : Any =>
      println(s"forward $x from client manager tester")
      clientManager forward(x)
  }
}

class ServerSpec() extends TestKit(ActorSystem("ServerActorSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll{


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Server Actor" must {
    "bind to local address then accept new conn and process logon" in {
      val clientManagerTester = system.actorOf(Props(classOf[ClientManager]))
      val props = Props(classOf[Server], "0.0.0.0", 8080, clientManagerTester);
      val server = system.actorOf(props)
      Thread.sleep(100)
      server ! ServerStatusQuery
      expectMsg(ServerStatusRes(ServerStatus("/0:0:0:0:0:0:0:0:8080")))

      val testClient = system.actorOf(Props(classOf[TestClient],self))

      Thread.sleep(100)
      //expectMsgAnyClassOf(classOf[Connected])
      /*
      expectMsgPF() {
        case _ @ NewConnection(remote,local) if local.toString().endsWith("127.0.0.1:8080") =>
          println(remote, local)
          true
      }*/

      val loginCommand = CommandObject(Protocol.LoginRequest, JsObject("clientId"->JsString("321234567890")))
      testClient ! loginCommand

      Thread.sleep(100)

      expectMsgPF() {
        case parseResult : Option[ParseResult]
          if parseResult.get.commands.length == 1
            && parseResult.get.commands.head.isLeft
            && parseResult.get.commands.head.left.get.code == Protocol.LoginResponse =>
          val command = parseResult.get.commands.head.left.get
          println(command.code, command.data.toString())

      }


      Thread.sleep(100)

      clientManagerTester ! QueryClient("321234567890")

      Thread.sleep(100)

      expectMsgPF() {
        case ch @ ClientHandlerInfo(clientId, clientHandler) if "321234567890".equals(clientId) && clientHandler.nonEmpty =>
          println(ch, clientHandler.get)
      }
    }
  }
}
