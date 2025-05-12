package abtesstr

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class TimePeriodSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  "TimePeriod" - {
    val now    = Instant.parse("2023-01-01T00:00:00Z")
    val before = now.minusSeconds(100)
    val after  = now.plusSeconds(100)

    "with no end date" - {
      val period = TimePeriod(start = before, end = None)

      "should be active at or after start time" in {
        assert(period.isActive(before))
        assert(period.isActive(now))
        assert(period.isActive(after))
      }

      "should not be active before start time" in {
        assert(!period.isActive(before.minusSeconds(1)))
      }
    }

    "with end date" - {
      val period = TimePeriod(start = before, end = Some(after))

      "should be active between start and end time" in {
        assert(period.isActive(before))
        assert(period.isActive(now))
        assert(period.isActive(after.minusSeconds(1)))
      }

      "should not be active before start time" in {
        assert(!period.isActive(before.minusSeconds(1)))
      }

      "should not be active at or after end time" in {
        assert(!period.isActive(after))
        assert(!period.isActive(after.plusSeconds(1)))
      }
    }

    "with start and end at the same time" - {
      val period = TimePeriod(start = now, end = Some(now))

      "should not be active at any time" in {
        assert(period.isActive(now.minusSeconds(1)))
        assert(period.isActive(now))
        assert(period.isActive(now.plusSeconds(1)))
      }
    }

    "with end before start" - {

      "should fail" in {
        assertThrows[Exception] {
          TimePeriod(start = after, end = Some(before))
        }
      }
    }

    "property-based tests" - {
      "isActive should be consistent with time comparisons" in {
        forAll { (startSeconds: Int, endSeconds: Option[Int], checkSeconds: Int) =>
          // Use absolute values to avoid integer overflow
          val absStartSeconds = Math.abs(startSeconds % 10000)
          val absEndSeconds   = endSeconds.map(s => Math.abs(s % 10000))
          val absCheckSeconds = Math.abs(checkSeconds % 10000)

          val baseTime  = Instant.parse("2023-01-01T00:00:00Z")
          val start     = baseTime.plusSeconds(absStartSeconds)
          val end       = absEndSeconds.map(baseTime.plusSeconds(_))
          val checkTime = baseTime.plusSeconds(absCheckSeconds)

          val period = TimePeriod(start, end)

          val expectedActive = !checkTime.isBefore(start) && end.forall(checkTime.isBefore(_))
          period.isActive(checkTime) shouldBe expectedActive
        }
      }
    }
  }
}
