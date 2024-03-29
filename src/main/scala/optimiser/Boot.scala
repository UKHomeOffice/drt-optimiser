package optimiser

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Boot extends App with OptimiserRoutes {

  implicit val system: ActorSystem = ActorSystem("drt-optimiser")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val optimiserActor: ActorRef = system.actorOf(OptimiserActor.props, "optimiserActor")

  lazy val routes: Route = optimiserRoutes

  val config = ConfigFactory.load

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.getString("bind-address"), 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server started: http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
