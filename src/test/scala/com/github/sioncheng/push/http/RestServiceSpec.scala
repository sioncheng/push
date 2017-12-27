package com.github.sioncheng.push.http

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.tcp.Messages.{QueryClient, SendNotificationAccept}
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.util.{Failure, Success}


class MockClientManager extends Actor {


  override def receive: Receive = {
    case x : CommandObject =>
      print(x)
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

      val formData = FormData(Map(
        "Keywords" -> "query", "Count" -> "25"
      ))
      val headers = scala.collection.immutable.Seq(
        RawHeader("accept", "application/json"),
        RawHeader("content-type", "application/json")
      )
      val sendNotificationResponse: Future[HttpResponse] = Http().singleRequest(HttpRequest(HttpMethods.POST,
        "http://localhost:8080/send-notification",
        headers,
        formData.toEntity))

      sendNotificationResponse.onComplete( f => {
        print(f.get)
        f.isSuccess should be(true)
      })

      Thread.sleep(1000)
    }
  }
}
