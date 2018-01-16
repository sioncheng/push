package com.github.sioncheng.push.storage

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.sioncheng.push.conf.ElasticSearchServerConfig
import com.github.sioncheng.push.log.DateUtil
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import spray.json.{JsObject, JsString}

class ElasticClientSpec extends TestKit(ActorSystem("ElasticClientSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val conf = ElasticSearchServerConfig("172.16.25.130", 9300, "my-application")
  val props = Props(classOf[ElasticClient], conf)
  val client = system.actorOf(props)

  "A elastic client" must {
    "should be able to save flying notification" in {

      client ! SaveFlyingNotification(createNotification())

      Thread.sleep(2000)
    }
  }

  private def createNotification(): JsObject = JsObject("messageId"->JsString(s"1234567890-${DateUtil.getTimestampOfNow()}")
    ,"clientId"->JsString(s"cid-1234567890-${DateUtil.getTimestampOfNow()}")
    ,"title"->JsString("notification title")
    ,"body"->JsString(s"notification body ${DateUtil.getTimestampOfNow()}"))

}
