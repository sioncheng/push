package com.github.sioncheng.push.tcp

import akka.actor.{Actor, ActorRef}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages._

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
  }

  def clientLogon(clientId: String, connectionHandler: ActorRef): Unit = {
    clients.get(clientId).foreach(x => x ! ShutdownClient(clientId))
    clients.put(clientId, sender())
  }

  def queryClient(clientId: String, ask: ActorRef): Unit = {
    if (clients.contains(clientId)) {
      ask ! ClientHandlerInfo(clientId, Some(self))
    } else {
      ask ! ClientHandlerInfo(clientId, None)
    }
  }

}
