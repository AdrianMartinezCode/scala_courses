package part3_grahps

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}
/*

Materializable components

GraphDSL.create(printer, counter)((printerMat, counterMat) => counterMat) {
  implicit builder => (printShape, counterShape) =>
}

 */
object GraphMaterializedValues extends App {

  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((count, _) => count + 1)

  /*
    A composite component (sink)
    - prints out all string which are lowercase
    - COUNTS the strings that are short (< 5 chars)
   */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(printer, counter)((printerMatValue, counterMatValue) => counterMatValue) { implicit b => (printerShape, counterShape) =>
      import GraphDSL.Implicits._

      val broadcast = b.add(Broadcast[String](2))
      val lowercaseFilter = b.add(Flow[String].filter(word => word == word.toLowerCase))
      val shortStringFilter = b.add(Flow[String].filter(_.length < 5))

      broadcast ~> lowercaseFilter ~> printerShape
      broadcast ~> shortStringFilter ~> counterShape

      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
  val shortStringsCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringsCountFuture.onComplete {
    case Success(count) => println(s"The total number of short strings is: $count")
    case Failure(exception) => println(s"The count of short strings failed: $exception")
  }

  /**
   * Exercise
   */

  def enhanceFlowOwn[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val flowMaterialized = Flow[A].fold(0)((c, _) => c + 1).toMat(Sink.last)(Keep.right)
    Flow.fromGraph(
      GraphDSL.create(flowMaterialized) { implicit b => flowShape =>
        import GraphDSL.Implicits._

        val broadcast = b.add(Broadcast[A](2))
        val originalFlow = b.add(flow)

        broadcast ~> flowShape.in
        broadcast ~> originalFlow.in

        FlowShape(broadcast.in, originalFlow.out)
      }
    )
  }

  /*
    Hint: use a broadcast and a Sink fold
   */

  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)
    Flow.fromGraph(
      GraphDSL.create(counterSink){ implicit b => counterSinkShape =>
        import GraphDSL.Implicits._

        val broadcast = b.add(Broadcast[B](2))
        val originalFlowShape = b.add(flow)

        originalFlowShape ~> broadcast ~> counterSinkShape

        FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhancedFlowCountFuture = simpleSource
    .viaMat(enhanceFlow(simpleFlow))(Keep.right)
    .toMat(simpleSink)(Keep.left)
    .run()

  enhancedFlowCountFuture.onComplete {
    case Success(count) => println(s"$count elements went through the enhanced flow")
    case _ => println("Something failed")
  }
}
