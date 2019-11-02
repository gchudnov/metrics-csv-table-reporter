package com.github.gchudnov.metrics.columns

import com.codahale.metrics.MetricAttribute

sealed trait Column {
  def code: String
}

case object Name extends Column {
  override val code: String = "name"
}

case object Kind extends Column {
  override val code: String = "kind"
}

case object Timestamp extends Column {
  override val code: String = "ts"
}

case object Value extends Column {
  override val code: String = "value"
}

case object RateUnit extends Column {
  override val code: String = "rate_unit"
}

case object DurationUnit extends Column {
  override val code: String = "duration_unit"
}

case object Max extends Column {
  override val code: String = MetricAttribute.MAX.getCode
}

case object Mean extends Column {
  override val code: String = MetricAttribute.MEAN.getCode
}

case object Min extends Column {
  override val code: String = MetricAttribute.MIN.getCode
}

case object StdDev extends Column {
  override val code: String = MetricAttribute.STDDEV.getCode
}

case object P50 extends Column {
  override val code: String = MetricAttribute.P50.getCode
}

case object P75 extends Column {
  override val code: String = MetricAttribute.P75.getCode
}

case object P95 extends Column {
  override val code: String = MetricAttribute.P95.getCode
}

case object P98 extends Column {
  override val code: String = MetricAttribute.P98.getCode
}

case object P99 extends Column {
  override val code: String = MetricAttribute.P99.getCode
}

case object P999 extends Column {
  override val code: String = MetricAttribute.P999.getCode
}

case object Count extends Column {
  override val code: String = MetricAttribute.COUNT.getCode
}

case object M1Rate extends Column {
  override val code: String = MetricAttribute.M1_RATE.getCode
}

case object M5Rate extends Column {
  override val code: String = MetricAttribute.M5_RATE.getCode
}

case object M15Rate extends Column {
  override val code: String = MetricAttribute.M15_RATE.getCode
}

case object MeanRate extends Column {
  override val code: String = MetricAttribute.MEAN_RATE.getCode
}

object Columns {
  val Ordered: Seq[Column] = Seq(
    Name,
    Kind,
    Timestamp,
    Value,
    Count,
    Max,
    Mean,
    Min,
    StdDev,
    P50,
    P75,
    P95,
    P98,
    P99,
    P999,
    M1Rate,
    M5Rate,
    M15Rate,
    MeanRate,
    RateUnit,
    DurationUnit
  )

  val ColumnAttributeMap: Map[Column, MetricAttribute] = Map(
    Max -> MetricAttribute.MAX,
    Mean -> MetricAttribute.MEAN,
    Min -> MetricAttribute.MIN,
    StdDev -> MetricAttribute.STDDEV,
    P50 -> MetricAttribute.P50,
    P75 -> MetricAttribute.P75,
    P95 -> MetricAttribute.P95,
    P98 -> MetricAttribute.P98,
    P99 -> MetricAttribute.P99,
    P999 -> MetricAttribute.P999,
    Count -> MetricAttribute.COUNT,
    M1Rate -> MetricAttribute.M1_RATE,
    M5Rate -> MetricAttribute.M5_RATE,
    M15Rate -> MetricAttribute.M15_RATE,
    MeanRate -> MetricAttribute.MEAN_RATE
  )

  val All: Set[Column] = Ordered.toSet
}
