package com.kunyan.vipanalyzer.task.snowball


import java.sql.DriverManager
import com.kunyan.vipanalyzer.config.Platform
import com.kunyan.vipanalyzer.db.LazyConnections
import com.kunyan.vipanalyzer.parser.SnowBallParser
import com.kunyan.vipanalyzer.util.{StringUtil, DBUtil}
import org.apache.log4j.{Level, LogManager}

import scala.io.Source
import scala.xml.{Elem, XML}

/**
  * Created by yang on 5/21/16.
  */
object SendHomePage {


  def main(args: Array[String]): Unit = {

    val configFile = XML.loadFile(args(0))
    val sendTopic = (configFile \ "kafka" \ "send").text

    LogManager.getRootLogger.setLevel(Level.WARN)

    sendPages(configFile, LazyConnections(configFile), sendTopic)

  }

  def sendPages(configFile: Elem, lazyConn: LazyConnections, sendTopic: String): Unit = {

    Class.forName("com.mysql.jdbc.Driver")
    val connection = DriverManager.getConnection((configFile \ "mysql" \ "url").text, (configFile \ "mysql" \ "username").text, (configFile \ "mysql" \ "password").text)
    val sql = "select user_id from vip_snowball"
    val statement = connection.createStatement()
    val queryStatement = connection.createStatement()
    val datas = statement.executeQuery(sql)
    val query = "select count(*) from temp_article_snowball where user_id="

    var executedCount = 0

    while (datas.next()) {

      executedCount += 1
      println(executedCount)

      val userId = datas.getString("user_id")

      val countResult = queryStatement.executeQuery(query + userId)

      while (countResult.next) {

        val count = countResult.getInt(1)

        if (count == 0) {

          val homePage = s"https://xueqiu.com/$userId"
          val result = DBUtil.query(Platform.SNOW_BALL.id.toString, homePage, lazyConn)

          if (result == null) {
            lazyConn.sendTask(sendTopic, StringUtil.getUrlJsonString(Platform.SNOW_BALL.id, homePage, 2))
          } else {
            SnowBallParser.extractArticles(result._1, result._2, lazyConn, sendTopic)
          }

        }

      }

    }

    statement.close()
    connection.close()
  }

}
