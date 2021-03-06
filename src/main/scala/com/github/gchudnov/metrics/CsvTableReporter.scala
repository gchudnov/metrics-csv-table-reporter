package com.github.gchudnov.metrics

import java.io.{OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.text.DateFormat
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}
import java.util.{Locale, TimeZone}
import java.{util => ju}

import com.codahale.metrics._
import com.github.gchudnov.metrics.columns._

import scala.jdk.CollectionConverters._

/**
  * A reporter which outputs measurements in the tabular format to an output stream.
  */
final class CsvTableReporter(
    registry: MetricRegistry,
    output: PrintWriter,
    separator: String,
    locale: Locale,
    clock: Clock,
    timeZone: TimeZone,
    rateUnit: TimeUnit,
    durationUnit: TimeUnit,
    filter: MetricFilter,
    executor: Option[ScheduledExecutorService],
    shutdownExecutorOnStop: Boolean,
    enabledColumns: Set[Column]
) extends ScheduledReporter(
      registry,
      "csv-table-reporter",
      filter,
      rateUnit,
      durationUnit,
      executor.orNull,
      shutdownExecutorOnStop,
      CsvTableReporter.calcDisabledMetricAttributes(enabledColumns).asJava
    ) {
  import CsvTableReporter._

  private val dateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale)
  dateFormat.setTimeZone(timeZone)

  def writeHeader(): Unit = {
    printHeader()
  }

  override def report(
      jgauges: ju.SortedMap[String, Gauge[_]],
      jcounters: ju.SortedMap[String, Counter],
      jhistograms: ju.SortedMap[String, Histogram],
      jmeters: ju.SortedMap[String, Meter],
      jtimers: ju.SortedMap[String, Timer]
  ): Unit = {
    val timestamp = clock.getTime

    val gauges = sortByName(jgauges).map { case (name, gauge)             => withCommonColumns(name, timestamp, gaugeValues(gauge)) }
    val counters = sortByName(jcounters).map { case (name, counter)       => withCommonColumns(name, timestamp, counterValues(counter)) }
    val histograms = sortByName(jhistograms).map { case (name, histogram) => withCommonColumns(name, timestamp, histogramValues(histogram)) }
    val meters = sortByName(jmeters).map { case (name, meter)             => withCommonColumns(name, timestamp, meterValues(meter)) }
    val timers = sortByName(jtimers).map { case (name, timer)             => withCommonColumns(name, timestamp, timerValues(timer)) }

    val all = gauges ++ counters ++ histograms ++ meters ++ timers
    val allWithoutDisabled = all.map(excludeDisabled)
    allWithoutDisabled.foreach(printValues)
  }

  private[metrics] def gaugeValues(gauge: Gauge[_]): Map[Column, String] = {
    Map(
      Kind -> "gauge",
      Value -> String.format(locale, "%s", asObject(gauge.getValue))
    )
  }

  private[metrics] def counterValues(counter: Counter): Map[Column, String] = {
    Map(
      Kind -> "counter",
      Count -> String.format(locale, "%d", asObject(counter.getCount))
    )
  }

  private[metrics] def histogramValues(histogram: Histogram): Map[Column, String] = {
    val snapshot: Snapshot = histogram.getSnapshot
    Map(
      Kind -> "histogram",
      Count -> String.format(locale, "%d", asObject(histogram.getCount)),
      Min -> String.format(locale, "%d", asObject(snapshot.getMin)),
      Max -> String.format(locale, "%d", asObject(snapshot.getMax)),
      Mean -> String.format(locale, "%2.2f", asObject(snapshot.getMean)),
      StdDev -> String.format(locale, "%2.2f", asObject(snapshot.getStdDev)),
      P50 -> String.format(locale, "%2.2f", asObject(snapshot.getMedian)), // median
      P75 -> String.format(locale, "%2.2f", asObject(snapshot.get75thPercentile())),
      P95 -> String.format(locale, "%2.2f", asObject(snapshot.get95thPercentile())),
      P98 -> String.format(locale, "%2.2f", asObject(snapshot.get98thPercentile())),
      P99 -> String.format(locale, "%2.2f", asObject(snapshot.get99thPercentile())),
      P999 -> String.format(locale, "%2.2f", asObject(snapshot.get999thPercentile()))
    )
  }

  private[metrics] def meterValues(meter: Meter): Map[Column, String] = {
    Map(
      Kind -> "meter",
      Count -> String.format(locale, "%d", asObject(meter.getCount)),
      MeanRate -> String
        .format(locale, "%2.2f", asObject(convertRate(meter.getMeanRate))),
      M1Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(meter.getOneMinuteRate))),
      M5Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(meter.getFiveMinuteRate))),
      M15Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(meter.getFifteenMinuteRate)))
    )
  }

  private[metrics] def timerValues(timer: Timer): Map[Column, String] = {
    val snapshot: Snapshot = timer.getSnapshot
    Map(
      Kind -> "timer",
      Count -> String.format(locale, "%d", asObject(timer.getCount)),
      MeanRate -> String
        .format(locale, "%2.2f", asObject(convertRate(timer.getMeanRate))),
      M1Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(timer.getOneMinuteRate))),
      M5Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(timer.getFiveMinuteRate))),
      M15Rate -> String
        .format(locale, "%2.2f", asObject(convertRate(timer.getFifteenMinuteRate))),
      Min -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.getMin.toDouble))),
      Max -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.getMax.toDouble))),
      Mean -> String.format(locale, "%2.2f", asObject(convertDuration(snapshot.getMean))),
      StdDev -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.getStdDev))),
      P50 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.getMedian))),
      P75 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.get75thPercentile()))),
      P95 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.get95thPercentile()))),
      P98 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.get98thPercentile()))),
      P99 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.get99thPercentile()))),
      P999 -> String
        .format(locale, "%2.2f", asObject(convertDuration(snapshot.get999thPercentile())))
    )
  }

  private[metrics] def withCommonColumns(
      name: String,
      timestamp: Long,
      m: Map[Column, String]
  ): Map[Column, String] = {
    m ++ Map(
      Name -> name,
      Timestamp -> String.format(locale, "%d", asObject(timestamp)),
      RateUnit -> String.format(locale, "%s", getRateUnit),
      DurationUnit -> String.format(locale, "%s", getDurationUnit)
    )
  }

  private[metrics] def excludeDisabled(vs: Map[Column, String]): Map[Column, String] = {
    vs.foldLeft(Map.empty[Column, String]) {
      case (acc, (k, v)) =>
        if (enabledColumns.contains(k)) {
          acc + (k -> v)
        } else {
          acc
        }
    }
  }

  private def printHeader(): Unit = {
    printLine(Columns.Ordered.map(_.code).mkString(separator))
    output.flush()
  }

  private def printValues(values: Map[Column, String]): Unit = {
    val line = Columns.Ordered
      .map(key => values.getOrElse(key, ""))
      .mkString(separator)
    printLine(line)
    output.flush()
  }

  private def printLine(line: String): Unit = {
    output.printf(locale, "%s%n", line)
  }
}

object CsvTableReporter {
  val DefaultSeparator = ";"

  final case class Builder(
      registry: MetricRegistry,
      output: OutputStream = System.out,
      separator: String = DefaultSeparator,
      locale: Locale = Locale.getDefault(),
      clock: Clock = Clock.defaultClock(),
      timeZone: TimeZone = TimeZone.getDefault,
      rateUnit: TimeUnit = TimeUnit.SECONDS,
      durationUnit: TimeUnit = TimeUnit.MILLISECONDS,
      filter: MetricFilter = MetricFilter.ALL,
      executor: Option[ScheduledExecutorService] = None,
      shutdownExecutorOnStop: Boolean = true,
      enabledColumns: Set[Column] = Columns.All
  ) {
    /**
      * Whether reporting executor stopped at the same time as reporter.
      */
    def shutdownExecutorOnStop(shutdownExecutorOnStop: Boolean): Builder =
      this.copy(shutdownExecutorOnStop = shutdownExecutorOnStop)

    /**
      * The executor to use while scheduling reporting of metrics.
      */
    def scheduleOn(executor: ScheduledExecutorService): Builder =
      this.copy(executor = Some(executor))

    /**
      * Write to the given OutputStream.
      */
    def outputTo(output: OutputStream): Builder =
      this.copy(output = output)

    /**
      * Format numbers using the given Locale.
      */
    def formattedFor(locale: Locale): Builder =
      this.copy(locale = locale)

    /**
      * Delimiter to separate the values.
      */
    def withSeparator(separator: String): Builder =
      this.copy(separator = separator)

    /**
      * Clock to use to get the time.
      */
    def withClock(clock: Clock): Builder =
      this.copy(clock = clock)

    /**
      * Format time using the given TimeZone.
      */
    def formattedFor(timeZone: TimeZone): Builder =
      this.copy(timeZone = timeZone)

    /**
      * Convert rates to the given time unit.
      */
    def convertRatesTo(rateUnit: TimeUnit): Builder =
      this.copy(rateUnit = rateUnit)

    /**
      * Convert durations to the given time unit.
      */
    def convertDurationsTo(durationUnit: TimeUnit): Builder =
      this.copy(durationUnit = durationUnit)

    /**
      * Report only metrics that match the given filter.
      */
    def filter(filter: MetricFilter): Builder =
      this.copy(filter = filter)

    /**
      * Enable only specified columns in the output.
      */
    def enabledColumns(columns: Set[Column]): Builder =
      this.copy(enabledColumns = columns)

    /**
      * Builds a CsvTableReporter with the given properties.
      */
    def build(): CsvTableReporter =
      new CsvTableReporter(
        registry,
        new PrintWriter(new OutputStreamWriter(output, UTF_8)),
        separator,
        locale,
        clock,
        timeZone,
        rateUnit,
        durationUnit,
        filter,
        executor,
        shutdownExecutorOnStop,
        enabledColumns
      )
  }

  def forRegistry(registry: MetricRegistry): Builder =
    Builder(registry)

  private[metrics] def calcDisabledMetricAttributes(
      enabledColumns: Set[Column]
  ): Set[MetricAttribute] = {
    (Columns.All -- enabledColumns).collect {
      case column if Columns.ColumnAttributeMap.contains(column) =>
        Columns.ColumnAttributeMap(column)
    }
  }

  private[metrics] def sortByName[T](m: ju.SortedMap[String, T]): Seq[(String, T)] =
    m.asScala.toSeq.sortBy(_._1)

  private def asObject[T](value: T): java.lang.Object = {
    value.asInstanceOf[java.lang.Object]
  }
}
