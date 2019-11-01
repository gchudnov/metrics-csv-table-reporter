package com.github.gchudnov.metrics

import com.codahale.metrics.{Clock, Gauge, MetricFilter, MetricRegistry}
import com.github.gchudnov.metrics.columns._
import com.github.gchudnov.metrics.CsvTableReporter
import com.github.gchudnov.metrics.CsvTableReporter.Builder
import java.{util => ju}
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry.MetricSupplier
import org.scalatest._

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
      .formattedFor(ju.Locale.CANADA)
      .withClock(clock)
      .formattedFor(timeZone)
      .convertRatesTo(TimeUnit.HOURS)
      .convertDurationsTo(TimeUnit.MINUTES)
      .filter(MetricFilter.ALL)
      .enabledColumns(Set[Column](Mean))

    builder.registry shouldBe registry

    builder.output shouldBe System.out
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
    m shouldBe Map(Value -> "10")
  }
}
