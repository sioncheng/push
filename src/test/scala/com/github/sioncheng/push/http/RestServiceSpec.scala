package com.github.sioncheng.push.http

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.tcp.Messages.SendNotificationAccept
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.{JsObject, JsString}

import scala.concurrent.Future


class MockClientManager extends Actor {


  override def receive: Receive = {
    case x : CommandObject =>
      println(x)
      sender() ! SendNotificationAccept(true, x)
  }
}

class RestServiceSpec() extends TestKit(ActorSystem("RestServiceSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "rest service " must {
    "return hello for get /" in {

      val clientManager = system.actorOf(Props[MockClientManager])

      val restService = system.actorOf(Props(classOf[RestService], "localhost", 8080, clientManager))

      Thread.sleep(1000) // should wait 1 second for bind complete

      implicit val materializer = ActorMaterializer()
      // needed for the future flatMap/onComplete in the end
      implicit val executionContext = system.dispatcher

      val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "http://localhost:8080"))
      responseFuture.onComplete(f => {
        f.isSuccess should be (true)
        println(f.get)
      })


      val jsonRequestData =  JsObject("clientId" -> JsString("321234567890")
        ,"title" -> JsString("title")
        ,"body" -> JsString("notification body")).toString()

      val headers = scala.collection.immutable.Seq(
        RawHeader("accept", "application/json")
      )
      val sendNotificationResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(HttpMethods.POST,
        "http://localhost:8080/send-notification",
        headers,
        HttpEntity(ContentTypes.`application/json`, jsonRequestData)))

        sendNotificationResponse.onComplete( f => {
        println(f.get)
        f.isSuccess should be(true)
      })

      Thread.sleep(1000)
    }
  }
}
