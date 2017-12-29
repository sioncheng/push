package com.github.sioncheng.push.tcp

import java.nio.ByteBuffer
import akka.util.ByteString
import com.github.sioncheng.push.tcp.Protocol.CommandObject
import spray.json._
import com.github.sioncheng.push.log.LogUtil

case class ParseResult(commands:  List[Either[CommandObject, Exception]])


class CommandParser {

  var expectBytesLen = 0
  val remainData: ByteBuffer = ByteBuffer.allocate(2052)
  var remainBytesLen = 0

  val logTitle = "Command Parser"

  def parseCommand(data: ByteString): Option[ParseResult] = {

    LogUtil.debug(logTitle, s"parse data ${data.utf8String}")

    if (remainData.limit() != remainData.capacity()) {
      remainData.compact()
    }
    remainData.put(data.asByteBuffer)
    remainBytesLen += data.length
    remainData.flip()

    var continue = true
    var result:List[Either[CommandObject, Exception]] = List.empty

    if (expectBytesLen == 0) {
      if (remainBytesLen < 4) {
        None
      } else {
        try {
          val lenHead = new Array[Byte](4)
          remainData.get(lenHead)
          val headStr = new String(lenHead).trim
          expectBytesLen = Integer.parseInt(headStr)
          remainBytesLen -= 4
        } catch {
          case e: Exception =>
            result = result.++(List(Right(e)))
            continue = false
        }
      }
    }


    while(continue) {
      if (expectBytesLen > 2048) {
        result = result.++(List(Right(new IndexOutOfBoundsException(s"unexpected command length $expectBytesLen"))))
        continue = false
      } else if (remainBytesLen < expectBytesLen) {
        LogUtil.debug(logTitle, s"expect ${expectBytesLen} but remain ${remainBytesLen}, break")
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
          result = result.++(List(Left(CommandObject(code, data))))

          if (remainBytesLen > 4) {
            val lenHead = new Array[Byte](4)
            remainData.get(lenHead)
            expectBytesLen = Integer.parseInt(new String(lenHead).trim)
            remainBytesLen -= 4
          } else {
            expectBytesLen = 0
            continue = false
          }

        } catch {
         case e: Exception =>
            result = result.++(List(Right(e)))
            continue = false
        }
      }
    }

    Some(ParseResult(result))
  }

}
