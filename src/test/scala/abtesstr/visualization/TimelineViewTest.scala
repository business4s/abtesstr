package abtesstr.visualization

import abtesstr.ABModelDTO
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.jdk.DurationConverters.*

class TimelineViewTest extends AnyFreeSpec {
  "fromABModelDTO" - {
    "groups by space ID" in {
      val now = Instant.now
      val tv  = createTimeline(
        List(
          experiment("a", now, None, 0, 0.1, "spaceA"),
          experiment("a", now, None, 0, 0.1, "spaceB"),
        ),
      )
      assert(tv.spaces.keySet == Set("spaceA", "spaceB"))
    }

    "sums bucket ranges per experiment" in {
      val t1  = Instant.now
      val tv  = createTimeline(
        List(
          experiment("exp1", t1, None, 0L, fraction = 0.5),
          experiment("exp1", t1, None, 0L, fraction = 0.25),
        ),
      )
      val exp = tv.spaces(defaultSpaceId).experiments.head
      assert(exp.spacePercentage == 0.75)
    }

    "handle overlapping segments" in {
      val e1   = experiment("exp1", Instant.now, Some(1000.seconds), 0L, fraction = 0.5)
      val e2   = experiment("exp1", e1.endExcl.get.minusSeconds(100), None, 0L, fraction = 0.25)
      val tv   = createTimeline(List(e1, e2))
      val exps = tv.spaces(defaultSpaceId).experiments
      assert(
        exps == List(
          TimelineView.Experiment("exp1", e1.startIncl, Some(e2.startIncl), 0.5),
          TimelineView.Experiment("exp1", e2.startIncl, e1.endExcl, 0.75),
          TimelineView.Experiment("exp1", e1.endExcl.get, e2.endExcl, 0.25),
        ),
      )
    }

  }

  def createTimeline(exps: List[ABModelDTO.Experiment]): TimelineView = TimelineView.fromDTO(ABModelDTO("1", "sha256", exps))

  val defaultSpaceId = "test-test-space"
  private def experiment(
      expId: String,
      start: Instant,
      time: Option[FiniteDuration],
      bucketStart: Long,
      fraction: Double,
      testSpaceId: String = defaultSpaceId,
  ): ABModelDTO.Experiment = ABModelDTO.Experiment(
    testSpaceId = testSpaceId,
    experimentId = expId,
    startIncl = start,
    endExcl = time.map(x => start.plus(x.toJava)),
    bucketStart = bucketStart,
    bucketEnd = bucketStart + ((Long.MaxValue / 100.0) * fraction).toLong,
  )
}
