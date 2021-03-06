package flinkSrc

import java.io.File

import com.Inkbamboo.beans.UserBehavior
import org.apache.flink.api.java.io.PojoCsvInputFormat
import org.apache.flink.api.java.tuple.Tuple
import org.apache.flink.api.java.typeutils.{PojoTypeInfo, TypeExtractor}
import org.apache.flink.core.fs.Path
import org.apache.flink.streaming.api.scala.{OutputTag, StreamExecutionEnvironment}
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala.function.ProcessWindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

/**
  * Created By InkBamboo
  * Date: 2019/3/19 14:49
  * Calm Positive
  * Think Then Ask
  *
  * DataStream功能测试验证代码
  */
object DataStreamTest extends App {

  val senv = StreamExecutionEnvironment.getExecutionEnvironment
  //设置时间窗口的类型：
  // eventime  事件时间：数据产生时自带的时间
  // processtime   处理时间：平台处理数据的时间
  // IngestionTime  摄入时间:数据进入平台的时间
  senv.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

 // val denv = ExecutionEnvironment.getExecutionEnvironment
 // val element =  senv.fromElements(new Tuple3("a",2,1511658000),new Tuple3("b",4,1511658000),new Tuple3("a",5,1511658000),new Tuple3("b",2,1511658000))

//数据源是用户消费行为数据
  val fileurl = DataStreamTest.getClass.getClassLoader.getResource("UserBehavior.csv")
  val filepath = Path.fromLocalFile(new File(fileurl.toURI))

  //构建数据需要的typeinfomation信息
  val pojoType = TypeExtractor.createTypeInfo(classOf[UserBehavior]).asInstanceOf[PojoTypeInfo[UserBehavior]]
  val fieldOrder = Array[String]("userId", "itemId", "categoryId", "behavior", "timestamp")
  val csvinput = new PojoCsvInputFormat[UserBehavior](filepath,pojoType,fieldOrder) // .isSkippingFirstLineAsHeader  设置跳过表头(表头是类名)
  /**
    * 构建数据源可以从下往上推。
    * 从createInput往上推需要什么参数，之后构建需要的参数
    */
  val datastream = senv.createInput(csvinput)

  //val dataset = denv.createInput(csvinput)


  /************************************************************************************
    * Stream operator测试
    ***********************************************************************************/
 val dres =  datastream
    //为数据流中的元素分配时间戳，并定期创建watermark，以指示事件时间进度。
    //时间是秒级别的转换为毫秒级别
    .assignAscendingTimestamps(x=>x.timestamp*1000)
    //根据用户动作分组
   .map(x=>(x.userId,x.itemId,1))
    .keyBy(0,1)

    /*************************************************************************************
      * [[org.apache.flink.streaming.api.scala.KeyedStream]]
      * 时间窗口类型
      *
      * 滑动窗口，滚动窗口，会话窗口，
      * 根据时间类型又有细分为：eventimeslidewindow，processtimetumblewindow等
      * 具体类型查看：[[org.apache.flink.streaming.api.windowing.assigners.WindowAssigner]]的实现
      */
    //1.滑动窗口 sliding time windows
    //.timeWindow(Time.minutes(30),Time.minutes(5))
    //2.滚动窗口 tumbling time windows
    .timeWindow(Time.minutes(30))
   .allowedLateness(Time.hours(1))
   //**************将晚到的数据单独保存到一个测输出中,用于后续数据修复使用
   .sideOutputLateData(new OutputTag[(Long, Long, Int)]("lateDataSave"))
    //3.会话窗口  session time window
    // 1).设置固定大小的session窗口
    //.window(EventTimeSessionWindows.withGap(Time.minutes(10)))
    // 2).动态设置session窗口  sessionWindowTimeGapExtractor用于从数据中提取时间字段
    /*.window(EventTimeSessionWindows.withDynamicGap(new SessionWindowTimeGapExtractor[UserBehavior](){
    override def extract(element: UserBehavior): Long = {
      element.timestamp*1000
    }
  }))*/
    /********************************************************************************
      * 计数窗口
      *  sliding count windows   滑动计数窗口   sliding time window  每个窗口向后滑动多少个元素
      *  tumble count windows    滚动计数窗口   size个元素作为一个窗口
      */
    //.countWindow(100)
    //.countWindow(100,20)
    //对每个key的分组进行reduce处理  下面两个用法基本一致
    //.reduce((x,y)=>new UserBehavior(x.userId,y.itemId,y.categoryId,x.behavior,(x.timestamp+y.timestamp)/2))
    /*.reduce(new ReduceFunction[UserBehavior] {
      override def reduce(x: UserBehavior, y: UserBehavior): UserBehavior = {
        new UserBehavior(x.userId,y.itemId,y.categoryId,x.behavior,(x.timestamp+y.timestamp)/2)
      }
      })*/
    //一个窗口对数据根据给定的字段，确定最大值max，最小值min，对指定字段求和，根据指定字段.
    //对于嵌套类型可以使用点来做类型下推，"field.field2"
    //.max("timestamp")
    //.min(4)
    //.sum(4)
    //--------------------------------------------------------------
    // 状态函数  ?????????????
    //--------------------------------------------------------------
    //创建一个新的DataStream，其中只包含满足给定有状态筛选器谓词的元素。要使用状态分区，必须使用. keyby(..)定义一个键，
    // 在这种情况下，每个键将保留一个独立的状态。 【注意】，用户状态对象UserBehavior需要是可序列化的
    //第二个参数，具体作用不明,状态判定的函数???????。
    //.filterWithState[UserBehavior]((x,y)=>(x.timestamp%10>5,y))
    //.mapWithState[Long,UserBehavior]((x,y)=>(x.timestamp*10000,y))
    //.flatMapWithState()
    //将键控流发布为可查询的ValueState实例。返回类型QueryableStateStream
    //.asQueryableState()
    /*****************************************************************************************
      *
      *[[org.apache.flink.streaming.api.scala.DataStream]]类operator
      * **************************************************************************************
      */
    //返回值类型为DataStream
    //.reduce((x,y)=>new UserBehavior(x.userId,x.itemId,y.categoryId,y.behavior,y.timestamp))
   //获取底层java DataStream对象
     //.javaStream
   //返回TypeInformation类型的信息
    // .dataType
  //获取执行参数对象，并用于获取指定的参数配置或者设置配置参数
   //  .executionConfig
  //设置最大并行度，设置了job动态缩放的上限
   //  .setMaxParallelism(200)
   //获得算子执行的最小资源量，包括cpu，内存等 ResourceSpec{cpuCores=0.0, heapMemoryInMB=0, directMemoryInMB=0, nativeMemoryInMB=0, stateSizeInMB=0}
     //.minResources
  //返回此操作的首选资源  ResourceSpec{cpuCores=0.0, heapMemoryInMB=0, directMemoryInMB=0, nativeMemoryInMB=0, stateSizeInMB=0}
     //.preferredResources
   //设置该datastream的名字，用于监控界面以及日志中使用
   //  .name("datastreamTest")
   //为当前的operator设置id，该id在该job中必须唯一，主要使用在开启checkpoint的情况下用于job的恢复。
   //  .uid("reduce")

//使用自定义ProcessWindowFunction并将部分数据写入到测输出流中,此处的processFunction方法的传入参数必须参照上一个operator产出的数据类型
     .process(new myprocessFuntion)
   //《《《《《《《《《《《《获取到上面定义的侧输出数据，根据id，具体类型根据使用调整,边缘输出的id定义在:myprocessFuntion中
    //.getSideOutput[(Long, Long, Int)](OutputTag[(Long, Long, Int)]("side-output"))

   //聚合函数的使用：统计根据userid和itermid分组的量
  /* .aggregate(new AggregateFunction[(Long,Long,Int),(Long,Long,Int),(Long,Long,Int)] {
   override def createAccumulator(): (Long, Long, Int) = (0,0,0)

   override def add(value: (Long, Long, Int), accumulator: (Long, Long, Int)): (Long, Long, Int) = (value._1,value._2,value._3+accumulator._3)

   override def getResult(accumulator: (Long, Long, Int)): (Long, Long, Int) = {
     accumulator
   }

   override def merge(a: (Long, Long, Int), b: (Long, Long, Int)): (Long, Long, Int) = (a._1,a._2,a._3+b._3)
 })*/

  //dres.print()

  println("------------------------")
  //《《《《《《《《《《《获取边缘输出的结果集
  val sideoutput = dres.getSideOutput(new OutputTag[(Long, Long, Int)]("side-output"))

  sideoutput.print()
  senv.execute("DataStreamTest")

}

//slide output  侧输出流 操作将数据流中的部分数据导入到侧输出流中
class myprocessFuntion extends ProcessWindowFunction[Tuple3[Long,Long,Int],Tuple3[Long,Long,Int],Tuple,TimeWindow]{
  override def process(key: Tuple, context: Context, elements: Iterable[(Long, Long, Int)], out: Collector[(Long, Long, Int)]): Unit = {
    for(ele<-elements){
      out.collect(ele)
      if(key.getField(0).asInstanceOf[Long]%10==5){
        // 《《《《《《《《《《《《 定义边缘输出的id
        context.output(new OutputTag[(Long, Long, Int)]("side-output"),ele)
      }
    }
  }
}

//pojo




