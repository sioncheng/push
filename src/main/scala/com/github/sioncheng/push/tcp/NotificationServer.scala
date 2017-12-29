package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages.{NewConnection, ServerStatus, ServerStatusQuery, ServerStatusRes}

class NotificationServer(host: String, port: Int, clientManager: ActorRef) extends Actor {

  implicit val system =  context.system
  implicit val ec = system.dispatcher

  var boundTo: String = null

  var connections = Map.empty[String, ActorRef]

  val logTitle = "Notification Server"

  override def preStart(): Unit = {
    val address = new InetSocketAddress(host, port);
    IO(Tcp) ! Bind(self, address)
  }

  override def postRestart(reason: Throwable): Unit = {
    context stop self
  }

  override def receive: Receive = {
    case Bound(localAddress) =>
      LogUtil.debug(logTitle, s"server is bound to ${localAddress}")
      boundTo = localAddress.toString
    case CommandFailed(_: Bind) =>
      LogUtil.error(logTitle, "bind failed")
      context stop self
    case Connected(remote, local) =>
      LogUtil.debug(logTitle, s"new conn ${local} ${remote}")
      val client = sender()
      val props = Props.create(classOf[ConnectionHandler], remote, client, clientManager);
      val handler = system.actorOf(props)
      client ! Register(handler)
      clientManager ! NewConnection(remote, local)
    case ServerStatusQuery =>
      LogUtil.debug(logTitle, "ask server status")
      sender() ! ServerStatusRes(ServerStatus(boundTo))
  }
}