package optimiser.actors

import akka.persistence.PersistentActor
import org.slf4j.{Logger, LoggerFactory}

abstract class AckingActor extends PersistentActor {
  val log: Logger = LoggerFactory.getLogger(getClass)

  import AckingReceiver._

  val ack: AckingReceiver.Ack.type = Ack

  override def receiveCommand: Receive = {
    case StreamInitialized =>
      log.info("Stream initialized!")
      sender() ! ack

    case StreamCompleted =>
      log.info("Stream completed!")

    case StreamFailure(ex) =>
      log.error("Stream failed!", ex)
  }
}
