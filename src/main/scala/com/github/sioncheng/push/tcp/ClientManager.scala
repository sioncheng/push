package com.github.sioncheng.push.tcp

import akka.actor.{Actor, ActorRef, Props}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.storage.{ConfirmedNotification, FlyingNotification, NotificationManager, OfflineNotification}
import com.github.sioncheng.push.tcp.Messages._
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.{JsObject, JsString}

import scala.collection.mutable

class ClientManager extends Actor {

  val clients = new mutable.HashMap[String, ActorRef]()

  val notificationManager = context.system.actorOf(Props(classOf[NotificationManager]))

  val logTitle = "Client Manager"

  override def receive: Receive = {
    case QueryClient(clientId) =>
      LogUtil.debug(logTitle, s"query client ${clientId}")
      queryClient(clientId, sender())
    case NewConnection(remote, local) =>
      LogUtil.debug(logTitle, s"new conn $remote $local")
    case ClientLogon(clientId, remoteAddress) =>
      LogUtil.debug(logTitle, s"client login $clientId $remoteAddress")
      clientLogon(clientId, sender())
    case c: CommandObject =>
      LogUtil.debug(logTitle, s"received command ${c}")
      processCommand(c, sender())
    case ClientPeerClosed(clientId, remoteAddress) =>
      LogUtil.debug(logTitle, s"client peer closed ${clientId} $remoteAddress")
      clientId.foreach(x => {clients.remove(x)})
  }

  def clientLogon(clientId: String, connectionHandler: ActorRef): Unit = {
    clients.get(clientId).foreach(x => x ! ShutdownClient(clientId))
    clients.put(clientId, connectionHandler)
    connectionHandler ! CommandObject(Protocol.LoginResponse, JsObject("clientId"->JsString(clientId)))
  }

  def queryClient(clientId: String, ask: ActorRef): Unit = {
    if (clients.contains(clientId)) {
      ask ! ClientHandlerInfo(clientId, Some(self))
    } else {
      ask ! ClientHandlerInfo(clientId, None)
    }
  }


  def processCommand(c: CommandObject, sender: ActorRef): Unit = {
    c.code match {
      case Protocol.SendNotification =>
        val clientId = c.data.fields.get("clientId").get.asInstanceOf[JsString].value
        val clientHandler = clients.get(clientId)
        clientHandler.isEmpty match {
          case true =>
            sender ! SendNotificationAccept(false, c)
            notificationManager ! OfflineNotification(c.data)
          case false =>
            sender ! SendNotificationAccept(true, c)
            clientHandler.get ! c
            notificationManager ! FlyingNotification(c.data)
        }
      case Protocol.ReceivedNotification =>
        notificationManager ! ConfirmedNotification(c.data)
    }
  }

}
