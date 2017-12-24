package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.{Close, PeerClosed, Received}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.ConnectionStatus.ConnectionStatus
import com.github.sioncheng.push.tcp.Messages.{ClientLogon, ConnectionClosed}
import com.github.sioncheng.push.tcp.Protocol.Command
import spray.json.JsString

object ConnectionStatus extends Enumeration {
  type ConnectionStatus = Value
  val Init = Value
  val Logon = Value
}

class ConnectionHandler(remoteAddress: InetSocketAddress, connection: ActorRef, clientManager: ActorRef) extends Actor {

  var status: ConnectionStatus = ConnectionStatus.Init
  val commandParser = new CommandParser
  var clientId: Option[String] = None

  def receive: Receive = {
    case Received(data) =>
      LogUtil.debug(s"received data ${data.utf8String}")
      val command = commandParser.parseCommand(data)
      if (command.isEmpty) {
        LogUtil.warn("received uncompleted command data")
      } else {
        command.head.foreach(processCommand _)
      }
    case PeerClosed =>
      clientManager ! ConnectionClosed(clientId, remoteAddress)
      context stop self
    case msg: String =>
      println(msg)
  }

  def processCommand(command: Either[Command, Exception]): Unit = {
    println(s"process command $command")
    try {
      if (status == ConnectionStatus.Init) {
        expectLogin(command)
      } else {
        expectOther(command)
      }
    } catch {
      case e : Exception =>
        LogUtil.error("process command err")
        unexpectedCommandException(e)
    }
  }

  def expectLogin(value: Either[Protocol.Command, Exception]): Unit = {
    value match {
      case _ @ Left(cmd) =>
        //
        cmd.code match {
          case Protocol.LoginRequest =>
            val clientIdString = cmd.data.fields.get("clientId").head.asInstanceOf[JsString].value
            clientId = Some(clientIdString)
            status = ConnectionStatus.Logon
            clientManager ! ClientLogon(clientIdString, remoteAddress)
          case _ =>
            unexpectedCommandException(new Exception(cmd.toString))
        }
      case _ @ Right(ex) =>
        unexpectedCommandException(ex)
    }
  }

  def expectOther(value: Either[Protocol.Command, Exception]): Unit = {

  }

  def unexpectedCommandException(ex: Exception): Unit = {
    connection ! Close
    clientManager ! ConnectionClosed(clientId, remoteAddress)
    context stop self
    ex.printStackTrace()
  }
}
