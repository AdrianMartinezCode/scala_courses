package part2_lowlevelserver

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import part2_lowlevelserver.GuitarDB.{CreateGuitar, FindAllGuitars, FindGuitar, GuitarCreated}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

// step 1
import spray.json._

case class Guitar(make: String, model: String, inventory: Int)

object GuitarDB {
    case class CreateGuitar(guitar: Guitar)
    case class GuitarCreated(id: Int)
    case class FindGuitar(id: Int)
    case object FindAllGuitars
    case object FindAllGuitarsOnStock
}

class GuitarDB extends Actor with ActorLogging {
    import GuitarDB._

    var guitars: Map[Int, Guitar] = Map()
    var currentGuitarId: Int = 0

    override def receive: Receive = {
        case FindAllGuitars =>
            log.info("Searching for all guitars")
            sender() ! guitars.values.toList

        case FindAllGuitarsOnStock =>
            log.info("Searching for all guitars")

        case FindGuitar(id) =>
            log.info(s"Searching guitar by $id")
            sender() ! guitars.get(id)

        case CreateGuitar(guitar) =>
            log.info(s"Adding guitar $guitar with id $currentGuitarId")
            guitars = guitars + (currentGuitarId -> guitar)
            sender() ! GuitarCreated(currentGuitarId)
            currentGuitarId += 1
    }
}

// step 2
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
    // step 3
    implicit val guitarFormat = jsonFormat3(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {

    implicit val system = ActorSystem("LowLevelRest")
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    /*
        GET on /api/guitar => ALL the guitars in the store
        POST on /api/guitar => insert the guitar into the store
        GET on /api/guitar?id=X => fetches the guitar associated with id X
     */

    // JSON -> marshalling
    val simpleGuitar = Guitar("Fender", "Stratocaster", 1)
    println(simpleGuitar.toJson.prettyPrint)

    // unmarshalling
    val simpleGuitarJsonString =
        """
          |{
          |"make": "Fender",
          |"model": "Stratocaster"
          |}
          |""".stripMargin
    println(simpleGuitarJsonString.parseJson.convertTo[Guitar])
    /*
        setup
     */

    val guitarDb = system.actorOf(Props[GuitarDB], "LowLevelGuitarDB")
    val guitarList = List(
        Guitar("Fender", "Stratocaster", 0),
        Guitar("Gibson", "Les Paul", 1),
        Guitar("Martin", "LX1", 1)
    )

    guitarList.foreach { guitar =>
        guitarDb ! CreateGuitar(guitar)
    }

    /*
        server code
     */
    implicit val defaultTimeout = Timeout(2 seconds)

    def getGuitar(query: Query): Future[HttpResponse] = {

        val guitarId = query.get("id").map(_.toInt) // Option[Int]
        guitarId match {
            case None => Future(HttpResponse(StatusCodes.NotFound)) // /api/guitar?id=
            case Some(id) =>
                val guitarFuture = (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]
                guitarFuture.map {
                    case None => HttpResponse(StatusCodes.NotFound)
                    case Some(guitar) =>
                        HttpResponse(
                            entity = HttpEntity(
                                ContentTypes.`application/json`,
                                guitar.toJson.prettyPrint
                            )
                        )
                }
        }
    }


    val requestHandler: HttpRequest => Future[HttpResponse] = {
        case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
            /*
                query parameter handling code
             */
            val query = uri.query() // query object <=> Map[String, String]
            if (query.isEmpty) {
                val guitarsFuture: Future[List[Guitar]] = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
                guitarsFuture.map { guitars =>
                    HttpResponse(
                        entity = HttpEntity(
                            ContentTypes.`application/json`,
                            guitars.toJson.prettyPrint
                        )
                    )
                }
            } else {
                // fetch guitar associated to the guitar id
                // /api/guitar/?id=45
                getGuitar(query)
            }


        case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
            // entities are a Source[ByteString]
            val strictEntityFuture = entity.toStrict(3 seconds)
            strictEntityFuture.flatMap { strictEntity =>
                val guitarJsonString = strictEntity.data.utf8String
                val guitar = guitarJsonString.parseJson.convertTo[Guitar]

                val guitarCreatedFuture: Future[GuitarCreated] = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
                guitarCreatedFuture.map { _ =>
                    HttpResponse(StatusCodes.OK)
                }

            }

        case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
            val query = uri.query()
            if (query.nonEmpty) {

            } else {
                Future(HttpResponse(StatusCodes.NotFound))
            }


        case request: HttpRequest =>
            request.discardEntityBytes()
            Future {
                HttpResponse(status = StatusCodes.NotFound)
            }

    }

    Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

    /**
     * Exercise: enhance the Guitar case class ith a quantity field, by default 0
     * - GET to /api/guitar/inventory?inStock=true/false which returns the guitars in stock as a JSON
     * - POST to /api/guitar/inventory?id=X&quantity=Y which adds Y guitars to the stock for guitar with id X
     */
}
