package com.github.sioncheng.push.log

import java.text.SimpleDateFormat
import java.util.Date

object LogUtil {
  def debug(title: String, message: String): Unit = {
    println(formatLog(title, message, "DEBUG"))
  }

  def info(title: String, message: String): Unit = {
    println(formatLog(title, message, "INFO"))
  }

  def warn(title: String, message: String): Unit = {
    println(formatLog(title, message, "WARN"))
  }

  def error(title: String, message: String): Unit = {
    println(formatLog(title, message, "ERROR"))
  }

  private def formatLog(title: String, message: String, level: String): String = {
    s"[$getNowDate()] [$level] $title -- $message"
  }

  def getNowDate():String={
    val now:Date = new Date()
    val  dateFormat:SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
    dateFormat.format( now )
  }
}
