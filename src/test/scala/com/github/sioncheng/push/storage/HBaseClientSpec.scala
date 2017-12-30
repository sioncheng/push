package com.github.sioncheng.push.storage

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.conf.HBaseStorageConfig
import com.github.sioncheng.push.log.DateUtil
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.{JsObject, JsString}

import scala.concurrent.duration._

class HBaseClientSpec extends TestKit(ActorSystem("HBaseClientSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val hBaseClientConfig = HBaseStorageConfig( "172.16.25.129", 2181)
  val hbaseClient = system.actorOf(Props(classOf[HBaseClient], hBaseClientConfig))

  "hbase client " must {
    "be able to save confirmed notification" in {
      val notification = JsObject("clientId" -> JsString("321234567890")
        , "title" -> JsString("title")
        , "body" -> JsString("body"))

      hbaseClient ! SaveConfirmedNotification(notification)
    }

    "be able to query saved notification" in {
      val now = DateUtil.getTimestampOfNow()
      val queryConfirmedNotifications = QueryConfirmedNotifications("321234567890", now - 60000, now + 60000)

      hbaseClient ! queryConfirmedNotifications

      implicit val timeout = 2 second

      expectMsgPF() {
        case QueryNotificationsResult(clientId, notifications) if ("321234567890".equals(clientId)) =>
          println(clientId, notifications)
      }
    }

    "be able to save offline notification and query it" in {
      val notification = JsObject("clientId" -> JsString("321234567890")
        , "title" -> JsString("title")
        , "body" -> JsString("body"))

      hbaseClient ! SaveOfflineNotification(notification)

      hbaseClient ! QueryOfflineNotifications("321234567890")

      implicit val timeout = 2 second

      expectMsgPF() {
        case QueryNotificationsResult(clientId, notifications) if ("321234567890".equals(clientId)) =>
          println(clientId, notifications)
      }
    }
  }
}
