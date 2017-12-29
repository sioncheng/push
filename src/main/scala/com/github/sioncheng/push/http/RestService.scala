package com.github.sioncheng.push.http

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.{ByteString, Timeout}
import com.github.sioncheng.push.log.LogUtil
import com.github.sioncheng.push.tcp.Messages.SendNotificationAccept
import com.github.sioncheng.push.tcp.Protocol
import com.github.sioncheng.push.tcp.Protocol.CommandObject

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class RestService(host: String, port: Int, clientManager: ActorRef) extends Actor {

  implicit val system =  context.system
  implicit val ec = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val logTitle = "Rest Service"

  val handler: (HttpRequest => Future[HttpResponse] ) = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), _, _, _) =>
      Future(HttpResponse(entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        "<html><body>Hello world!</body></html>")))

    case HttpRequest(HttpMethods.POST, Uri.Path("/send-notification"), _, entity, _) =>
      processSendNotification(entity)

    case r: HttpRequest =>
      r.discardEntityBytes() // important to drain incoming HTTP Entity stream
      Future(HttpResponse(404, entity = "Unknown resource!"))
  }

  val bindFuture = Http().bindAndHandleAsync(handler, host, port)


  bindFuture.onComplete {
    case Success(v) =>
      LogUtil.debug(logTitle, s"rest service bind success $v")
    case Failure(f) =>
      LogUtil.error(logTitle, s"rest service bind failure $f")
  }

  override def receive: Receive = {
    case x =>
      println(x)
  }

  override def postStop(): Unit = {
    bindFuture.flatMap(x => x.unbind())
  }

  def processSendNotification(entity: HttpEntity): Future[HttpResponse] = {
    entity.dataBytes.runWith(Sink.fold(ByteString.empty)(_ ++ _)).map(_.utf8String).flatMap(jsonData => {
      import spray.json._
      val jsonObj = jsonData.parseJson.asJsObject
      val command = CommandObject(Protocol.SendNotification, jsonObj)

      implicit val timeout = Timeout(2 second)
      ask(clientManager, command).map( x => {
        val accepted = x.asInstanceOf[SendNotificationAccept]
        accepted.accepted match {
          case true =>
            HttpResponse(200, entity = "done!")
          case false =>
            HttpResponse(404, entity = "Unknown resource!")
        }
      })
    })


  }
}
