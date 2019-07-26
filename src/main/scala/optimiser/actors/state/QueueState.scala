package optimiser.actors.state

import drtlib.TQM

import scala.collection.mutable

case class QueueState(var optimiserQueue: mutable.SortedMap[Long, Seq[(TQM, Double)]], var bookmark: Long)

object QueueState {
  val empty = QueueState(mutable.SortedMap[Long, Seq[(TQM, Double)]](), 0)
}

case class Bookmark(position: Long) {
  val nextPosition: Long = position + 1
}
