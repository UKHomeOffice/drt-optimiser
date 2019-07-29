package optimiser

import drt.shared.TQM

case class DayLoads(day: Long, loadMinutes: Map[TQM, Double])
