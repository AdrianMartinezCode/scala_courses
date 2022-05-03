package part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {

    class SimpleLoggingActor extends Actor with ActorLogging {
        override def receive: Receive = {
            case message => log.info(message.toString)
        }
    }

    /**
     * 1 - Inline configuration
     */
    val configString =
        """
          | akka {
          |     loglevel = "ERROR"
          | }
          |""".stripMargin

    val config = ConfigFactory.parseString(configString)
    val system = ActorSystem("ConfigurationDemo", ConfigFactory.load(config))
    val actor = system.actorOf(Props[SimpleLoggingActor])

    actor ! "A message to remember"

    /**
     * 2 - Config file
     */
    val defaultConfigFileSystem = ActorSystem("DefaultConfigFileDemo")
    val defaultConfigActor = defaultConfigFileSystem.actorOf(Props[SimpleLoggingActor])
    actor ! "Remember me"

    /**
     * 3 - separate config in the same file
     */
    val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig")
    val specialConfigSystem = ActorSystem("SpecialConfigDemo", specialConfig)
    val specialConfigActor = specialConfigSystem.actorOf(Props[SimpleLoggingActor])
    actor ! "Remember me, I am special"

    /**
     * 4 - separate config in another file
     */
    val separatedConfig = ConfigFactory.load("secretFolder")
    println(s"separated config: log level: ${separatedConfig.getString("akka.loglevel")}")

    /**
     * 5 - different file formats
     * JSON, Properties
     */
    val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
    println(s"json config: ${jsonConfig.getString("aJsonProperty")}")
    println(s"json config: ${jsonConfig.getString("akka.loglevel")}")

    val propsConfig = ConfigFactory.load("props/propsConfiguration.properties")
//    println(s"properties config: ${jsonConfig.getString("my.simpleProperty")}") --> doesn't works??
    println(s"properties config: ${jsonConfig.getString("akka.loglevel")}")
}
