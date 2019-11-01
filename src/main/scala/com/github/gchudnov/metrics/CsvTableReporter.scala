package com.github.gchudnov.metrics

import java.io.PrintStream
import java.text.DateFormat
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}
import java.util.{Date, Locale, TimeZone}
import java.{util => ju}
import com.github.gchudnov.metrics.columns._

import com.codahale.metrics._

import scala.jdk.CollectionConverters._

// https://github.com/dropwizard/metrics/blob/3748f09b249f47a24ef868595fed4556ec5e92b1/metrics-core/src/main/java/com/codahale/metrics/CsvReporter.java

/**
  * A reporter which outputs measurements in the tabular format to a PrintStream, like System.out.
  */
class CsvTableReporter(
    registry: MetricRegistry,
    output: PrintStream,
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
  private val Separator = ";"

  import CsvTableReporter._

  private val dateFormat =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale)
  dateFormat.setTimeZone(timeZone)

  override def report(
      jgauges: ju.SortedMap[String, Gauge[_]],
      jcounters: ju.SortedMap[String, Counter],
      jhistograms: ju.SortedMap[String, Histogram],
      jmeters: ju.SortedMap[String, Meter],
      jtimers: ju.SortedMap[String, Timer]
  ): Unit = {
    val timestamp = clock.getTime

    val gauges = sortByName(jgauges).map { case (name, gauge) => withCommonColumns(name, timestamp, gaugeValues(gauge)) }
    val counters = sortByName(jcounters).map { case (name, counter) => withCommonColumns(name, timestamp, counterValues(counter)) }
    val histograms = sortByName(jhistograms).map { case (name, histogram) => withCommonColumns(name, timestamp, histogramValues(histogram)) }
    val meters = sortByName(jmeters).map { case (name, meter) => withCommonColumns(name, timestamp, meterValues(meter)) }
    val timers = sortByName(jtimers).map { case (name, timer) => withCommonColumns(name, timestamp, timerValues(timer)) }

    val all = gauges ++ counters ++ histograms ++ meters ++ timers
    val allWithoutDisabled = all.map(excludeDisabled)
    allWithoutDisabled.foreach(printValues)
  }

  private[metrics] def gaugeValues(gauge: Gauge[_]): Map[Column, String] = {
    Map(
      Value -> String.format(locale, "%s", gauge.getValue)
    )
  }

  private[metrics] def counterValues(counter: Counter): Map[Column, String] = {
    Map(
      Count -> String.format(locale, "%d", counter.getCount)
    )
  }

  private def histogramValues(histogram: Histogram): Map[Column, String] = {
    val snapshot: Snapshot = histogram.getSnapshot
    Map(
      Count -> String.format(locale, "%d", histogram.getCount),
      Min -> String.format(locale, "%d", snapshot.getMin),
      Max -> String.format(locale, "%d", snapshot.getMax),
      Mean -> String.format(locale, "%2.2f", snapshot.getMean),
      StdDev -> String.format(locale, "%2.2f", snapshot.getStdDev),
      P50 -> String.format(locale, "%2.2f", snapshot.getMedian), // median
      P75 -> String.format(locale, "%2.2f", snapshot.get75thPercentile()),
      P95 -> String.format(locale, "%2.2f", snapshot.get95thPercentile()),
      P98 -> String.format(locale, "%2.2f", snapshot.get98thPercentile()),
      P99 -> String.format(locale, "%2.2f", snapshot.get99thPercentile()),
      P999 -> String.format(locale, "%2.2f", snapshot.get999thPercentile())
    )
  }

  private def meterValues(meter: Meter): Map[Column, String] = {
    Map(
      Count -> String.format(locale, "%d", meter.getCount),
      MeanRate -> String
        .format(locale, "%2.2f", convertRate(meter.getMeanRate)),
      M1Rate -> String
        .format(locale, "%2.2f", convertRate(meter.getOneMinuteRate)),
      M5Rate -> String
        .format(locale, "%2.2f", convertRate(meter.getFiveMinuteRate)),
      M15Rate -> String
        .format(locale, "%2.2f", convertRate(meter.getFifteenMinuteRate))
    )
  }

  private def timerValues(timer: Timer): Map[Column, String] = {
    val snapshot: Snapshot = timer.getSnapshot
    Map(
      Count -> String.format(locale, "%d", timer.getCount),
      MeanRate -> String
        .format(locale, "%2.2f", convertRate(timer.getMeanRate)),
      M1Rate -> String
        .format(locale, "%2.2f", convertRate(timer.getOneMinuteRate)),
      M5Rate -> String
        .format(locale, "%2.2f", convertRate(timer.getFiveMinuteRate)),
      M15Rate -> String
        .format(locale, "%2.2f", convertRate(timer.getFifteenMinuteRate)),
      Min -> String
        .format(locale, "%2.2f", convertDuration(snapshot.getMin.toDouble)),
      Max -> String
        .format(locale, "%2.2f", convertDuration(snapshot.getMax.toDouble)),
      Mean -> String.format(locale, "%2.2f", convertDuration(snapshot.getMean)),
      StdDev -> String
        .format(locale, "%2.2f", convertDuration(snapshot.getStdDev)),
      P50 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.getMedian)),
      P75 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.get75thPercentile())),
      P95 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.get95thPercentile())),
      P98 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.get98thPercentile())),
      P99 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.get99thPercentile())),
      P999 -> String
        .format(locale, "%2.2f", convertDuration(snapshot.get999thPercentile()))
    )
  }

  private[metrics] def withCommonColumns(
      name: String,
      timestamp: Long,
      m: Map[Column, String]
  ): Map[Column, String] = {
    m ++ Map(
      Name -> name,
      Timestamp -> String.format(locale, "%d", timestamp),
      RateUnit -> String.format(locale, "%s", getRateUnit),
      DurationUnit -> String.format(locale, "%s", getDurationUnit)
    )
  }

  private def printValues(values: Map[Column, String]): Unit = {
    val line = Columns.Ordered
      .map(key => values.getOrElse(key, ""))
      .mkString(Separator)
    printLine(line)
  }

  private def printHeader(): Unit = {
    printLine(Columns.Ordered.mkString(Separator))
  }

  private def printLine(line: String): Unit = {
    output.printf(locale, "%s%n", line)
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
}

object CsvTableReporter {
  final case class Builder(
      registry: MetricRegistry,
      output: PrintStream = System.out,
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
      * Specifies whether or not, the executor (used for reporting) will be stopped with same time with reporter.
      */
    def shutdownExecutorOnStop(shutdownExecutorOnStop: Boolean): Builder =
      this.copy(shutdownExecutorOnStop = shutdownExecutorOnStop)

    /**
      * Specifies the executor to use while scheduling reporting of metrics.
      */
    def scheduleOn(executor: ScheduledExecutorService): Builder =
      this.copy(executor = Some(executor))

    /**
      * Write to the given PrintStream.
      */
    def outputTo(output: PrintStream): Builder =
      this.copy(output = output)

    /**
      * Format numbers for the given Locale.
      */
    def formattedFor(locale: Locale): Builder =
      this.copy(locale = locale)

    /**
      * Use the given Clock instance for the time.
      */
    def withClock(clock: Clock): Builder =
      this.copy(clock = clock)

    /**
      * Use the given TimeZone for the time.
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
      * Only report metrics which match the given filter.
      */
    def filter(filter: MetricFilter): Builder =
      this.copy(filter = filter)

    /**
      * Enable only the specified columns in the output.
      */
    def enabledColumns(columns: Set[Column]): Builder =
      this.copy(enabledColumns = columns)

    /**
      * Builds a CsvTableReporter with the given properties.
      */
    def build(): CsvTableReporter =
      new CsvTableReporter(
        registry,
        output,
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
}
