package com.github.sioncheng.push.tcp

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.util.ByteString
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json.JsObject


object Messages {

  case class ServerStatus(boundTo: String)

  case class ServerStatusQuery()
  case class ServerStatusRes(status: ServerStatus)
  case class NewConnection(remoteAddress: InetSocketAddress, localAddress: InetSocketAddress)
  case class ClientPeerClosed(clientId: Option[String], remoteAddress: InetSocketAddress)
  case class ClientLogon(clientId: String, remoteAddress: InetSocketAddress)
  case class ShutdownClient(clientId: String)
  case class QueryClient(clientId: String)
  case class ClientHandlerInfo(clientId: String, clientHandler: Option[ActorRef])
  case class SendNotificationAccept(accepted: Boolean, c: CommandObject)
}


object Protocol {
  val LoginRequest = 1
  val LoginResponse = 2
  val SendNotification = 3
  val ReceivedNotification = 4
  val HeartBeatPing = 5
  val heartBeatPong = 6

  case class CommandObject(code: Int, data:JsObject)


  import spray.json._
  object CommandProtocol extends DefaultJsonProtocol {
    implicit object CommandProtocolFormat extends RootJsonFormat[CommandObject] {
      def write(c: CommandObject): JsObject = {
        JsObject("code" -> JsNumber(c.code), "data" -> c.data)
      }

      def read(value: JsValue): CommandObject = {
        val jObj = value.asJsObject()
        CommandObject(jObj.fields.get("code").head.asInstanceOf[JsNumber].value.toInt,
          jObj.fields.get("data").head.asJsObject)
      }
    }
  }

  def serializeCommand(c: CommandObject): ByteString = {
    import CommandProtocol._
    val data = ByteString.fromString(c.toJson.toString())
    val head = ByteString.fromString("%4d".format(data.length))
    head.++(data)
  }
}