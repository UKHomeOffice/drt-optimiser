package optimiser.actors

import akka.NotUsed
import akka.actor.ActorRef
import akka.stream.scaladsl.Sink

object AckingReceiver {

  case object Ack

  case object StreamInitialized

  case object StreamCompleted

  final case class StreamFailure(ex: Throwable)

  def apply[A](actorRef: ActorRef): Sink[A, NotUsed] = {
    val AckMessage = AckingReceiver.Ack
    val InitMessage = AckingReceiver.StreamInitialized
    val OnCompleteMessage = AckingReceiver.StreamCompleted
    val onErrorMessage = (ex: Throwable) => AckingReceiver.StreamFailure(ex)

    Sink.actorRefWithAck(actorRef, InitMessage, AckMessage, OnCompleteMessage, onErrorMessage)
  }
}
