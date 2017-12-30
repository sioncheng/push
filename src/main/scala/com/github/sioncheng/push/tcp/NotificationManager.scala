package com.github.sioncheng.push.tcp

import akka.actor.{Actor, ActorRef}
import com.github.sioncheng.push.log.{DateUtil, LogUtil}
import com.github.sioncheng.push.storage._
import spray.json.JsObject

case class OfflineNotification(jsObject: JsObject)
case class FlyingNotification(jsObject: JsObject)
case class ConfirmedNotification(jsObject: JsObject)
case class CheckFlyingNotificationTimeout()

class NotificationManager(hBaseClient: ActorRef) extends Actor {

  val logTitle = "Notification Manager"

  import scala.concurrent.duration._
  implicit val ec = context.system.dispatcher
  context.system.scheduler.schedule(30 second, 30 second, self, new CheckFlyingNotificationTimeout)

  override def receive: Receive = {
    case OfflineNotification(jsObject) =>
      LogUtil.debug(logTitle, s"offline notification ${jsObject.toString()}")
      hBaseClient ! SaveOfflineNotification(jsObject)
    case FlyingNotification(jsObject) =>
      LogUtil.debug(logTitle, s"flying notification ${jsObject.toString()}")
      hBaseClient ! SaveFlyingNotification(jsObject)
    case ConfirmedNotification(jsObject) =>
      LogUtil.debug(logTitle, s"confirmed notification ${jsObject.toString()}")
      hBaseClient ! SaveConfirmedNotification(jsObject)
    case _ : CheckFlyingNotificationTimeout =>
      val timestamp = DateUtil.getTimestampOfNow()
      LogUtil.debug(logTitle, s"scan $timestamp flying notification")
      hBaseClient ! QueryFlyingNotification(timestamp)
    case QueryNotificationsResult(clientId, notifications) =>
      LogUtil.debug(logTitle, s"query notification result $clientId ${notifications.length}")
      clientId.length match {
        case 0 =>
          hBaseClient ! CheckAndSaveUnconfirmedNotification(notifications)
      }
  }
}
