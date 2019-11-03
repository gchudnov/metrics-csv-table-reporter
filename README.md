# metrics-csv-table-reporter

<img src="docs/metrics-csv-table-reporter.png" width="120px" height="120px" align="right">

Capture metrics to a single csv table.

![](https://github.com/gchudnov/metrics-csv-table-reporter/workflows/Scala%20CI/badge.svg)

<br clear="right"> <!-- Turn off the wrapping for the logo image. -->

## Usage

Add the following dependency to your `build.sbt`:

```scala
libraryDependencies += "com.github.gchudnov" %% "metrics-csv-table-reporter" % "1.0.0"
```

In your code, import `com.github.gchudnov.metrics.CsvTableReporter`, construct and start the reporter:

```scala
import java.io.FileOutputStream
import com.codahale.metrics.MetricRegistry
import com.github.gchudnov.metrics.CsvTableReporter

val registry = new MetricRegistry
val outStream = new FileOutputStream("/some/path/report.csv", true)

val reporter = CsvTableReporter
  .forRegistry(registry)
  .outputTo(out)
  .build()

reporter.writeHeader()
reporter.start(1.second.toSeconds, TimeUnit.SECONDS)
```

add meters, histograms to the `registry`. When running, metrics output will be appended to the provided output stream.

## Quick Reference

```scala
CsvTableReporter
  .forRegistry(registry: MetricRegistry)                              // A registry to build a reporter for.
  .shutdownExecutorOnStop(shutdownExecutorOnStop: Boolean)            // Whether reporting executor stopped at the same time as reporter.
  .scheduleOn(executor: ScheduledExecutorService)                     // The executor to use while scheduling reporting of metrics.
  .outputTo(output: OutputStream = System.out)                        // Write to the given OutputStream.
  .withSeparator(separator: String = ";")                             // Delimiter to separate the values.
  .formattedFor(locale: Locale = Locale.getDefault())                 // Format numbers using the given Locale.
  .withClock(clock: Clock = Clock.defaultClock())                     // Clock to use to get the time.
  .formattedFor(timeZone: TimeZone = TimeZone.getDefault)             // Format time using the given TimeZone.
  .convertRatesTo(rateUnit: TimeUnit = TimeUnit.SECONDS)              // Convert rates to the given time unit.
  .convertDurationsTo(durationUnit: TimeUnit = TimeUnit.MILLISECONDS) // Convert durations to the given time unit.
  .filter(filter: MetricFilter = MetricFilter.ALL)                    // Report only metrics that match the given filter.
  .enabledColumns(Set[Column] = Columns.All)                          // Enable only specified columns in the output.
```

## Output Fields

The output contains the following columns:

- `name` - name of the metric.
- `kind` - type of the metric. one of the: `gauge`, `counter`, `histogram`, `meter`, `timer`.
- `ts` - timestamp.
- `value` - measurement of a value for a `gauge`.
- `count` - measurement of a value for a `counter`, `histogram`, `meter` or `timer`.
- `max` - max value.
- `mean` - average value.
- `min` - min value.
- `stddev` - standard deviation -- how measurements are spread out from the average (mean) value.
- `p50` - median.
- `p75` - 75th percentile.
- `p95` - 95th percentile.
- `p98` - 98th percentile.
- `p99` - 99th percentile.
- `p999` - 99.9th percentile.
- `m1_rate` - 1-minute rate, measured in `[1/rate_unit]`.
- `m5_rate` - 5-minutes rate, measured in `[1/rate_unit]`.
- `m15_rate` - 15-minutes rate, measured in `[1/rate_unit]`.
- `mean_rate` - mean rate, measured in `[1/rate_unit]`.
- `rate_unit` - time unit used to measure the rate, e.g. `second`.
- `duration_unit` - time unit used to measure the duration, e.g. `milliseconds`.

Rates `m1_rate`, `m5_rate`, `m15_rate`, `mean_rate` define the **throughput**; - how many units (events) where processed per `rate_unit`.

Metric-related columns

```text
| kind      | value | count | max | mean | min | stddev | p50 | p75 | p95 | p98 | p99 | p999 | m1_rate | m5_rate | m15_rate | mean_rate |
| --------- | ----- | ----- | --- | ---- | --- | ------ | --- | --- | --- | --- | --- | ---- | ------- | ------- | -------- | --------- |
| gauge     | x     |       |     |      |     |        |     |     |     |     |     |      |         |         |          |           |
| counter   |       | x     |     |      |     |        |     |     |     |     |     |      |         |         |          |           |
| histogram |       | x     | x   | x    | x   | x      | x   | x   | x   | x   | x   | x    |         |         |          |           |
| meter     |       | x     |     |      |     |        |     |     |     |     |     |      | r       | r       | r        | r         |
| timer     |       | x     | d   | d    | d   | d      | d   | d   | d   | d   | d   | d    | r       | r       | r        | r         |
```

where:

- `x` is a plain value.
- `d` represents a duration in *duration_units*.
- `r` represents a rate in *rate_units*.

Example

```text
name;kind;ts;value;count;max;mean;min;stddev;p50;p75;p95;p98;p99;p999;m1_rate;m5_rate;m15_rate;mean_rate;rate_unit;duration_unit
as.node.ignore.left.demand;counter;1572730123847;;1;;;;;;;;;;;;;;;second;milliseconds
as.node.ignore.left.downstream-finish;counter;1572730123847;;0;;;;;;;;;;;;;;;second;milliseconds
```

## Contact

[Grigorii Chudnov](mailto:g.chudnov@gmail.com)

## License

Distributed under the [The MIT License (MIT)](https://github.com/gchudnov/metrics-csv-table-reporter/blob/master/LICENSE).
