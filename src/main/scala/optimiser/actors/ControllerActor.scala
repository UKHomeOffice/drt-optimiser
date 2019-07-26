package optimiser.actors

import akka.NotUsed
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.AskableActorRef
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import optimiser.actors.state.Bookmark
import optimiser.sources.{DesksAndWaitsSource, LoadUpdatesSource}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext

class ControllerActor extends Actor {
  val persistenceId = "queue-load"

  val log: Logger = LoggerFactory.getLogger(getClass)

  val offsetMillis: Long = 120 * 60000
  val optimiserQueueActorProps: Props = OptimiserQueueActor.props(offsetMillis, Seq(self))
  val optimiserQueueActor: AskableActorRef = context.system.actorOf(optimiserQueueActorProps, "optimiserQueueActor")

  val desksAndWaitsActor: ActorRef = context.system.actorOf(DesksAndWaitsActor.props(), "desksAndWaitsActor")
  val desksAndWaitsSink = AckingReceiver(desksAndWaitsActor)

  val queueSink = AckingReceiver(optimiserQueueActor.actorRef)

  val queries: JdbcReadJournal = PersistenceQuery(context.system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

  override def receive: Receive = {
    case bookmark: Bookmark =>
      log.info(s"Starting up the queue system. Picking up events from sequence number ${bookmark.nextPosition}")

      val eventSource: Source[EventEnvelope, NotUsed] = queries.eventsByPersistenceId(persistenceId, bookmark.nextPosition, Long.MaxValue)
      implicit val materializer: ActorMaterializer = ActorMaterializer()
      implicit val executionContext: ExecutionContext = context.system.dispatcher

      LoadUpdatesSource(eventSource, offsetMillis).runWith(queueSink)
      DesksAndWaitsSource(optimiserQueueActor).runWith(desksAndWaitsSink)
  }
}
