package com.github.sioncheng.push.storage

import java.net.InetAddress

import akka.actor.Actor
import com.github.sioncheng.push.conf.ElasticSearchServerConfig
import com.github.sioncheng.push.log.{DateUtil, LogUtil}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.transport.client.PreBuiltTransportClient
import spray.json.{JsBoolean, JsNumber, JsObject, JsString}

class ElasticClient(esServerConfig: ElasticSearchServerConfig) extends Actor {

  val logTitle = "ElasticSearchClient"

  var client: Option[TransportClient] = None

  override def preStart(): Unit = {
    super.preStart()
    initClient()
  }

  override def postStop(): Unit = {
    super.postStop()
    client.foreach(_.close())
  }

  override def receive: Receive = {
    case x: SaveFlyingNotification =>
      LogUtil.info(logTitle, s"save flying notification $x")
      client.foreach(saveFlyingNotification(x.notification, _))
    case x: SaveConfirmedNotification =>
      LogUtil.info(logTitle, s"save confirmed notification $x")
      client.foreach(saveConfirmedNotification(x.notification, _))
    case x =>
      LogUtil.warn(logTitle, s"received ? $x")
  }

  private def initClient(): Unit = {

    val settings = Settings.builder()
      .put("cluster.name", esServerConfig.clusterName)
      .put("client.transport.sniff", true)
      .build()

    val transportAddress =
      new InetSocketTransportAddress(InetAddress.getByName(esServerConfig.host), esServerConfig.port)

    client = Some(new PreBuiltTransportClient(settings)
      .addTransportAddress(transportAddress))

  }


  private def  saveFlyingNotification(notification: JsObject, client: TransportClient): Unit = {
    saveNotification(notification, client, "flying")
  }

  private def saveConfirmedNotification(notification: JsObject, client: TransportClient): Unit = {
    saveNotification(notification, client, "confirmed")
  }

  private def saveNotification(notification: JsObject, client: TransportClient, status: String): Unit = {
    val data = notification.fields.+("status"->JsString(status))
    println(data)
    val builder = XContentFactory.jsonBuilder().startObject();
    notification.fields.foreach(kv => {
      if (kv._2.isInstanceOf[JsString]) {
        builder.field(kv._1, kv._2.asInstanceOf[JsString].value)
      } else if (kv._2.isInstanceOf[JsNumber]) {
        builder.field(kv._1, kv._2.asInstanceOf[JsNumber].value.toLong)
      } else if (kv._2.isInstanceOf[JsBoolean]) {
        builder.field(kv._1, kv._2.asInstanceOf[JsBoolean].value)
      } else {
        builder.field(kv._1, kv._2.toString())
      }
    })
    builder.field("status", status)
      .field("@timestamp", DateUtil.getTimestampOfNow())
      .endObject()
    val indexResponse = client.prepareIndex("push-notification", "trace").setSource(builder).get()
    LogUtil.info(logTitle, s"save notification $indexResponse")
  }

}
