package optimiser.sources

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.{EventEnvelope, Sequence}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit}
import drtlib.{Load, TQM}
import optimiser.actors.LoadUpdates
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import server.protobuf.messages.QueueLoad.{QueueLoadMessage, QueueLoadsMessage}

class LoadUpdatesSourceSpec
  extends TestKit(ActorSystem("LoadUpdates"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  implicit val materialiser: ActorMaterializer = ActorMaterializer()

  "A call to crunch some workload" must {
    "produce some desk recommendations" in {
      val minute = 0L
      val seqNr = 0L
      val terminal = "T1"
      val queue = "EEA"

      val loadsMessages = Seq(QueueLoadMessage(Option(terminal), Option(queue), Option(minute), Option(2), Option(3.5)))

      val eventSource = Source.single(EventEnvelope(Sequence(0L), "some-id", seqNr, QueueLoadsMessage(loadsMessages)))
      val source: Source[LoadUpdates, NotUsed] = LoadUpdatesSource(eventSource)

      val expected = LoadUpdates(List((TQM(terminal, queue, 0), Load(2.0, 3.5))), seqNr)

      source.runWith(TestSink.probe[LoadUpdates]).request(1).expectNext(expected)
    }
  }
}
