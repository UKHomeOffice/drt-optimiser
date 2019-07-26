package drtlib

import scala.collection.immutable.SortedMap


case class Loads(loadMinutes: SortedMap[TQM, Load])

case class DayLoads(day: Long, loadMinutes: Map[TQM, Double])

case class Load(pax: Double, work: Double)
