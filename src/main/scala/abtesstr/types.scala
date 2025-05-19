package abtesstr

import java.time.Instant
import scala.Predef.???
import scala.collection.immutable.NumericRange
import scala.math.Ordering.Long.mkOrderingOps
import scala.util.chaining.*

opaque type TestSpaceId <: String = String

object TestSpaceId {
  def apply(value: String): TestSpaceId = value
}

opaque type ExperimentId <: String = String

object ExperimentId {
  def apply(value: String): ExperimentId = value
}

opaque type SpaceFraction <: Double = Double

object SpaceFraction {
  def apply(value: Double): SpaceFraction = value
  def zero: SpaceFraction                 = SpaceFraction(0.0)

  extension (x: SpaceFraction) {
    def length: FragmentLength = FragmentLength((x * SpaceSize).toLong)
  }

//  given Fractional[SpaceFraction] & Ordering[SpaceFraction] = Numeric.DoubleIsFractional
}

opaque type FragmentLength <: Long = Long

object FragmentLength {
  def apply(value: Long): FragmentLength = {
    Predef.assert(value >= 0, "Fragment length must be positive")
    value
  }

  def min(a: FragmentLength, b: FragmentLength): FragmentLength = a.min(b)

  extension (x: FragmentLength) {
    def minus(other: FragmentLength): FragmentLength = apply(x - other)

    def asFraction: SpaceFraction = SpaceFraction(x.toDouble / SpaceSize)
  }
}

opaque type UserId <: String = String

object UserId {
  def apply(value: String): UserId = value
}

case class SpaceFragment(start: Point, end: Point) {
  Predef.assert(start <= end, "end must be after start")

  def length: FragmentLength       = FragmentLength(end - start + 1)
  def spaceFraction: SpaceFraction = SpaceFraction(length.toDouble / SpaceSize)

  def contains(point: Point): Boolean = point >= start && point <= end
}

object SpaceFragment {

  def wholeSpace: SpaceFragment = SpaceFragment(Point.zero, Point.max)

  def ofFraction(start: Point, fraction: SpaceFraction): SpaceFragment = {
    SpaceFragment.ofLength(start, fraction.length)
  }

  def ofLength(start: Point, length: FragmentLength): SpaceFragment = {
    SpaceFragment(start, start.add(length - 1))
  }

  def remaining(start: Point): SpaceFragment = SpaceFragment(start, Point.max)

}

opaque type Point <: Long = Long

object Point {
  def apply(value: Long): Point = {
    Predef.assert(value >= 0, "Points can only be positive")
    Predef.assert(value < SpaceSize, s"Points have to be below ${SpaceSize}")
    value
  }
  def zero                      = Point(0)
  def max                       = Point(SpaceSize - 1)

  given Ordering[Point] & Integral[Point] = Numeric.LongIsIntegral

  extension (x: Point) {
    def next                    = Point(x + 1)
    def add(other: Long): Point = Point(Math.addExact(x, other))
    def sub(other: Long): Point = Point(Math.subtractExact(x, other))
  }
}

// start inclusive, end exclusive
case class TimePeriod(startIncl: Instant, endExcl: Option[Instant]) {
  Predef.assert(endExcl.forall(_.isAfter(startIncl)), "end must be after start")

  def isActive(at: Instant): Boolean =
    !at.isBefore(startIncl) && endExcl.forall(at.isBefore)
}

val SpaceSize: Long = Long.MaxValue

case class ExperimentRun(
    experimentId: ExperimentId,
    period: TimePeriod,
    bucket: SpaceFragment,
)

case class TestSpace(
    id: TestSpaceId,
    experiments: List[ExperimentRun],
) {
  def activeRanges(at: Instant): List[SpaceFragment] =
    activeExperiments(at).map(_.bucket)

  def availableRanges(at: Instant): List[SpaceFragment] = {
    val used      = activeRanges(at).sortBy(_.start)
    val gaps      = used
      .sliding(2)
      .flatMap {
        case List(a, b) => Option.when(b.start - a.end > 1)(SpaceFragment(a.end.next, b.start.sub(1)))
        case List(_)    => None
      }
      .toList
    val remaining = used.lastOption match {
      case Some(last) => Option.when(last.end < Point.max)(SpaceFragment.remaining(last.end.next))
      case None       => Some(SpaceFragment.wholeSpace)
    }
    gaps ++ remaining
  }

  def findFit(size: SpaceFraction, at: Instant): Option[List[SpaceFragment]] = {
    val requiredSpace = size.length
    val (remaining, fragments) = availableRanges(at)
      .foldLeft((requiredSpace, List.empty[SpaceFragment])) { case ((remainingRequiredSpace, acc), freeFragment) =>
        if (remainingRequiredSpace > 0) {
          val usedFragment = SpaceFragment.ofLength(freeFragment.start, FragmentLength.min(remainingRequiredSpace, freeFragment.length))
          (remainingRequiredSpace.minus(usedFragment.length), acc :+ usedFragment)
        } else {
          (remainingRequiredSpace, acc)
        }
      }
    Option.when(remaining <= 0)(fragments)
  }

  def availableSpace(at: Instant): SpaceFraction = SpaceFraction(availableRanges(at).map(_.spaceFraction).sum)

  def activeExperiments(at: Instant): List[ExperimentRun] = {
    experiments.filter(_.period.isActive(at))
  }

}
