package com.test.mailanalytics

import org.apache.spark.sql.SparkSession
import com.databricks.spark.avro._
import org.apache.spark.sql.functions._
//import sqlContext.implicits._
//import org.apache.spark.sql.SQLContext.implicts._

/**
  * Created by joan on 6/30/17.
  */
object Analyze {

  def main(args: Array[String]): Unit = {
//    val conf = new SparkConf().setAppName("enron mail analytics").setMaster("local")
//    val sc = new SparkContext(conf)

    val spark = SparkSession
      .builder()
      .appName("Enron mail analytics")
      .master("local")
      .getOrCreate()

//    todo - how to make the schema to enforce fields to be non nullable
    val df = spark.read.avro("/tmp/emails.avro")
//    df.printSchema()
//    df.show()
//    df.select("from", "date").show()
    df.groupBy("from").count().orderBy(desc("count")).show()
//    df.filter("to equal 'Peter Styles'").show()
//    df.filter(col("to")).equal("Peter Styles'")
  }
}
