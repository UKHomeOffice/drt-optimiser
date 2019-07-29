package optimiser.sources

import akka.NotUsed
import akka.actor.Cancellable
import akka.pattern.AskableActorRef
import akka.stream.scaladsl.Source
import akka.util.Timeout
import drt.shared.FlightsApi.{QueueName, TerminalName}
import drt.shared.{Queues, TQM}
import optimiser._
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.immutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Try

case class DesksAndWaitsForQueue(terminal: String, queue: String, desksAndWaits: DesksAndWaits)

object DesksAndWaitsSource {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val europeLondonId = "Europe/London"
  val europeLondonTimeZone: DateTimeZone = DateTimeZone.forID(europeLondonId)

  def apply(optimiserQueueActor: AskableActorRef,
            desksByQueue: Map[TerminalName, Map[QueueName, (List[Int], List[Int])]],
            slas: Map[QueueName, Int],
            egateBankSize: Int)(implicit ec: ExecutionContext): Source[DesksAndWaitsForQueue, Cancellable] = Source
    .tick(0 seconds, 5 seconds, NotUsed)
    .mapAsync(1) { _ => eventualMaybeHeadOfQueue(optimiserQueueActor) }
    .collect {
      case Some(DayLoads(dayMillis, loads)) if loads.nonEmpty => (dayMillis, loads)
    }
    .mapConcat { case (dayMillis, dayLoads) => groupByTerminalAndQueue(dayLoads).map(qLoads => (dayMillis, qLoads)) }
    .map { case (dayMillis, (t, q, loads)) =>
      val lastMinuteMilli = dayMillis + (1440 * 60 * 1000)
      val workload = (dayMillis until lastMinuteMilli by 60000).map(minute => loads.getOrElse(minute, 0d))
      val adjustedWorkload = if (q == Queues.EGate) workload.map(_ / egateBankSize) else workload
      val wlDescription = s"$t/$q/${SDate(dayMillis).toISOString()}"

      val (minDesksByStartHour, maxDesksByStartHour) = minMaxDesksByMinute(desksByQueue, dayMillis, t, q, adjustedWorkload)
      val wl = WorkloadToOptimise(adjustedWorkload, minDesksByStartHour, maxDesksByStartHour, slas.getOrElse(q, 25), wlDescription)

      val desksAndWaits = Optimiser.optimise(wl)

      DesksAndWaitsForQueue(t, q, desksAndWaits)
    }

  def minMaxDesksByMinute(desksByQueue: Map[TerminalName, Map[QueueName, (List[Int], List[Int])]],
                          dayMillis: Long, t: String, q: String,
                          adjustedWorkload: immutable.IndexedSeq[Double]): (Seq[Int], Seq[Int]) = {
    val (minDesks, maxDesks) = desksByQueue.getOrElse(t, Map()).getOrElse(q, defaultMinMaxDesks(adjustedWorkload))

    val startingHour = new DateTime(dayMillis).withZone(europeLondonTimeZone).getHourOfDay
    val minDesksByStartHour = desksByMinute(desksByStartHour(minDesks, startingHour))
    val maxDesksByStartHour = desksByMinute(desksByStartHour(maxDesks, startingHour))
    (minDesksByStartHour, maxDesksByStartHour)
  }

  def defaultMinMaxDesks(fullWl: immutable.IndexedSeq[Double]): (List[Int], List[Int]) = {
    val defaultMinMax = (List.fill[Int](fullWl.size)(1), List.fill[Int](fullWl.size)(10))
    defaultMinMax
  }

  def desksByStartHour(byHour: Seq[Int], startHour: Int): Seq[Int] =
    (0 until 24).map(h => {
      val index = (h + startHour) % 24
      Try(byHour(index)).getOrElse(1)
    })

  def desksByMinute(byHour: Seq[Int]): Seq[Int] = byHour.flatMap(List.fill[Int](60)(_))

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
