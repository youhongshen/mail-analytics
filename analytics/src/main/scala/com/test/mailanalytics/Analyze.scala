package com.test.mailanalytics

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import com.databricks.spark.avro._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions._

/**
  * Created by joan on 6/30/17.
  */
object Analyze {

  def main(args: Array[String]): Unit = {

    val spark = getSparkSession()
    import spark.implicits._
    val df = spark.read.avro("/tmp/emails.avro")

    //    df.printSchema()

    //    q1 - 3 recipients who received the most direct emails
    df.filter($"to_count" === 1).groupBy($"to" (0)).count().orderBy(desc("count")).show(3, false)

    //    q2 - 3 senders who sent the most number of broadcast emails
    df.filter($"to_count" > 1).groupBy("from").count().orderBy(desc("count")).show(3, false)

    //    q3 - 5 emails with the fastest response time

    val result = findResponseTime(spark, df)
    result.show(5, false)

  }

  def getSparkSession() = {
    val spark = SparkSession
      .builder()
      .appName("Enron mail analytics")
      .master("local")
      .getOrCreate()

    spark
  }

  def findResponseTime(spark: SparkSession, df: DataFrame) = {

    //    to use the $ notation
    import spark.implicits._

    val inArray = udf((array: Seq[String], item: String) =>
      array != null && item != null && (array contains item)
    )

    val isSubString = udf((origSubj: String, replySubj: String) =>
      origSubj != null && replySubj != null && replySubj.contains(origSubj)
    )

    //    since they are ordered by date (ascending),
    //    the "x" fields refer to the original email
    //    and the next_x fields refer to the replied email
    //    e.g.: date is the date of the orig email and next_date is the date of the replied email
    df
      .select(
        $"*",
        coalesce(lead($"date", 1).over(Window.partitionBy($"proc_subj").orderBy($"date"))).as("next_date"),
        coalesce(lead($"from", 1).over(Window.partitionBy($"proc_subj").orderBy($"date"))).as("next_from"),
        coalesce(lead($"to", 1).over(Window.partitionBy($"proc_subj").orderBy($"date"))).as("next_to"),
        coalesce(lead($"file", 1).over(Window.partitionBy($"proc_subj").orderBy($"date"))).as("next_file"),
        coalesce(lead($"subject", 1).over(Window.partitionBy($"proc_subj").orderBy($"date"))).as("next_subj")
      )
      .filter(inArray($"next_to", $"from") && inArray($"to", $"next_from") && isSubString($"subject", $"next_subj"))
      .select($"file".as("orig_email"), $"next_file".as("replied_email"), $"from", $"next_from".as("recipient"),
        ($"next_date" - $"date").cast("int").as("resp_time"), $"subject")
      .orderBy($"resp_time")
  }
}
