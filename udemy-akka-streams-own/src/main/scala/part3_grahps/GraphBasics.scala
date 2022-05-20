package part3_grahps

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

/*
Recap

Complex Akka Stream graphs
The Graph DSL

Non-linear components:
- fan-out
- fan-in

Fan-out components:
- Broadcast
- Balance

Fan-in components:
- Zip/ZipWith
- Merge
- Concat

 */
object GraphBasics extends App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - typing up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
      // return a shape object
    } // graph
  ) // runnable graph

  //  graph.run() // run the graph and materialize it

  /**
   * exercise 1: feed a source into 2 sinks at the same time (hint: use a broadcast)
   */
  val sinkEx1 = Sink.foreach[String](println)
  val graphEx1 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      val mapF = (id: String) => Flow[Int].map(elem => s"$id:$elem")

      input ~> broadcast

      broadcast.out(0) ~> mapF("#1") ~> sinkEx1
      broadcast.out(1) ~> mapF("#2") ~> sinkEx1

      ClosedShape
    }
  )

//  graphEx1.run()

  // Another solution

  val firstSink = Sink.foreach[Int](x => println(s"First sink: $x"))
  val secondSink = Sink.foreach[Int](x => println(s"Second sink: $x"))

  // step 1
  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - declaring components
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - typing up the components
      //      input ~> broadcast
      //      broadcast.out(0) ~> firstSink
      //      broadcast.out(1) ~> secondSink
      // another approach
      input ~> broadcast ~> firstSink // implicit port numbering
      broadcast ~> secondSink

      // step 4
      ClosedShape
    }
  )

  /**
   * Exercise 2
   */
  val fastSourceSleep = Source(1 to 1000).map { x => Thread.sleep(1); x }
  val slowSourceSleep = Source(1 to 1000).map { x => Thread.sleep(100); x }

  val sourceToTwoSinksGraph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val balance = builder.add(Balance[Int](2))
      val merge = builder.add(Merge[Int](2))

      fastSourceSleep ~> merge.in(0)
      slowSourceSleep ~> merge.in(1)

      merge ~> balance

      balance.out(0) ~> firstSink
      balance.out(1) ~> secondSink

      ClosedShape
    }
  )

//  sourceToTwoSinksGraph2.run()

  // Another version of the exercise

  import scala.concurrent.duration._
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink1 number of elements: $count")
    count + 1
  })
  val sink2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink2 number of elements: $count")
    count + 1
  })
//  val sink2 = Sink.foreach[Int](x => println(s"Sink 2: $x"))
  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~>  balance ~> sink1
      slowSource ~> merge;    balance ~> sink2

      ClosedShape
    }
  )

  balanceGraph.run()
}
