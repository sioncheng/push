package com.github.sioncheng.push.storage

import akka.actor.Actor
import com.github.sioncheng.push.conf.HBaseStorageConfig
import com.github.sioncheng.push.log.LogUtil
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import spray.json._

import scala.collection.mutable

case class SaveConfirmedNotification(notification:JsObject)
case class QueryConfirmedNotifications(clientId: String, beginTimestamp: Long, endTimestamp: Long)
case class SaveOfflineNotification(notification: JsObject)
case class QueryOfflineNotifications(clientId: String)
case class SaveFlyingNotification(notification: JsObject)
case class QueryFlyingNotification(beforeTimestamp: Long)
case class QueryNotificationsResult(clientId: String, notifications: Seq[JsObject])
case class CheckAndSaveUnconfirmedNotification(notifications: Seq[JsObject])

class HBaseClient(conf: HBaseStorageConfig) extends Actor {

  val logTitle = "HBase Client"

  val (connection
  , confirmNotificationTable
  , offlineNotificationTable
  , flyingNotificationTable) = init()

  override def receive: Receive = {
    case SaveConfirmedNotification(notification) =>
      LogUtil.debug(logTitle, s"save confirmed notification ${notification.toString()}")
      saveConfirmedNotification(notification)
    case QueryConfirmedNotifications(clientId, beginTimestamp, endTimestamp) =>
      LogUtil.debug(logTitle, s"query confirmed notification $clientId, $beginTimestamp, $endTimestamp")
      queryConfirmedNotification(clientId, beginTimestamp, endTimestamp)
    case SaveOfflineNotification(notification) =>
      LogUtil.debug(logTitle, s"save offline notification ${notification.toString()}")
      saveOfflineNotification(notification)
    case QueryOfflineNotifications(clientId) =>
      LogUtil.debug(logTitle, s"query offline notification ${clientId}")
      queryOfflineNotification(clientId)
    case SaveFlyingNotification(notification) =>
      LogUtil.debug(logTitle, s"save flying notification ${notification.toString()}")
      saveFlyingNotification(notification)
    case QueryFlyingNotification(beforeTimestamp) =>
      LogUtil.debug(logTitle, s"query flying notification $beforeTimestamp")
      queryFlyingNotification(beforeTimestamp)
    case CheckAndSaveUnconfirmedNotification(notifications) =>
      LogUtil.debug(logTitle, s"check and save unconfirmed notifications ${notifications.size}")
      checkAndSaveUnconfirmedNotifications(notifications)
  }

  override def postStop(): Unit = {
    if (confirmNotificationTable != null) {
      confirmNotificationTable.close()
    }
    if (connection != null) {
      connection.close()
    }
  }

  def init(): (Connection, Table, Table, Table) = {
    val configuration =  HBaseConfiguration.create()
    configuration.set("hbase.zookeeper.property.clientPort", conf.port.toString)
    configuration.set("hbase.zookeeper.quorum", conf.quorum)
    val connection = ConnectionFactory.createConnection(configuration)
    val table = connection.getTable(TableName.valueOf("confirmed_notification"))
    val offlineNotificationTable = connection.getTable(TableName.valueOf("offline_notification"))
    val flyingNotificationTable = connection.getTable(TableName.valueOf("flying_notification"))
    (connection
      , table
      , offlineNotificationTable
      , flyingNotificationTable)
  }

  def saveConfirmedNotification(notification: JsObject): Unit = {
    val clientId = notification.fields.get("clientId").head.asInstanceOf[JsString].value
    val timestamp = notification.fields.get("timestamp").head.asInstanceOf[JsNumber].value.toLong
    val messageId =  notification.fields.get("messageId").head.asInstanceOf[JsString].value
    val put = new Put(Bytes.toBytes(s"$clientId-$timestamp-$messageId"))
    put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("q1"), Bytes.toBytes(notification.toString()))
    confirmNotificationTable.put(put)

    deleteFlyingNotification(timestamp, messageId)
  }

  def queryConfirmedNotification(clientId: String, beginTimestamp: Long, endTimestamp: Long): Unit = {
    val scan = new Scan()
    scan.setStartRow(Bytes.toBytes(s"$clientId-$beginTimestamp"))
    scan.setStopRow(Bytes.toBytes(s"$clientId-$endTimestamp"))
    val notifications = query(scan, confirmNotificationTable)

    sender() ! QueryNotificationsResult(clientId, notifications)
  }

  def saveOfflineNotification(notification: JsObject): Unit = {
    val clientId = notification.fields.get("clientId").head.asInstanceOf[JsString].value
    val timestamp = notification.fields.get("timestamp").head.asInstanceOf[JsNumber].value.toLong
    val put = new Put(Bytes.toBytes(s"$clientId-$timestamp"))
    put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("q1"), Bytes.toBytes(notification.toString()))
    offlineNotificationTable.put(put)
  }

  def queryOfflineNotification(clientId: String): Unit = {
    val scan = new Scan()
    scan.setRowPrefixFilter(Bytes.toBytes(clientId))
    val notifications = query(scan, offlineNotificationTable)

    sender() ! QueryNotificationsResult(clientId, notifications)
  }

  def saveFlyingNotification(notification: JsObject): Unit = {
    val timestamp = notification.fields.get("timestamp").get.asInstanceOf[JsNumber].value.toLong
    val messageId = notification.fields.get("messageId").get.asInstanceOf[JsString].value
    val put = new Put(Bytes.toBytes(s"$timestamp-$messageId"))
    put.addColumn(Bytes.toBytes("f1"), Bytes.toBytes("q1"), Bytes.toBytes(notification.toString()))
    flyingNotificationTable.put(put)
  }

  def queryFlyingNotification(beforeTimestamp: Long): Unit = {
    val scan = new Scan()
    scan.setStartRow(Bytes.toBytes((beforeTimestamp-24*60*60*1000).toString))
    scan.setStopRow(Bytes.toBytes(beforeTimestamp.toString))
    val notifications = query(scan, flyingNotificationTable)

    sender() ! QueryNotificationsResult("", notifications)
  }

  def checkAndSaveUnconfirmedNotifications(notifications: Seq[JsObject]): Unit = {
    notifications.foreach( x => {
      val clientId = x.fields.get("clientId").head.asInstanceOf[JsString].value
      val timestamp = x.fields.get("timestamp").head.asInstanceOf[JsNumber].value.toLong
      val messageId =  x.fields.get("messageId").head.asInstanceOf[JsString].value
      val get = new Get(Bytes.toBytes(s"$clientId-$timestamp-$messageId"))
      val result = confirmNotificationTable.get(get)
      val value = result.getValue(Bytes.toBytes("f1"), Bytes.toBytes("q1"))
      if (value == null) {
        saveOfflineNotification(x)
      }
      deleteFlyingNotification(timestamp, messageId)
    })
  }

  private def deleteFlyingNotification(timestamp: Long, messageId: String): Unit = {
    val del = new Delete(Bytes.toBytes(s"$timestamp-$messageId"))
    flyingNotificationTable.delete(del)
  }

  private def query(scan: Scan, table: Table): Seq[JsObject] = {
    val scanner = table.getScanner(scan)
    val notifications = new mutable.ListBuffer[JsObject]
    scanner.forEach( result => {
      val notificationJsonString = Bytes.toString(result.getValue(Bytes.toBytes("f1"), Bytes.toBytes("q1")))
      val notification = notificationJsonString.parseJson.asJsObject
      notifications.+=(notification)
    })
    scanner.close()

    notifications
  }
}
