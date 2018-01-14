package com.github.sioncheng.push.storage

import java.net.InetAddress

import akka.actor.Actor
import com.github.sioncheng.push.conf.ElasticSearchServerConfig
import com.github.sioncheng.push.log.LogUtil
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import spray.json.JsObject

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
      saveFlyingNotification(x.notification)
    case x: SaveConfirmedNotification =>
      LogUtil.info(logTitle, s"save confirmed notification $x")
      saveConfirmed(x.notification)
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


  private def  saveFlyingNotification(notification: JsObject): Unit = {

  }

  private def saveConfirmed(notification: JsObject): Unit = {

  }
}
