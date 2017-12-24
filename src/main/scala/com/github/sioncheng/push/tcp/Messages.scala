package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import akka.util.ByteString
import spray.json.JsObject


object Messages {

  case class ServerStatus(boundTo: String)

  case class ServerStatusQuery()
  case class ServerStatusRes(status: ServerStatus)
  case class NewConnection(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress)
  case class ConnectionClosed(clientId: Option[String], remoteAddress: InetSocketAddress)
  case class ClientLogon(clientId: String, remoteAddress: InetSocketAddress)
}


object Protocol {
  val LoginRequest = 1
  val LoginResponse = 2

  case class Command(code: Int, data:JsObject)


  import spray.json._
  object CommandProtocol extends DefaultJsonProtocol {
    implicit object CommandProtocolFormat extends RootJsonFormat[Command] {
      def write(c: Command): JsObject = {
        JsObject("code" -> JsNumber(c.code), "data" -> c.data)
      }

      def read(value: JsValue): Command = {
        val jObj = value.asJsObject()
        Command(jObj.fields.get("code").head.asInstanceOf[JsNumber].value.toInt,
          jObj.fields.get("data").head.asJsObject)
      }
    }
  }

  def serializeCommand(c: Command): ByteString = {
    import CommandProtocol._
    val data = ByteString.fromString(c.toJson.toString())
    val head = ByteString.fromString("%4d".format(data.length))
    head.++(data)
  }
}