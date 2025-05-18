package abtesstr

import org.scalacheck.Arbitrary
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Instant

class TestSpaceTest extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators {

  "TestSpace" - {
    val now         = Instant.parse("2023-01-01T00:00:00Z")
    val testSpaceId = TestSpaceId("test-space")

    "when empty" - {
      val emptySpace = TestSpace(testSpaceId, List.empty)

      "should have no active ranges" in {
        emptySpace.activeRanges(now) shouldBe empty
      }

      "should have the entire bucket scale as available range" in {
        val availableRanges = emptySpace.availableRanges(now)
        assert(availableRanges == List(SpaceFragment.wholeSpace))
      }

      "should have 100% available space" in {
        emptySpace.availableSpace(now) shouldBe SpaceFraction(1.0)
      }

      "should be able to allocate an experiment of any size up to 100%" in {
        emptySpace.findFit(SpaceFraction(0.5), now) should not be empty
        emptySpace.findFit(SpaceFraction(1.0), now) should not be empty
      }
    }

    "with active experiments" - {
      val exp1 = ExperimentRun(
        experimentId = ExperimentId("exp1"),
        period = TimePeriod(start = now.minusSeconds(100), end = None),
        bucket = SpaceFragment(Point(0L), Point(SpaceSize / 4 - 1)),
      )

      val exp2 = ExperimentRun(
        experimentId = ExperimentId("exp2"),
        period = TimePeriod(start = now.minusSeconds(50), end = None),
        bucket = SpaceFragment(Point(SpaceSize / 2), Point(3L * (SpaceSize / 4))),
      )

      val testSpace = TestSpace(testSpaceId, List(exp1, exp2))

      "should return active ranges correctly" in {
        val activeRanges = testSpace.activeRanges(now)
        assert(activeRanges == List(exp1.bucket, exp2.bucket))
      }

      "should calculate available ranges correctly" in {
        val availableRanges = testSpace.availableRanges(now)
        assert(
          availableRanges == List(
            SpaceFragment(exp1.bucket.end.add(1), exp2.bucket.start.sub(1)),
            SpaceFragment(exp2.bucket.end.add(1), Point.max),
          ),
        )
      }

      "should calculate available space correctly" in {
        testSpace.availableSpace(now) shouldBe SpaceFraction(0.5)
      }

      "should find next fit in available space" in {
        val requiredFraction = SpaceFraction(0.2)
        val nextFit          = testSpace.findFit(requiredFraction, now)
        assert(nextFit == List(SpaceFragment.ofFraction(exp1.bucket.end.add(1), requiredFraction)))
      }

      "should find next fit even if no contiguous space is available" in {
        val exp1 = ExperimentRun(
          experimentId = ExperimentId("exp1"),
          period = TimePeriod(start = now.minusSeconds(100), end = None),
          bucket = SpaceFragment(Point.zero, Point(10L)),
        )

        val exp2 = ExperimentRun(
          experimentId = ExperimentId("exp2"),
          period = TimePeriod(start = now.minusSeconds(50), end = None),
          bucket = SpaceFragment(Point(20L), Point(30L)),
        )

        val testSpace = TestSpace(testSpaceId, List(exp1, exp2))

        val requiredLength = FragmentLength(40L)
        val nextFit        = testSpace.findFit(requiredLength.asFraction, now)
        assert(
          nextFit == List(
            SpaceFragment.ofLength(exp1.bucket.end.add(1), FragmentLength(9L)),
            SpaceFragment.ofLength(exp2.bucket.end.add(1), FragmentLength(31L)),
          ),
        )
      }
    }

    "with expired experiments" - {
      val expiredExp = ExperimentRun(
        experimentId = ExperimentId("expired"),
        period = TimePeriod(start = now.minusSeconds(200), end = Some(now.minusSeconds(100))),
        bucket = SpaceFragment(Point(0L), Point(SpaceSize / 2)),
      )

      val testSpace = TestSpace(testSpaceId, List(expiredExp))

      "should not include expired experiments in active ranges" in {
        testSpace.activeRanges(now) shouldBe empty
      }
    }

    "find fit should always return the requested fraction" in {
      forAll(genSpaceFraction(max = 0.2), genTestSpace) { (fraction, testSpace) =>
        val nextFit = testSpace.findFit(fraction, now)
        assert(nextFit.map(_.length).sum == fraction.length)
      }
    }
    "availableSpace should be 1 - sum of all active experiments" in {
      forAll(genTestSpace) { testSpace =>
        val now = Instant.now() // TODO generate as well
        assert(testSpace.availableRanges(now).map(_.length).sum == SpaceSize - testSpace.activeRanges(now).map(_.length).sum)
        assert(testSpace.availableSpace(now) == 1 - testSpace.activeExperiments(now).map(_.bucket.spaceFraction).sum)
      }
    }
  }
}
