package com.github.sioncheng.push.client

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Tcp}
import akka.io.Tcp._
import akka.util.ByteString
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.{CommandParser, Protocol}
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.{JsObject, JsString}

class NotificationClient(serverHost: String, serverPort: Int, clientId: String) extends Actor {

  import context.system

  IO(Tcp) ! Connect(new InetSocketAddress(serverHost, serverPort))

  var connection: ActorRef = null

  val commandParser = new CommandParser

  val logTitle = "Notification Client"

  override def receive: Receive = {
    case Connected(remoteAddress, localAddress) =>
      LogUtil.debug(logTitle, s"connect to $remoteAddress at $localAddress")
      connection = sender()
      connection ! Register(self)
      login()
    case Received(data) =>
      LogUtil.debug(logTitle, s"received ${data.utf8String}")
      processReceivedData(data)
    case PeerClosed =>
      LogUtil.warn(logTitle, "server side closed")
      context stop self
  }

  def login(): Unit = {
    val command = CommandObject(Protocol.LoginRequest, JsObject("clientId"-> JsString(clientId)))
    val commandData = Protocol.serializeCommand(command)
    connection ! Write(commandData)
  }

  def processReceivedData(data: ByteString): Unit = {
    val parseResult = commandParser.parseCommand(data)
    parseResult.foreach( x => {
      x.commands.foreach(processCommand _)
    })
  }

  def processCommand(command: Either[CommandObject, Exception]): Unit = {
    command.isLeft match {
      case true =>
        val co = command.left.get
        co.code match {
          case Protocol.LoginResponse =>
            processLoginResponse(co)
          case Protocol.SendNotification =>
            processNotification(co)
          case _ =>
            LogUtil.warn(logTitle, s"what ? ${co}")
        }
      case false =>
        LogUtil.error(logTitle, s"${command.right.get.toString}")
        connection ! Close
        context stop self
    }
  }

  def processLoginResponse(commandObject: Protocol.CommandObject): Unit = {
    LogUtil.debug(logTitle, s"logon ${commandObject}")
  }

  def processNotification(commandObject: Protocol.CommandObject): Unit = {
    LogUtil.debug(logTitle, s"got notification $commandObject")
    val confirm = CommandObject(Protocol.ReceivedNotification, commandObject.data)
    val confirmData = Protocol.serializeCommand(confirm)
    connection ! Write(confirmData)
  }
}
