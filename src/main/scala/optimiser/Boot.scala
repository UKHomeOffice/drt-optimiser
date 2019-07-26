package optimiser

import akka.actor.{ActorSystem, Props}
import optimiser.actors.ControllerActor

import scala.concurrent.Await
import scala.concurrent.duration._

object Boot extends App {

  implicit val system: ActorSystem = ActorSystem("drt-optimiser")

  val controller = system.actorOf(Props(classOf[ControllerActor]))

  Await.result(system.whenTerminated, Duration.Inf)
}
