package com.github.sioncheng.push.tcp

import akka.actor.{Actor, ActorRef}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages._
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.{JsObject, JsString}

import scala.collection.mutable

class ClientManager extends Actor {

  val clients = new mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {
    case QueryClient(clientId) =>
      LogUtil.debug(s"query client ${clientId}")
      queryClient(clientId, sender())
    case NewConnection(remote, local) =>
      LogUtil.debug(s"new conn $remote $local")
    case ClientLogon(clientId, remoteAddress) =>
      LogUtil.debug(s"client login $clientId $remoteAddress")
      clientLogon(clientId, sender())
    case c: CommandObject =>
      LogUtil.debug(s"received command ${c}")
      processCommand(c, sender())
    case ClientPeerClosed(clientId, remoteAddress) =>
      LogUtil.debug(s"client peer closed ${clientId} $remoteAddress")
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
          case false =>
            sender ! SendNotificationAccept(true, c)
            sendNotification(c, clientHandler.get)
        }
    }
  }

  def sendNotification(commandObject: Protocol.CommandObject, clientHandler: ActorRef): Unit = {
    clientHandler ! commandObject
  }

}
