package com.github.gchudnov.metrics.columns

import org.scalatest._

class ColumnSpec extends FlatSpec with Matchers {
  "All columns" should "have the expected size" in {
    val size = Columns.All.size
    size shouldBe 15
  }
}
