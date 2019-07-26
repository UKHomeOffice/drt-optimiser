package optimiser.sources

import akka.NotUsed
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import drtlib.TQM
import optimiser.actors.{LoadUpdates, LoadUpdatesByDay}
import org.slf4j.{Logger, LoggerFactory}
import server.protobuf.messages.QueueLoad.QueueLoadsMessage

object LoadUpdatesSource {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(eventSource: Source[EventEnvelope, NotUsed], offsetMillis: Long): Source[LoadUpdatesByDay, NotUsed] = eventSource
    .map {
      case EventEnvelope(_, _, seqNr, QueueLoadsMessage(queueLoads)) =>
        log.info(s"Received ${queueLoads.length} queue loads!")
        val loadUpdates = queueLoads.map { msg => (TQM(msg.getTerminal, msg.getQueue, msg.getMinute), msg.getWork) }

        Option(LoadUpdates(loadUpdates, seqNr).byDay(offsetMillis))

      case EventEnvelope(_, _, _, unexpected) =>
        log.warn(s"Unexpected event: ${unexpected.getClass}")
        None
    }
    .collect { case Some(updates) => updates }
}
