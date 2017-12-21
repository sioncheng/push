package com.github.sioncheng.push.tcp

object Messages {

  case class ServerStatus(boundTo: String)

  case class ServerStatusQuery()
  case class ServerStatusRes(status: ServerStatus)
}
