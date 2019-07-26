package optimiser.actors

import akka.actor.Props
import optimiser.DesksAndWaits
import optimiser.sources.DesksAndWaitsForQueue
import server.protobuf.messages.DesksAndWaits.{DesksAndWaitsMessage, DesksAndWaitsUpdatesMessage}

object DesksAndWaitsActor {
  def props() = Props(new DesksAndWaitsActor)
}

class DesksAndWaitsActor() extends AckingActor {
  override def persistenceId: String = "desks-and-waits"

  override def receiveRecover: Receive = {
    case DesksAndWaits(desks, waits) =>
  }

  override def receiveCommand: Receive = super.receiveCommand orElse myReceiveCommand

  def myReceiveCommand: Receive = {
    case DesksAndWaitsForQueue(t, q, DesksAndWaits(desks, waits)) =>
      val changedMinutes = desks.zip(waits).zipWithIndex.foldLeft(List[(Long, (Int, Int))]()) {
        case (updatesSoFar, ((d, w), m)) => (m.toLong, (d, w)) :: updatesSoFar
      }

      val msg = DesksAndWaitsUpdatesMessage(desksAndWaitsToMessages(t, q, changedMinutes))

      persist(msg) { minutes =>
        log.info(s"Persisting ${minutes.serializedSize} bytes")
      }

      sender() ! ack
  }

  def desksAndWaitsToMessages(terminal: String, queue: String, minutes: List[(Long, (Int, Int))]): List[DesksAndWaitsMessage] = minutes
    .map { case (m, (d, w)) =>
      DesksAndWaitsMessage(Option(terminal), Option(queue), Option(m), Option(d), Option(w))
    }
}
