package com.github.sioncheng.push.tcp

import org.scalatest.{Matchers, WordSpecLike}
import spray.json._
import com.github.sioncheng.push.tcp.Protocol.Command

class SprayJsonSpec extends WordSpecLike with Matchers {

  "spray json library" must {
    "be able to parse json string" in {
      val jsonStr = """{"a":1,"b":{"c":1}}"""
      val jso = jsonStr.parseJson
      println(jso.prettyPrint)
    }

    "be able to serialize command object" in {
      val jsonStr = """{"a":1,"b":{"c":1}}"""
      val jso = jsonStr.parseJson.asJsObject
      val cmd = Command(1, jso)
      import Protocol.CommandProtocol._
      val cmdJson = cmd.toJson
      println(cmdJson.prettyPrint)
    }
  }
}
