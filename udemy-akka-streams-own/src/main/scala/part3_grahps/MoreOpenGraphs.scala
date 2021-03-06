package part3_grahps

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

import java.util.Date

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
    Example: Max3 operator
    - 3 inputs of type int
    - the maximum of the 3
   */

  val max3StaticGraph = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val max1 = b.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = b.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is: $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val max3Shape = b.add(max3StaticGraph)

      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)

      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

  max3RunnableGraph.run()

  /*
    Non-uniform fan out shape

    Processing bank transactions
    Txn suspicious if amount > 10000

    Streams component for txns
    - output1: let the transaction go through
    - output2: suspicious txn ids

   */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("5273890572", "Paul", "Jim", 100, new Date),
    Transaction("3578902532", "Daniel", "Jim", 100000, new Date),
    Transaction("5489036033", "Jim", "Alice", 7000, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious transaction ID: $txnId"))

  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val broadcast = b.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = b.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val txnIdExtractor = b.add(Flow[Transaction].map(txn => txn.id))

    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val suspiciousTxnShape = b.add(suspiciousTxnStaticGraph)

      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()

}
