package com.github.sioncheng.push.tcp

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import com.github.sioncheng.push.log.{DateUtil, LogUtil}
import com.github.sioncheng.push.storage.{QueryNotificationsResult, QueryOfflineNotifications}
import com.github.sioncheng.push.tcp.Messages._
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.{JsObject, JsString}

import scala.collection.mutable

class ClientManager(notificationManager: ActorRef) extends Actor {

  val clients = new mutable.HashMap[String, ActorRef]()

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
    case QueryNotificationsResult(clientId, notifications) =>
      LogUtil.debug(logTitle, s"${notifications.length} offline notifications for $clientId")
      notifications.foreach(resendOfflineNotification _)
  }

  def clientLogon(clientId: String, connectionHandler: ActorRef): Unit = {
    clients.get(clientId).foreach(x => x ! ShutdownClient(clientId))
    clients.put(clientId, connectionHandler)

    notificationManager ! QueryOfflineNotifications(clientId)
  }

  def queryClient(clientId: String, ask: ActorRef): Unit = {
    if (clients.contains(clientId)) {
      ask ! ClientHandlerInfo(clientId, Some(self))
    } else {
      ask ! ClientHandlerInfo(clientId, None)
    }
  }


  private def processCommand(c: CommandObject, sender: ActorRef): Unit = {
    c.code match {
      case Protocol.SendNotification =>
        val clientId = c.data.fields.get("clientId").get.asInstanceOf[JsString].value
        val clientHandler = clients.get(clientId)
        val notification = markNotification(c.data)
        clientHandler.isEmpty match {
          case true =>
            sender ! SendNotificationAccept(false, notification)
            notificationManager ! OfflineNotification(notification)
          case false =>
            sender ! SendNotificationAccept(true, notification)
            clientHandler.get ! CommandObject(c.code, notification)
            notificationManager ! FlyingNotification(notification)
        }
      case Protocol.ReceivedNotification =>
        notificationManager ! ConfirmedNotification(c.data)
    }
  }

  private def markNotification(notification: JsObject): JsObject = {
    import spray.json._
    import DefaultJsonProtocol._
    val uuid = UUID.randomUUID().toString.replace("-","")
    val timestamp = "timestamp" -> JsNumber(DateUtil.getTimestampOfNow())
    val messageId = "messageId" -> JsString(uuid)
    notification.fields.+(timestamp).+(messageId).toJson.asJsObject
  }

  private def resendOfflineNotification(c: JsObject): Unit = {
    val clientId = c.fields.get("clientId").get.asInstanceOf[JsString].value
    clients.get(clientId).foreach(x => x ! CommandObject(Protocol.SendNotification, c))
  }
}
