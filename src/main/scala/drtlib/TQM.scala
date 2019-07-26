package drtlib

case class TQM(terminalName: String, queueName: String, minute: Long) extends Ordered[TQM] {
  override def equals(o: scala.Any): Boolean = o match {
    case TQM(t, q, m) => t == terminalName && q == queueName && m == minute
    case _ => false
  }

  lazy val comparisonString = s"$minute-$queueName-$terminalName"

  override def compare(that: TQM): Int = this.comparisonString.compareTo(that.comparisonString)
}
