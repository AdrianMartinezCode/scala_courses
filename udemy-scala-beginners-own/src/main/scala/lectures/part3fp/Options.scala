package lectures.part3fp

import scala.util.Random

object Options extends App {
    val myFirstOption : Option[Int] = Some(4)
    val noPtion: Option[Int] = None

    def unsafeMethod() : String = null
    val result = Option(unsafeMethod())
    println(result)

    // chained methods
    def backupMethod(): String = "A valid result"
    val chainedResult = Option(unsafeMethod()).orElse(Option(backupMethod()))

    // DESIGN unsafe APIs
    def betterUnsafeMethod(): Option[String] = None
    def betterBackupMethod(): Option[String] = Some("A valid result")

    val betterChainedResult = betterUnsafeMethod() orElse betterBackupMethod()

    // functions on Options
    println(myFirstOption.isEmpty)
    println(myFirstOption.get) // UNSAFE - DO NOT USE THIS

    // map, flatMap, filter
    println(myFirstOption.map(_ * 2))
    println(myFirstOption.filter(_ > 10))
    println(myFirstOption.flatMap(x => Option(x * 10)))

    // for-comprehensions

    /*
        Exercise:

    */
    val config: Map[String, String] = Map(
        // fetched from elsewhere
        "host" -> "176.45.36.1",
        "port" -> "80"
    )

    class Connection {
        def connect = "Connected" // connect to some server
    }
    object Connection {
        val random = new Random(System.nanoTime())
        def apply(host: String, port: String): Option[Connection] =
            if (random.nextBoolean()) Some(new Connection)
            else None


    }

    // try to establish connection, if so - print the connect method

    val host = config.get("host")
    val port = config.get("port")

    val connectionStatus = host.flatMap(host => port.flatMap(port => Connection(host, port))).map(_.connect)
    println(connectionStatus)
    connectionStatus.foreach(println)


    val connectionStatusTwo = for {
        host <- config.get("host")
        port <- config.get("port")
        connection <- Connection(host, port)
    } yield connection.connect
    connectionStatusTwo.foreach(println)
    

}
