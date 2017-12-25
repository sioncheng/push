package com.github.sioncheng.push.tcp

import java.nio.ByteBuffer

import akka.util.ByteString
import org.scalatest.{Matchers, WordSpecLike}

class CommandObjectParserSpec extends WordSpecLike with Matchers {

  "command parser" must {
    "be able to parse command" in {

      def createCommand(commandJson: String): ByteString = {
        val buffer = ByteString.fromString(commandJson)
        val len =  "%4d".format(buffer.length)
        ByteString.fromString(len).++(buffer)
      }


      val command1 = createCommand("""{"code":1,"data":{"clientId":"321234567890"}}""")
      val command2 = createCommand("""{"code":2,"data":{"clientId":"321234567890"}}""")
      val command3 = createCommand("""{"code":1,"data":{"clientId":"321234567891"}}""")
      val (command3_1, command3_2) = command3.splitAt(10)

      val commandParser = new CommandParser
      val result = commandParser.parseCommand(command1.++(command2).++(command3_1))
      result.isEmpty should be(false)
      println(result)

      val commandResult1 = result.get.commands.head
      val commandResult2 = result.get.commands.last
      commandResult1.isLeft should be(true)
      commandResult2.isLeft should be(true)
      commandResult1.left.get.code should be (1)
      commandResult2.left.get.code should be (2)

      val result2 = commandParser.parseCommand(command3_2)
      result2.isEmpty should be (false)
      val commandResult3 = result2.get.commands.head
      commandResult3.isLeft should be (true)
      commandResult3.left.get.data.toString().endsWith(""""321234567891"}""") should be (true)
    }
  }
}
