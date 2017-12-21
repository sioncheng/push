package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.Actor
import akka.io.Tcp.{Bind, Bound}
import akka.io.{IO, Tcp}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages.{ServerStatus, ServerStatusQuery, ServerStatusRes}

class ServerActor(host: String, port: Int) extends Actor {

  implicit val system =  context.system
  implicit val ec = system.dispatcher

  var boundTo: String = null

  override def preStart(): Unit = {
    val address = new InetSocketAddress(host, port);
    IO(Tcp) ! Bind(self, address)
  }

  override def postRestart(reason: Throwable): Unit = {
    context stop self
  }

  override def receive: Receive = {
    case Bound(localAddress) =>
      LogUtil.debug(s"server is bound to ${localAddress}")
      boundTo = localAddress.toString
    case ServerStatusQuery =>
      LogUtil.debug("ask server status")
      sender() ! ServerStatusRes(ServerStatus(boundTo))

  }
}
