package optimiser

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import drt.shared.AirportConfigs
import optimiser.actors.ControllerActor

import scala.concurrent.Await
import scala.concurrent.duration._

object Boot extends App {

  implicit val system: ActorSystem = ActorSystem("drt-optimiser")

  val config = ConfigFactory.load

  val portCode = config.getString("portcode").toUpperCase
  val controller = system.actorOf(ControllerActor(AirportConfigs.confByPort(portCode)))

  Await.result(system.whenTerminated, Duration.Inf)
}
