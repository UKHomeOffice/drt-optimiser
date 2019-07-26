package optimiser.sources

import akka.NotUsed
import akka.actor.Cancellable
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Source
import akka.util.Timeout
import drtlib._
import optimiser.{DesksAndWaits, Optimiser, WorkloadToOptimise}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

case class DesksAndWaitsForQueue(terminal: String, queue: String, desksAndWaits: DesksAndWaits)

object DesksAndWaitsSource {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(optimiserQueueActor: AskableActorRef)(implicit ec: ExecutionContext): Source[DesksAndWaitsForQueue, Cancellable] = Source
    .tick(0 seconds, 5 seconds, NotUsed)
    .mapAsync(1) { _ => eventualMaybeHeadOfQueue(optimiserQueueActor) }
    .collect {
      case Some(DayLoads(dayMillis, loads)) if loads.nonEmpty => (dayMillis, loads)
    }
    .mapConcat { case (dayMillis, dayLoads) => groupByTerminalAndQueue(dayLoads).map(qLoads => (dayMillis, qLoads)) }
    .map { case (dayMillis, (t, q, loads)) =>
      val lastMinuteMilli = dayMillis + (1440 * 60 * 1000)
      val fullWl = (dayMillis until lastMinuteMilli by 60000).map(minute => loads.getOrElse(minute, 0d))
      val wlDescription = s"$t/$q/${SDate(dayMillis).toISOString()}"
      val wl = WorkloadToOptimise(fullWl, List.fill[Int](fullWl.size)(1), List.fill[Int](fullWl.size)(10), 25, wlDescription)
      val desksAndWaits = Optimiser.optimise(wl)
      log.debug(s"Got optimised desks")
      DesksAndWaitsForQueue(t, q, desksAndWaits)
    }

  def groupByTerminalAndQueue(allQueues: Map[TQM, Double]): immutable.Iterable[(String, String, Map[Long, Double])] = allQueues
    .groupBy { case (TQM(t, q, _), _) => (t, q) }
    .map { case ((t, q), tqmLoads) =>
      (t, q, tqmLoads.map { case (TQM(_, _, minute), work) => (minute, work) })
    }

  def eventualMaybeHeadOfQueue(actorToAsk: AskableActorRef)(implicit ec: ExecutionContext): Future[Option[DayLoads]] = actorToAsk
    .ask(HeadOfQueue)(Timeout(10 seconds))
    .map {
      case Some(DayLoads(day, loads)) => Option(DayLoads(day, loads))
      case None => None
    }
    .recoverWith { case t =>
      log.info(s"Didn't receive a reply", t.getMessage)
      Future(None)
    }
}

object HeadOfQueue
