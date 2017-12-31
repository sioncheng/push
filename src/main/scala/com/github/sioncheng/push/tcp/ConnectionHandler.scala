package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp._
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.ConnectionStatus.ConnectionStatus
import com.github.sioncheng.push.tcp.Messages.{ClientLogon, ClientPeerClosed}
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.{JsObject, JsString}

object ConnectionStatus extends Enumeration {
  type ConnectionStatus = Value
  val Init = Value
  val Logon = Value
}

class ConnectionHandler(remoteAddress: InetSocketAddress, connection: ActorRef, clientManager: ActorRef) extends Actor {

  var status: ConnectionStatus = ConnectionStatus.Init
  val commandParser = new CommandParser
  var clientId: Option[String] = None

  val logTitle = "Connection Handler"

  def receive: Receive = {
    case Received(data) =>
      LogUtil.debug(logTitle, s"received data ${data.utf8String}")
      val command = commandParser.parseCommand(data)
      if (command.isEmpty) {
        LogUtil.warn(logTitle, "received uncompleted command data")
      } else {
        command.head.commands.foreach(processCommand _)
      }
    case Close =>
      LogUtil.warn(logTitle, s"close event $clientId $remoteAddress")
      close()
    case PeerClosed =>
      LogUtil.warn(logTitle, s"peer closed event $clientId $remoteAddress")
      clientManager ! ClientPeerClosed(clientId, remoteAddress)
      close()
    case ErrorClosed(cause) =>
      LogUtil.warn(logTitle, s"error closed $cause")
      close()
    case cmd: CommandObject =>
      val msg = Protocol.serializeCommand(cmd)
      LogUtil.debug(logTitle, s"send to client ${msg.utf8String}")
      connection ! Write(msg)
    case x =>
      LogUtil.warn(logTitle, s"what? $x")
  }

  def processCommand(command: Either[CommandObject, Exception]): Unit = {
    LogUtil.debug(logTitle, s"process command $command")
    try {
      if (status == ConnectionStatus.Init) {
        expectLogin(command)
      } else {
        expectOther(command)
      }
    } catch {
      case e : Exception =>
        LogUtil.error(logTitle, "process command err")
        unexpectedCommandException(e)
    }
  }

  def expectLogin(value: Either[Protocol.CommandObject, Exception]): Unit = {
    value match {
      case _ @ Left(cmd) =>
        //
        cmd.code match {
          case Protocol.LoginRequest =>
            val clientIdString = cmd.data.fields.get("clientId").head.asInstanceOf[JsString].value
            clientId = Some(clientIdString)
            status = ConnectionStatus.Logon

            self ! CommandObject(Protocol.LoginResponse, JsObject("clientId"->JsString(clientIdString)))

            clientManager ! ClientLogon(clientIdString, remoteAddress)
          case _ =>
            unexpectedCommandException(new Exception(cmd.toString))
        }
      case _ @ Right(ex) =>
        unexpectedCommandException(ex)
    }
  }

  def expectOther(value: Either[Protocol.CommandObject, Exception]): Unit = {
    value match {
      case Left(cmd) =>
        //
        clientManager ! cmd
      case Right(ex) =>
        unexpectedCommandException(ex)
    }
  }

  def unexpectedCommandException(ex: Exception): Unit = {
    connection ! Close
    clientManager ! ClientPeerClosed(clientId, remoteAddress)
    context stop self
  }

  private def close(): Unit = {
    clientManager ! ClientPeerClosed(clientId, remoteAddress)
    connection ! Close
    context stop self
  }
}
