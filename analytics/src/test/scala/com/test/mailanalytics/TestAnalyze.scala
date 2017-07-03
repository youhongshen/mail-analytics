package com.test.mailanalytics

import org.scalatest.FunSuite


/**
  * Created by joan on 7/3/17.
  */
class TestAnalyze extends FunSuite {

  val spark = Analyze.getSparkSession()

  import spark.implicits._

  test("test non matching subject") {
    val data = Seq(
      ("jamie", 11, Seq("john"), "foo", "", "1"),
      ("john", 12, Seq("jamie"), "RE: ", "", "2")
    ).toDF("from", "date", "to", "subject", "proc_subj", "file")

    val result = Analyze.findResponseTime(spark, data)
    val it = result.toLocalIterator()
    assert(!it.hasNext)
  }

  test("test empty from or to fields") {
    val data = Seq(
      ("", 11, Seq("john"), "foo", "", "1"),  // empty from field
      ("john", 12, Seq(), "RE: ", "", "2")    // empty to field
    ).toDF("from", "date", "to", "subject", "proc_subj", "file")

    val result = Analyze.findResponseTime(spark, data)
    val it = result.toLocalIterator()
    assert(!it.hasNext)
  }

  test("reply with empty subject") {
    val data = Seq(
      ("jamie", 11, Seq("john"), "", "", "1"),
      ("john", 12, Seq("jamie"), "RE: ", "", "2")
    ).toDF("from", "date", "to", "subject", "proc_subj", "file")

    val result = Analyze.findResponseTime(spark, data)
    val it = result.toLocalIterator()
    val row1 = it.next()
    assert(row1.get(0) == "1")
    assert(row1.get(1) == "2")
    assert(row1.get(2) == "jamie")
    assert(row1.get(3) == "john")
    assert(row1.get(4) == 1)
    assert(row1.get(5) == "")
    assert(!it.hasNext)
  }

  test("test the basics") {
    val data = Seq(
      ("jamie", 40, Seq("john", "carol"), "RE: some stuff", "some stuff", "4"), // resp to email 1
      ("bee", 34, Seq("joe", "alice"), "RE: other", "other", "3"), // resp to email 2
      ("john", 22, Seq("alice", "carol", "jamie"), "some stuff", "some stuff", "1"), // orig email 1
      ("bob", 44, Seq("john"), "RE: foo", "foo", "6"), // reply to email 3
      ("tom", 12, Seq("john"), "RE: foo", "foo", "7"), // email w/ no reply
      ("tom", 12, Seq("victor"), "RE: foo", "foo", "8"), // email w/ no reply
      ("alice", 33, Seq("ben", "john", "bee"), "other", "other", "2"), // orig email 2
      ("john", 42, Seq("bob"), "foo", "foo", "5") // orig email 3
    ).toDF("from", "date", "to", "subject", "proc_subj", "file")

    val result = Analyze.findResponseTime(spark, data)
    result.show()
    val it = result.toLocalIterator()
    val row1 = it.next()
    assert(row1.get(0) == "2")
    assert(row1.get(1) == "3")
    assert(row1.get(2) == "alice")
    assert(row1.get(3) == "bee")
    assert(row1.get(4) == 1)
    assert(row1.get(5) == "other")

    val row2 = it.next()
    assert(row2.get(0) == "5")
    assert(row2.get(1) == "6")
    assert(row2.get(2) == "john")
    assert(row2.get(3) == "bob")
    assert(row2.get(4) == 2)
    assert(row2.get(5) == "foo")

    val row3 = it.next()
    assert(row3.get(0) == "1")
    assert(row3.get(1) == "4")
    assert(row3.get(2) == "john")
    assert(row3.get(3) == "jamie")
    assert(row3.get(4) == 18)
    assert(row3.get(5) == "some stuff")

    assert(!it.hasNext)

    /*
+----+---------+---------+-----+---------+----------+
|file|next_file|from     |recip|resp_time|   subject|
+----+---------+---------+-----+---------+----------+
|   2|        3|    alice|  bee|        1|     other|
|   5|        6|     john|  bob|        2|       foo|
|   1|        4|     john|jamie|       18|some stuff|
+----+---------+---------+-----+---------+----------+
     */
  }
}
