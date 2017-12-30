package com.github.sioncheng.push.log

import java.util.{Calendar, Date}


object DateUtil {


  def getTimestampOfNow(): Long = {
    now().getTime
  }

  def now(): Date = {
    val calendar = Calendar.getInstance()
    calendar.getTime
  }
}
