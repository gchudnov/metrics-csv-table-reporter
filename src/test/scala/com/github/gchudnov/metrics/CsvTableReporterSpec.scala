package com.github.gchudnov.metrics

import java.io.{File, FileOutputStream}
import java.time.Duration
import java.util.concurrent.{Executors, TimeUnit}
import java.{util => ju}

import com.codahale.metrics.{Clock, Gauge, MetricFilter, MetricRegistry}
import com.github.gchudnov.metrics.columns._
import org.scalatest._

import scala.io.Source
import scala.util.Using

class CsvTableReporterSpec extends FlatSpec with Matchers {
  "CsvTableReporter" should "get all disabled attributes if no columns are enabled" in {
    val enabled = Set.empty[Column]
    val disabled = CsvTableReporter.calcDisabledMetricAttributes(enabled)
    disabled.size shouldBe 15
  }

  it should "can be used to build an instance of CsvTableReporter" in {
    val registry = new MetricRegistry
    val ses = Executors.newScheduledThreadPool(1)
    val clock = new Clock.UserTimeClock
    val timeZone = ju.TimeZone.getTimeZone("PST")

    val builder = CsvTableReporter
      .forRegistry(registry)
      .shutdownExecutorOnStop(false)
      .scheduleOn(ses)
      .outputTo(System.out)
      .withSeparator(":")
      .formattedFor(ju.Locale.CANADA)
      .withClock(clock)
      .formattedFor(timeZone)
      .convertRatesTo(TimeUnit.HOURS)
      .convertDurationsTo(TimeUnit.MINUTES)
      .filter(MetricFilter.ALL)
      .enabledColumns(Set[Column](Mean))

    builder.registry shouldBe registry

    builder.output shouldBe System.out
    builder.separator shouldBe ":"
    builder.locale shouldBe ju.Locale.CANADA
    builder.clock shouldBe clock
    builder.timeZone shouldBe timeZone
    builder.rateUnit shouldBe TimeUnit.HOURS
    builder.durationUnit shouldBe TimeUnit.MINUTES
    builder.executor shouldBe Some(ses)
    builder.shutdownExecutorOnStop shouldBe false
    builder.filter shouldBe MetricFilter.ALL
    builder.enabledColumns shouldBe Set[Column](Mean)
  }

  it should "exclude the disabled columns" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .enabledColumns(Set[Column](Max))
      .build()

    val m = reporter.excludeDisabled(Map(Max -> "10", Mean -> "12", Timestamp -> "123456"))
    m shouldBe Map(Max -> "10")
  }

  it should "exclude the disabled columns if all columns are disabled" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .enabledColumns(Set.empty[Column])
      .build()

    val m = reporter.excludeDisabled(Map(Max -> "10", Mean -> "12", Timestamp -> "123456"))
    m shouldBe Map.empty[Column, String]
  }

  it should "allow to write header to a file" in {
    val registry = new MetricRegistry

    val file = File.createTempFile("csv-table-report", null)
    file.length() shouldBe 0

    Using.resource(new FileOutputStream(file, true))(out => {
      val reporter = CsvTableReporter
        .forRegistry(registry)
        .outputTo(out)
        .build()

      reporter.writeHeader()

      Using.resource(Source.fromFile(file))(source => {
        val header = source.getLines.mkString
        header shouldBe "name;kind;ts;value;count;max;mean;min;stddev;p50;p75;p95;p98;p99;p999;m1_rate;m5_rate;m15_rate;mean_rate;rate_unit;duration_unit"
      })

      file.length() should not be 0
    })
  }

  it should "write metrics to a file" in {
    val registry = new MetricRegistry

    val file = File.createTempFile("csv-table-report", null)
    file.length() shouldBe 0

    Using.resource(new FileOutputStream(file, true))(out => {
      val reporter = CsvTableReporter
        .forRegistry(registry)
        .outputTo(out)
        .build()

      val counter = registry.counter("counter-a")
      counter.inc()

      reporter.writeHeader()
      reporter.report()

      Using.resource(Source.fromFile(file))(source => {
        val lineCount = source.getLines.size
        lineCount shouldBe 2
      })

      file.length() should not be 0
    })
  }

  "sortByName" should "sort items by name" in {
    val xs = new ju.TreeMap[String, Int]
    xs.put("aa", 1)
    xs.put("cc", 3)
    xs.put("bb", 2)

    val ys = CsvTableReporter.sortByName(xs)
    ys shouldBe Seq[(String, Int)](("aa", 1), ("bb", 2), ("cc", 3))
  }

  "withCommonColumns" should "add common columns to the map" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val m = reporter.withCommonColumns("my-name", 123L, Map(Mean -> "123"))
    m shouldBe Map(Name -> "my-name", RateUnit -> "second", DurationUnit -> "milliseconds", Mean -> "123", Timestamp -> "123")
  }

  "gaugeValues" should "return the expected values" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val g = new Gauge[Int] {
      override def getValue: Int = 10
    }

    registry.gauge("g", () => g)

    val m = reporter.gaugeValues(g)
    m shouldBe Map(Kind -> "gauge", Value -> "10")
  }

  "counterValues" should "return the expected values" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val counter = registry.counter("counterName")
    counter.inc(1)

    val m = reporter.counterValues(counter)
    m shouldBe Map(Kind -> "counter", Count -> "1")
  }

  "histogramValues" should "return the expected values" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val histogram = registry.histogram("histogramName")
    histogram.update(1)

    val m = reporter.histogramValues(histogram)
    m shouldBe Map(
      Kind -> "histogram",
      Mean -> "1.00",
      P75 -> "1.00",
      Max -> "1",
      P999 -> "1.00",
      Count -> "1",
      StdDev -> "0.00",
      P99 -> "1.00",
      P95 -> "1.00",
      Min -> "1",
      P50 -> "1.00",
      P98 -> "1.00"
    )
  }

  "meterValues" should "return the expected keys" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val meter = registry.meter("meterName")
    meter.mark()

    val m = reporter.meterValues(meter)
    m.keySet shouldBe Set(Kind, MeanRate, M1Rate, Count, M15Rate, M5Rate)
    m.get(Count) shouldBe Some("1")
  }

  "timerValues" should "return the expected keys" in {
    val registry = new MetricRegistry
    val reporter = CsvTableReporter
      .forRegistry(registry)
      .build()

    val timer = registry.timer("timerName")
    timer.update(Duration.ofMillis(10))

    val m = reporter.timerValues(timer)
    m.keySet shouldBe Set(Kind, MeanRate, Mean, P75, Max, P999, M1Rate, Count, M15Rate, P99, P95, Min, P50, P98, StdDev, M5Rate)
  }
}
