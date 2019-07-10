package optimiser

import java.util.Date

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.pattern.ask
import akka.util.Timeout
import optimiser.OptimiserActor._

import scala.concurrent.duration._
import scala.language.postfixOps

trait OptimiserRoutes extends JsonSupport {

  implicit def system: ActorSystem

  lazy val log = Logging(system, classOf[OptimiserRoutes])

  def optimiserActor: ActorRef

  implicit lazy val timeout: Timeout = Timeout(120 seconds)

  lazy val optimiserRoutes: Route =
    pathPrefix("optimise") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[WorkloadToOptimise]) { workloadToOptimise =>
                val desksAndWaitsFuture = (optimiserActor ? OptimiseWorkload(workloadToOptimise)).mapTo[DesksAndWaits]

                onSuccess(desksAndWaitsFuture) { desksAndWaits =>
                  complete((StatusCodes.Created, desksAndWaits))
                }
              }
            }
          )
        }
      )
    }
}
