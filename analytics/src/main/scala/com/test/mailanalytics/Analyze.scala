package com.test.mailanalytics

import org.apache.spark.sql.{Row, SparkSession}
import com.databricks.spark.avro._
import org.apache.spark.sql.functions._

/**
  * Created by joan on 6/30/17.
  */
object Analyze {

  def main(args: Array[String]): Unit = {

    val spark = SparkSession
      .builder()
      .appName("Enron mail analytics")
      .master("local")
      .getOrCreate()

//    to use the $ notation
    import spark.implicits._

    val df = spark.read.avro("/tmp/emails.avro")
    df.printSchema()

//    q1 - 3 recipients who received the most direct emails
    df.filter($"to_count" === 1).groupBy($"to"(0)).count().orderBy(desc("count")).show(3, false)

//    q2 - 3 senders who sent the most number of broadcast emails
    df.filter($"to_count" > 1).groupBy("from").count().orderBy(desc("count")).show(3, false)
  }
}
