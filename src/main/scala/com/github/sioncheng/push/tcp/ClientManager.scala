package com.github.sioncheng.push.tcp

import akka.actor.Actor
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages.NewConnection

class ClientManager extends Actor {

  override def receive: Receive = {
    case NewConnection(remote, local) =>
      LogUtil.debug(s"new conn $remote $local")
  }
}
