package drtlib

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}

import scala.util.Try

object SDate {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val europeLondonId = "Europe/London"
  val europeLondonTimeZone: DateTimeZone = DateTimeZone.forID(europeLondonId)

  case class JodaSDate(dateTime: DateTime) extends SDateLike {

    def getDayOfWeek(): Int = dateTime.getDayOfWeek

    def getFullYear(): Int = dateTime.getYear

    def getMonth(): Int = dateTime.getMonthOfYear

    def getDate(): Int = dateTime.getDayOfMonth

    def getHours(): Int = dateTime.getHourOfDay

    def getMinutes(): Int = dateTime.getMinuteOfHour

    def getSeconds(): Int = dateTime.getSecondOfMinute

    def addDays(daysToAdd: Int): SDateLike = JodaSDate(dateTime.plusDays(daysToAdd))

    def addMonths(monthsToAdd: Int): SDateLike = JodaSDate(dateTime.plusMonths(monthsToAdd))

    def addHours(hoursToAdd: Int): SDateLike = JodaSDate(dateTime.plusHours(hoursToAdd))

    def addMinutes(mins: Int): SDateLike = JodaSDate(dateTime.plusMinutes(mins))

    def addMillis(millisToAdd: Int): SDateLike = JodaSDate(dateTime.plusMillis(millisToAdd))

    def millisSinceEpoch: Long = dateTime.getMillis

    def toISOString(): String = jodaSDateToIsoString(JodaSDate(dateTime))

    def getZone(): String = dateTime.getZone.getID

    def getTimeZoneOffsetMillis(): Long = dateTime.getZone.getOffset(millisSinceEpoch)

    def startOfTheMonth(): SDateLike = {
      val time = JodaSDate(dateTime)
      SDate(time.getFullYear(), time.getMonth(), 1, 0, 0, europeLondonTimeZone)
    }

    def getLocalPreviousMidnight: SDateLike = {
      val localNow = SDate(dateTime.getMillis, europeLondonTimeZone)
      val localMidnight = s"${localNow.toISODateOnly}T00:00"
      SDate(localMidnight, europeLondonTimeZone)
    }
  }

  def jodaSDateToIsoString(dateTime: SDateLike): String = {
    val fmt = ISODateTimeFormat.dateTimeNoMillis()
    val dt = dateTime.asInstanceOf[JodaSDate].dateTime
    fmt.print(dt)
  }

  def apply(dateTime: String): SDateLike = JodaSDate(new DateTime(dateTime, DateTimeZone.UTC))

  def apply(dateTime: DateTime): SDateLike = JodaSDate(dateTime)

  def apply(dateTime: String, timeZone: DateTimeZone): SDateLike = JodaSDate(new DateTime(dateTime, timeZone))

  def apply(dateTime: SDateLike, timeZone: DateTimeZone): SDateLike = JodaSDate(new DateTime(dateTime.millisSinceEpoch, timeZone))

  def apply(millis: Long): SDateLike = JodaSDate(new DateTime(millis, DateTimeZone.UTC))

  def apply(millis: Long, timeZone: DateTimeZone): SDateLike = JodaSDate(new DateTime(millis, timeZone))

  def now(): JodaSDate = JodaSDate(new DateTime(DateTimeZone.UTC))

  def now(dtz: DateTimeZone): JodaSDate = JodaSDate(new DateTime(dtz))

  def apply(y: Int, m: Int, d: Int, h: Int, mm: Int): SDateLike = jodaToSDate(new DateTime(y, m, d, h, mm, DateTimeZone.UTC))

  def apply(y: Int, m: Int, d: Int, h: Int, mm: Int, dateTimeZone: DateTimeZone): SDateLike = jodaToSDate(new DateTime(y, m, d, h, mm, dateTimeZone))

  def tryParseString(dateTime: String) = Try(apply(dateTime))

  def jodaToSDate(dateTime: DateTime): SDateLike = JodaSDate(dateTime)
}

trait SDateLike {

  def ddMMyyString: String = f"${getDate}%02d/${getMonth}%02d/${getFullYear - 2000}%02d"

  val months = List(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  )

  /**
    * Days of the week 1 to 7 (Monday is 1)
    *
    * @return
    */
  def getDayOfWeek(): Int

  def getFullYear(): Int

  def getMonth(): Int

  def getMonthString(): String = months(getMonth() - 1)

  def getDate(): Int

  def getHours(): Int

  def getMinutes(): Int

  def getSeconds(): Int

  def millisSinceEpoch: Long

  def toISOString(): String

  def addDays(daysToAdd: Int): SDateLike

  def addMonths(monthsToAdd: Int): SDateLike

  def addHours(hoursToAdd: Int): SDateLike

  def addMinutes(minutesToAdd: Int): SDateLike

  def addMillis(millisToAdd: Int): SDateLike

  def roundToMinute(): SDateLike = {
    val remainder = millisSinceEpoch % 60000
    addMillis(-1 * remainder.toInt)
  }

  def toLocalDateTimeString(): String = f"${getFullYear()}-${getMonth()}%02d-${getDate()}%02d ${getHours()}%02d:${getMinutes()}%02d"

  def toISODateOnly: String = f"${getFullYear()}-${getMonth()}%02d-${getDate()}%02d"

  def toHoursAndMinutes(): String = f"${getHours()}%02d:${getMinutes()}%02d"

  def prettyDateTime(): String = f"${getDate()}%02d-${getMonth()}%02d-${getFullYear()} ${getHours()}%02d:${getMinutes()}%02d"

  def prettyTime(): String = f"${getHours()}%02d:${getMinutes()}%02d"

  def hms(): String = f"${getHours()}%02d:${getMinutes()}%02d:${getSeconds()}%02d"

  def getZone(): String

  def getTimeZoneOffsetMillis(): Long

  def getLocalPreviousMidnight: SDateLike

  def startOfTheMonth(): SDateLike

  def getLastSunday: SDateLike =
    if (getDayOfWeek() == 7)
      this
    else
      addDays(-1 * getDayOfWeek())

  override def toString: String = f"${getFullYear()}-${getMonth()}%02d-${getDate()}%02dT${getHours()}%02d${getMinutes()}%02d"

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case d: SDateLike =>
        d.millisSinceEpoch == millisSinceEpoch
      case _ => false
    }
  }

  def compare(that: SDateLike): Int = millisSinceEpoch.compare(that.millisSinceEpoch)

  def <=(compareTo: SDateLike): Boolean = millisSinceEpoch <= compareTo.millisSinceEpoch
}
