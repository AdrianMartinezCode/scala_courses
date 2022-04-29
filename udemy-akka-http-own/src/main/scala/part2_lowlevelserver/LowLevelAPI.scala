package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object LowLevelAPI extends App {

    implicit val system = ActorSystem("LowLevelServerAPI")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    val serverSource = Http().bind("localhost", 8000)
    val connectionSink = Sink.foreach[IncomingConnection] { connection =>
        println(s"Accepted incoming connection from: ${connection.remoteAddress}")
    }

    val serverBindingFuture = serverSource.to(connectionSink).run()
    serverBindingFuture.onComplete {
        case Success(binding) =>
            println("Server binding successful.")
            binding.terminate(2 seconds)
        case Failure(ex) => println(s"Server binding failed: $ex")
    }

    /*
    Method 1: synchronously serve HTTP responses
     */
    val requestHandler: HttpRequest => HttpResponse = {
        case HttpRequest(HttpMethods.GET, uri, headers, entity, protocol) =>
            HttpResponse(
                StatusCodes.OK, // HTTP 200
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     Hello from Akka HTTP!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            )

        case request: HttpRequest =>
            request.discardEntityBytes()
            HttpResponse(
                StatusCodes.NotFound,
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     OOPS! The resource can't be found!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            )
    }

    val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
        connection.handleWithSyncHandler(requestHandler)
    }

//    Http().bind("localhost", 8080).runWith(httpSyncConnectionHandler)
    // shorthand version
//    Http().bindAndHandleSync(requestHandler, "localhost", 8080)

    /*
    Method 2: serve back
     */
    val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.GET, Uri.Path("/home"), headers, entity, protocol) => // mehtod, uri, http headers, content and the protocol
            Future(HttpResponse(
                StatusCodes.OK, // HTTP 200
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     Hello from Akka HTTP!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            ))

        case request: HttpRequest =>
            request.discardEntityBytes()
            Future(HttpResponse(
                StatusCodes.NotFound,
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     OOPS! The resource can't be found!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            ))
    }
    val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
        connection.handleWithAsyncHandler(asyncRequestHandler)
    }

//    Http().bind("localhost", 8081).runWith(httpAsyncConnectionHandler)

//    Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 8081)
    /*
    Method 3: async via Akka Streams
     */
    val streamsBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
        case HttpRequest(HttpMethods.GET, Uri.Path("/home"), headers, entity, protocol) => // mehtod, uri, http headers, content and the protocol
            HttpResponse(
                StatusCodes.OK, // HTTP 200
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     Hello from Akka HTTP!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            )

        case request: HttpRequest =>
            request.discardEntityBytes()
            HttpResponse(
                StatusCodes.NotFound,
                entity = HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    """
                      |<html>
                      | <body>
                      |     OOPS! The resource can't be found!
                      | </body>
                      |</html>
                      |""".stripMargin
                )
            )
    }
//    val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
//        connection.handleWithAsyncHandler(asyncRequestHandler)
//    }

    Http().bind("localhost", 8082).runForeach { connection =>
        connection.handleWith(streamsBasedRequestHandler)
    }

    Http().bindAndHandle(streamsBasedRequestHandler, "localhost", 8082)

    /**
     * Exercise: create your own HTTP server running on localhost on 8388, which replies
     *  - with a welcome message on the "front door" localhost:8388
     *  - with a proper HTML on localhost:8388
     */
}
