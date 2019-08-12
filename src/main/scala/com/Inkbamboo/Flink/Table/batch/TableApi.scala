package com.Inkbamboo.Flink.Table.batch

import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.core.fs.FileSystem.WriteMode
import org.apache.flink.table.api.{Table, TableEnvironment, Types}
import org.apache.flink.table.sinks.{CsvTableSink, TableSink}
import org.apache.flink.types.Row
import org.apache.flink.table.api.scala._
import org.apache.flink.table.descriptors.FileSystem


/**
  * Created By InkBamboo
  * Date: 2018/12/17 10:41
  * Calm Positive
  * Think Then Ask
  *
  * desc:  tableAPI源码中example样例测试
  */
/**
  * 加载csv文件处理
  * InkBamboo:测试通过
  */
object batchTable{
  import org.apache.flink.api.scala._
  def main(args: Array[String]): Unit = {

    //初始化环境
    val batchenv = ExecutionEnvironment.getExecutionEnvironment
    val tableEnv = TableEnvironment.getTableEnvironment(batchenv)

    //加载csv文件
    val csvds:DataSet[(String,String,String,String,String)] = batchenv.readCsvFile("target/classes/UserBehavior.csv")

    /*******************************************************************************************************************************
      * 创建表 function One
      */

    tableEnv.registerDataSet("TestTwo",csvds,'id,'id2,'id3,'flag,'number)

    val queryRes = tableEnv.sqlQuery("select id,id2,id3,flag,number from TestTwo")

      /** 此处的dataset需要指定typeinformation */
      .toDataSet[(String, String, String, String, String)]

      //.print()

    /**
      * 创建表 function Two
      */
    val tbl =  tableEnv.fromDataSet(csvds)

      /**为数据指定每列的名字。*/
      .as("id,id2,id3,flag,number")

    /**注册成表*/
    tableEnv.registerTable("testOne",tbl)

    val restbl =  tableEnv.sqlQuery("select * from testOne order by id limit 1000")

    //部分数据输出
    //数据已csv的格式存储到hdfs上
    tableEnv.toDataSet[(String,String,String,String,String)](restbl)
      .setParallelism(1)
      .writeAsCsv("hdfs:///zh/csvSinkTable/csvSinkTable.csv","\n",",",WriteMode.OVERWRITE)


    /******************************************************************************************************************
      * tableApi operater操作
      */







    //更新表数据，此操作可以直接将数据写入外部系统，或者只更新内部注册的表
    /*tableEnv.sqlUpdate(
          """
            |insert into CsvSinkTable select * from TestTwo
          """.stripMargin)*/

    /******************************************************************************************************************
      * 结果数据写入外部文件系统 function One
      */
    val writeSink = new CsvTableSink(".\\res100",",",1,WriteMode.OVERWRITE)
    //restbl.writeToSink(writeSink)

    /**
      * 结果数据写入外部文件系统 function Two
      */
    //restbl.toDataSet[(String,String,String,String,String)]
    // .setParallelism(1).writeAsCsv("res.csv","\n",",",WriteMode.OVERWRITE)

    /**
      * 注册并使用tablesink
      *   将table数据写入到外部文件系统，数据库，消息队列等
      *
      *   测试写入文件中成功
      */

    //创建tablesink
    val tblsink = new CsvTableSink("./csvSinkTable/csvSinkTable.csv",",",1,WriteMode.OVERWRITE)

    //定义元素名称和类型
    val fieldName:Array[String] = Array("a","b","c","d","e")
    val fieldType:Array[TypeInformation[_]] = Array(Types.STRING,Types.STRING,Types.STRING,Types.STRING,Types.STRING)

    //注册需要写入的表的名称为"csvSinkTable"
    tableEnv.registerTableSink("CsvSinkTable",fieldName,fieldType,tblsink)
    //更新表数据，此操作可以直接将数据写入外部系统，或者只更新内部注册的表
    /*tableEnv.sqlUpdate(
          """
            |insert into CsvSinkTable select * from TestTwo
          """.stripMargin)*/


    //数据写出到hdfs上  **未成功
   /* tableEnv.connect((new FileSystem).path("hdfs:///zh/csvSinkTable/csvSinkTable.csv")).registerTableSink("hdfsCsvSinkTable")

    tableEnv.sqlUpdate(
      """
        |insert into hdfsCsvSinkTable select * from TestTwo
      """.stripMargin)*/


    batchenv.execute("table_test")
  }
}


/**
  * InkBamboo :测试通过数据正常输出
  */
object TableAPITest {
  import org.apache.flink.api.scala._

  def main(args: Array[String]): Unit = {
    val arr = Array(new DataPackage(1,"a",2),new DataPackage(2,"b",3),new DataPackage(3,"c",4))
    val env = ExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    val tblenv = TableEnvironment.getTableEnvironment(env)

    val source =  env.fromCollection(arr)
    val tbl1 = tblenv.fromDataSet(source).as("id,name,id2")   //as用逗号分割，重命名列名
    tblenv.registerTable("dataPackage",tbl1)

    val tblAPIResult = tblenv.scan("dataPackage").select("id,name,id2")
    tblenv.toDataSet[Row](tblAPIResult).print()

    //println("-----------------id--------"+tblAPIResult.toString())

    env.execute()

  }
  def flatfun(): Unit ={

  }
}

case class DataPackage(id:Int,name:String,id2:Int) extends Serializable
case class tableSchema(id:String,id2:String,id3:String,flag:String,number:String)