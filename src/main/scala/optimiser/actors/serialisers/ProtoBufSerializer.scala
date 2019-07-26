package optimiser.actors.serialisers

import akka.serialization.SerializerWithStringManifest
import org.slf4j.{Logger, LoggerFactory}
import server.protobuf.messages.DesksAndWaits.{DesksAndWaitsMessage, DesksAndWaitsStateMessage, DesksAndWaitsUpdatesMessage}
import server.protobuf.messages.OptimiserQueue.{DaysToAddMessage, DaysToRemoveMessage}
import server.protobuf.messages.QueueLoad.{QueueLoadMessage, QueueLoadsMessage}

class ProtoBufSerializer extends SerializerWithStringManifest {
  override def identifier: Int = 9001

  override def manifest(targetObject: AnyRef): String = targetObject.getClass.getName

  final val DaysToAdd: String = classOf[DaysToAddMessage].getName
  final val DaysToRemove: String = classOf[DaysToRemoveMessage].getName
  final val DesksAndWaitsState: String = classOf[DesksAndWaitsStateMessage].getName
  final val DesksAndWaitsUpdates: String = classOf[DesksAndWaitsUpdatesMessage].getName
  final val DesksAndWaits: String = classOf[DesksAndWaitsMessage].getName
  final val QueueLoads: String = classOf[QueueLoadsMessage].getName
  final val QueueLoad: String = classOf[QueueLoadMessage].getName

  override def toBinary(objectToSerialize: AnyRef): Array[Byte] = {
    objectToSerialize match {
      case m: DaysToAddMessage => m.toByteArray
      case m: DaysToRemoveMessage => m.toByteArray
      case m: DesksAndWaitsStateMessage => m.toByteArray
      case m: DesksAndWaitsUpdatesMessage => m.toByteArray
      case m: DesksAndWaitsMessage => m.toByteArray
      case m: QueueLoadsMessage => m.toByteArray
      case m: QueueLoadMessage => m.toByteArray
    }
  }

  val log: Logger = LoggerFactory.getLogger(getClass)

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case DaysToAdd => DaysToAddMessage.parseFrom(bytes)
      case DaysToRemove => DaysToRemoveMessage.parseFrom(bytes)
      case DesksAndWaitsState => DesksAndWaitsStateMessage.parseFrom(bytes)
      case DesksAndWaitsUpdates => DesksAndWaitsUpdatesMessage.parseFrom(bytes)
      case DesksAndWaits => DesksAndWaitsMessage.parseFrom(bytes)
      case QueueLoads => QueueLoadsMessage.parseFrom(bytes)
      case QueueLoad => QueueLoadMessage.parseFrom(bytes)
    }
  }
}
