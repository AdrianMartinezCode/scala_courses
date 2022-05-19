package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Random, Success}

/*

Goal:
getting a meaningful value out of a running stream

Materializing
Components are static until they run

val graph = source.via(flow).to(sink)
val result = graph.run() // result is a materialized value

A graph is a "blueprint" for a stream

Running a graph allocates the right resources
- instantiating actors, allocating thread pools
- sockets, connections
- etc - everything is transparent

Running a graph = materializing

Materialized Values

Materializing a graph = materializing all components
- each component produces a materialized value when run
- the graph produces a single materialized value
- our job to choose which one to pick

A component can materialize multiple times
- you can reuse the same component in different graphs
- different runs = different materializations!

A materialized value can be ANYTHING!

Recap

Materializing a graph = materializing all components
- each component produces a materialized value when run
- the graph produces a single materialized value
- our job is to choose which one to pick

A component can materialize multiple times

A materialized value can be ANYTHING

 */
object MaterializingStreams extends App {

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  val sumFuture = source.runWith(sink)
  sumFuture.onComplete {
    case Success(value) => println(s"The sum of all elements is: $value")
    case Failure(ex) => println(s"The sum of the elements could not be computed: $ex")
  }

  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)
  simpleSource.viaMat(simpleFlow)((sourceMat, flowMat) => flowMat) // or we can use Keep.right
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  graph.run().onComplete {
    case Success(_) => println("Stream processing finished")
    case Failure(ex) => println(s"Stream processing failed with: $ex")
  }

  // sugars
//  val sum = Source(1 to 10).runWith(Sink.reduce(_ + _)) // source.to(Sink.reduce)(Keep.right) -> by default
//  sum.onComplete{
//    case Success(v) => println(s"Stream final value $v")
//    case Failure(ex) => println(s"Stream sum failed $ex")
//  }

  Source(1 to 10).runReduce(_ + _) // exactly the same

  // backwards
  Sink.foreach[Int](println)
    .runWith(Source.single(42)) // source(..).to(sink...).run()
  // both ways
  Flow[Int].map(x => 2 * x)
    .runWith(simpleSource, simpleSink)

  /**
   * - return the last element out of a source (use Sink.last)
   * - compute the total word count out of a stream of sentences
   *    - map, fold, reduce
   */
  val sourceElems = Source(1 to 10000)

  source
    .runWith(Sink.last)
    .onComplete{
      case Success(elem) => println(s"Elem getted $elem")
    }


  val flowConvertToString = Flow[Int].map(_ => Random.nextString(Random.nextInt(50)))

  val sourceWithFlowString = sourceElems.viaMat(flowConvertToString)(Keep.right)

  val flowFold = Flow[String].fold(0)(_ + _.length)
  val flowReduce = Flow[String]
    .map(_.length)
    .reduce(_ + _)

  val sinkExercise = Sink.foreach[Int](elem => println(s"Completed sink $elem"))

  sourceWithFlowString
    .viaMat(flowFold)(Keep.right)
    .runWith(sinkExercise)

  sourceWithFlowString
    .viaMat(flowReduce)(Keep.right)
    .runWith(sinkExercise)


  /*
  Other solutions
   */
  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val sentenceSource = Source(List(
    "Akka is awesome",
    "I love streams",
    "Materialized values are killing me"
  ))
  val f = (currentWords: Int, newSentence: String) => currentWords + newSentence.split(" ").length

  val wordCountSink = Sink.fold[Int, String](0)(f)
  val g1 = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentenceSource.runWith(wordCountSink)
  val g3 = sentenceSource.runFold(0)(f)

  val wordCountFlow = Flow[String].fold[Int](0)(f)
  val g4 = sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5 = sentenceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
  val g6 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(sentenceSource, Sink.head)._2


}
