package com.github.sioncheng.push.tcp

import java.nio.ByteBuffer
import java.util

import akka.util.ByteString
import com.github.sioncheng.push.tcp.Protocol.Command
import spray.json._
import DefaultJsonProtocol._
import com.github.sioncheng.push.log.LogUtil


class CommandParser {

  var expectBytesLen = 0
  val remainData: ByteBuffer = ByteBuffer.allocate(2052)
  var remainBytesLen = 0


  def parseCommand(data: ByteString): Option[List[Either[Command, Exception]]] = {

    LogUtil.debug(s"parse data ${data.utf8String}")

    LogUtil.debug(s"limit ${remainData.limit()} capacity ${remainData.capacity()}")
    if (remainData.limit() != remainData.capacity()) {
      remainData.compact()
    }
    remainData.put(data.asByteBuffer)
    remainBytesLen += data.length
    remainData.flip()

    if (expectBytesLen == 0) {
      if (remainBytesLen < 4) {
        None
      } else {
        val lenHead = new Array[Byte](4)
        remainData.get(lenHead)
        val headStr = new String(lenHead).trim
        println(s"head string [$headStr]")
        expectBytesLen = Integer.parseInt(headStr)
        remainBytesLen -= 4
      }
    }

    var continue = true
    var result:List[Either[Command, Exception]] = List.empty
    while(continue) {
      if (expectBytesLen > 2048) {
        result = result.++(List(Right(new IndexOutOfBoundsException(s"unexpected command length $expectBytesLen"))))
        continue = false
      } else if (remainBytesLen < expectBytesLen) {
        LogUtil.debug(s"expect ${expectBytesLen} but remain ${remainBytesLen}, break")
        continue = false
      } else {
        val commandData = new Array[Byte](expectBytesLen)
        remainData.get(commandData)
        val commandJsonString = new String(commandData)
        remainBytesLen -= expectBytesLen
        try {
          val commandObj = commandJsonString.parseJson.asJsObject
          val code = commandObj.fields.get("code").head.asInstanceOf[JsNumber].value.toInt
          val data = commandObj.fields.get("data").head.asJsObject
          result = result.++(List(Left(Command(code, data))))

          if (remainBytesLen > 4) {
            val lenHead = new Array[Byte](4)
            remainData.get(lenHead)
            expectBytesLen = Integer.parseInt(new String(lenHead).trim)
            remainBytesLen -= 4
          } else {
            continue = false
          }

        } catch {
         case e: Exception =>
            result = result.::(Right(e))
            continue = false
        }
      }
    }

    Some(result)
  }

}
