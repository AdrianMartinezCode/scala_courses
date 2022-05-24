package part3_grahps

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, SourceShape, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}
import part2_primer.BackpressureBasics.fastSource

object GraphCycles extends App {

  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~>  mergeShape ~> incrementerShape
                    mergeShape <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(accelerator).run()
  // graph cycle deadlock!

  /*
    Solution 1: MergePreferred
   */
  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~>  mergeShape            ~> incrementerShape
                    mergeShape.preferred  <~ incrementerShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(actualAccelerator).run()

  /*
    Solution 2: buffers
   */
  val bufferRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~>  mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape

    ClosedShape
  }

//  RunnableGraph.fromGraph(bufferRepeater).run()

  /*
    Cycles risk deadlocking
    - add bounds to the number of elements in the cycle

    boundedness vs liveness
   */

  /**
   * Challenge: create a fan-in shape
   * - two inputs which will be fed with EXACTLY ONE number (1 and 1)
   * - output will emit an INFINITE FIBONACCI SEQUENCE based off those 2 numbers
   * 1, 2, 3, 5, 8 ...
   *
   * Hint: Use ZipWith and cycles, MergePreferred
   */

  val fiboGeneratorOwn = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val source = Source(List(1))

    val firstSourceShape = b.add(source)
    val secondSourceShape = b.add(source)

    val firstBroadcastShape = b.add(Broadcast[Int](2))

    val zipWithSum = ZipWith[Int, Int, Int](_ + _)
    val firstZipShape = b.add(zipWithSum)

    val firstMergeShape = b.add(MergePreferred[Int](1))
    val secondMergeShape = b.add(MergePreferred[Int](1))

    val finalBroadcastShape = b.add(Broadcast[Int](2))

    firstSourceShape ~> firstMergeShape; firstMergeShape.out ~> firstZipShape.in0 ; firstZipShape.out ~> finalBroadcastShape

    secondSourceShape ~> secondMergeShape ~>  firstBroadcastShape
                                              firstBroadcastShape.out(0) ~> firstMergeShape.preferred
                                              firstBroadcastShape.out(1) ~> firstZipShape.in1

    finalBroadcastShape.out(1) ~> secondMergeShape.preferred

    SourceShape(finalBroadcastShape.out(0))
  }

  import scala.concurrent.duration._
  Source.fromGraph(fiboGeneratorOwn)
    .throttle(2, 1 second)
    .to(Sink.foreach(println)).run()


  val fibonacciGenerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val zip = builder.add(Zip[BigInt, BigInt])
    val mergePreferred = builder.add(MergePreferred[(BigInt, BigInt)](1))
    val fiboLogic = builder.add(Flow[(BigInt, BigInt)].map { pair =>
      val last = pair._1
      val previous = pair._2

      Thread.sleep(100)
      (last + previous, last)
    })
    val broadcast = builder.add(Broadcast[(BigInt, BigInt)](2))
    val extractLast = builder.add(Flow[(BigInt, BigInt)].map(pair => pair._1))

    zip.out ~>  mergePreferred            ~> fiboLogic ~> broadcast ~> extractLast
                mergePreferred.preferred        <~        broadcast

    UniformFanInShape(extractLast.out, zip.in0, zip.in1)
  }

  val fiboGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val source1 = b.add(Source.single[BigInt](1))
      val source2 = b.add(Source.single[BigInt](1))
      val sink = b.add(Sink.foreach[BigInt](println))
      val fibo = b.add(fibonacciGenerator)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)
      fibo.out ~> sink

      ClosedShape
    }
  )


  fiboGraph.run()


  //  val fiboGenerator = GraphDSL.create() { implicit b =>
  //    import GraphDSL.Implicits._
  //
  //    val firstSeqShape = b.add(Source(List(1)))
  //    val secondSeqShape = b.add(Source(List(1)))
  //
  //    val zipWithSum = ZipWith[Int, Int, Int](_ + _)
  //    val firstFanInShape = b.add(zipWithSum)
  //    val secondFanInShape = b.add(zipWithSum)
  //
  //    val mergePreferredShape = b.add(MergePreferred[Int](1))
  //
  //    val broadcastShape = b.add(Broadcast[Int](3))
  //
  //    firstSeqShape ~> firstFanInShape.in0
  //    secondSeqShape ~> firstFanInShape.in1   ; firstFanInShape.out ~> mergePreferredShape
  //
  //    mergePreferredShape.out ~> secondFanInShape.in0 ; secondFanInShape.out ~> broadcastShape.in
  //
  //    broadcastShape.out(1) ~> mergePreferredShape.preferred
  //    broadcastShape.out(2) ~> secondFanInShape.in1
  //
  //    SourceShape(broadcastShape.out(0))
  //  }


//  val fiboGenerator = GraphDSL.create() { implicit b =>
//    import GraphDSL.Implicits._
//
//    val source = Source(List(1))
//
//    val firstSourceShape = b.add(source)
//    val secondSourceShape = b.add(source)
//
//    val firstBroadcastShape = b.add(Broadcast[Int](2))
//
//    val zipWithSum = ZipWith[Int, Int, Int](_ + _)
//    val firstZipShape = b.add(zipWithSum)
//    val secondZipShape = b.add(zipWithSum)
//
//    val firstMergeShape = b.add(MergePreferred[Int](1))
//    val secondMergeShape = b.add(MergePreferred[Int](1))
//
//    val finalBroadcastShape = b.add(Broadcast[Int](3))
//
//
//    firstSourceShape.out        ~> firstZipShape.in0
//    secondSourceShape.out ~> firstBroadcastShape.in ;   firstBroadcastShape.out(0)  ~> firstZipShape.in1
//
//    firstBroadcastShape.out(1) ~> secondMergeShape
//
//    firstZipShape.out ~> firstMergeShape ~> secondZipShape.in0 ; secondZipShape.out ~> finalBroadcastShape.in
//
//    finalBroadcastShape.out(1) ~> secondMergeShape.preferred ; secondMergeShape ~> secondZipShape.in1
//    finalBroadcastShape.out(2) ~> firstMergeShape.preferred
//
//
//    SourceShape(finalBroadcastShape.out(0))
//  }
}
