package com.github.sioncheng.push.storage

import akka.actor.Actor
import com.github.sioncheng.push.log.LogUtil
import spray.json.JsObject

case class OfflineNotification(jsObject: JsObject)
case class FlyingNotification(jsObject: JsObject)
case class ConfirmedNotification(jsObject: JsObject)

class NotificationManager extends Actor {

  val logTitle = "Notification Manager"

  override def receive: Receive = {
    case OfflineNotification(jsObject) =>
      LogUtil.debug(logTitle, s"offline notification ${jsObject.toString()}")
    case FlyingNotification(jsObject) =>
      LogUtil.debug(logTitle, s"flying notification ${jsObject.toString()}")
    case ConfirmedNotification(jsObject) =>
      LogUtil.debug(logTitle, s"confirmed notification ${jsObject.toString()}")
  }
}
