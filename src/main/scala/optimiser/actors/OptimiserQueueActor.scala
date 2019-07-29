package optimiser.actors

import akka.actor.{ActorRef, Props}
import akka.persistence._
import drt.shared.TQM
import optimiser.{DayLoads, SDate}
import optimiser.actors.state.{Bookmark, QueueState}
import optimiser.sources.HeadOfQueue
import server.protobuf.messages.OptimiserQueue.{DaysToAddMessage, DaysToRemoveMessage, LoadMessage, LoadsMessage}


object OptimiserQueueActor {
  def props(dayOffsetMillis: Long, onReadySubscribers: Seq[ActorRef]) = Props(new OptimiserQueueActor(dayOffsetMillis, onReadySubscribers))
}

class OptimiserQueueActor(dayOffsetMillis: Long, onReadySubscribers: Seq[ActorRef]) extends AckingActor {

  def persistenceId: String = "optimiser-queue"

  var state: QueueState = QueueState.empty

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, DaysToAddMessage(loads, Some(seqNr))) =>
      log.info(s"Processing SnapshotOffer")
      state.optimiserQueue ++= loadMessagesToQueueLoads(loads)
      state.bookmark = seqNr

    case DaysToAddMessage(loads, Some(seqNr)) =>
      log.info(s"Processing recovery message containing ${loads.size} days")
      state.optimiserQueue ++= loadMessagesToQueueLoads(loads)
      state.bookmark = seqNr

    case DaysToRemoveMessage(days) =>
      state.optimiserQueue --= days

    case RecoveryCompleted =>
      log.info(s"Recovery completed. ${state.optimiserQueue.size} days queued. Bookmark at ${state.bookmark}. Informing subscribers")
      onReadySubscribers.foreach(subscriber => subscriber ! Bookmark(state.bookmark))

    case x => log.info(s"Received unexpected recovery message ${x.getClass}")
  }

  def loadMessagesToQueueLoads(loads: Seq[LoadsMessage]): Seq[(Long, Seq[(TQM, Double)])] = loads
    .collect {
      case LoadsMessage(Some(day), dayLoads) =>
        (day, dayLoads.map { qlm => (TQM(qlm.getTerminal, qlm.getQueue, qlm.getMinute), qlm.getWork) })
    }

  override def receiveCommand: Receive = super.receiveCommand orElse myReceiveCommand

  def myReceiveCommand: Receive = {
    case loadUpdatesByDay: LoadUpdatesByDay =>
      state.optimiserQueue ++= loadUpdatesByDay.updatesByDay
      state.bookmark = loadUpdatesByDay.lastSequenceNr
      log.info(s"Received LoadUpdates. Now ${state.optimiserQueue.size} days in the queue. Bookmark: ${state.bookmark}")

      val daysToAddMessage = dayLoadsToDaysToAddMessage(loadUpdatesByDay.updatesByDay, state.bookmark)

      persist(daysToAddMessage) { message =>
        log.info(s"Persisting ${message.serializedSize} bytes")
        if (lastSequenceNr % 2 == 0) {
          log.info(s"Snapshotting @ seqNr $lastSequenceNr")
          saveSnapshot(dayLoadsToDaysToAddMessage(state.optimiserQueue.toSeq, state.bookmark))

          log.info(s"Cleaning up old snapshots & messages")
          deleteMessages(lastSequenceNr - 1)
          deleteSnapshots(SnapshotSelectionCriteria(maxSequenceNr = lastSequenceNr - 1))
        }
      }

      sender() ! ack

    case HeadOfQueue if state.optimiserQueue.isEmpty =>
      log.debug(s"No days in optimiser queue")
      sender() ! None

    case HeadOfQueue =>
      val (dayMillis, loadsForDay) = state.optimiserQueue.head
      state.optimiserQueue -= dayMillis
      log.info(s"Sending ${SDate(dayMillis).toISOString()} to optimise. ${state.optimiserQueue.size} days in queue")

      persist(DaysToRemoveMessage(Seq(dayMillis))) { _ =>
        log.info(s"Persisted DaysToRemoveMessage($dayMillis)")
      }

      sender() ! Some(DayLoads(dayMillis, loadsForDay.toMap))

    case SaveSnapshotSuccess(md) =>
      log.info(s"Snapshot saved successfully. Seq no: ${md.sequenceNr}")

    case DeleteMessagesSuccess(toSeqNr) =>
      log.info(s"Successfully deleted old messages up to $toSeqNr")

    case DeleteSnapshotsSuccess(_) =>
      log.info(s"Successfully deleted old snapshots")

    case unexpected => log.warn(s"Received unexpected command message ${unexpected.getClass}")
  }

  def dayLoadsToDaysToAddMessage(loadUpdatesByDay: Seq[(Long, Seq[(TQM, Double)])], bookmark: Long): DaysToAddMessage = {
    val daysOfLoads = loadUpdatesByDay.map {
      case (day, loads) => LoadsMessage(Option(day), loads.map {
        case (TQM(t, q, m), load) => LoadMessage(Option(t), Option(q), Option(m), Option(load))
      })
    }

    DaysToAddMessage(daysOfLoads, Option(bookmark))
  }
}

case class LoadUpdatesByDay(updatesByDay: Seq[(Long, Seq[(TQM, Double)])], lastSequenceNr: Long)

case class LoadUpdates(updates: Seq[(TQM, Double)], lastSequenceNr: Long) {
  def byDay(offsetMillis: Long): LoadUpdatesByDay = {
    val loadsByDay = updates.groupBy { case (tqm, _) =>
      val dayStartMillis = SDate(tqm.minute).getLocalPreviousMidnight.millisSinceEpoch + offsetMillis
      dayStartMillis
    }.toSeq

    LoadUpdatesByDay(loadsByDay, lastSequenceNr)
  }
}
