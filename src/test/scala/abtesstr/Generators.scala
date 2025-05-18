package abtesstr

import org.scalacheck.{Arbitrary, Gen}

import java.time.Instant

trait Generators {

  given Arbitrary[Point]          = Arbitrary(Gen.chooseNum(0L, SpaceSize - 1).map(Point(_)))
  given Arbitrary[FragmentLength] = Arbitrary(Gen.chooseNum(1L, SpaceSize).map(FragmentLength(_)))

  def genSpaceFraction(min: Double = 0.0, max: Double = 1.0): Gen[SpaceFraction] = Gen.chooseNum(min, max).map(SpaceFraction(_))

  def genSpaceSplit(max: Option[SpaceFraction] = None): Gen[Seq[SpaceFraction]] = {
    def loop(remaining: SpaceFraction, n: Int): Gen[Seq[SpaceFraction]] = {
      if (remaining == 0) Gen.const(Seq())
      else if (n <= 1) Gen.const(Seq(remaining))
      else
        for {
          fraction <- genSpaceFraction(0.0001.min(remaining), remaining)
          rest     <- loop(SpaceFraction(remaining - fraction), n - 1)
        } yield fraction +: rest
    }

    for {
      maxValue <- max.fold(genSpaceFraction(min = 0.0001))(Gen.const)
      size     <- Gen.chooseNum(1, 5)
      result   <- loop(maxValue, size)
    } yield result
  }

  val genNonOverlappingFragments: Gen[List[SpaceFragment]] = {
    for {
      totalSize <- genSpaceFraction(max = 0.5) // Use up to half of space
      fractions <- genSpaceSplit(Some(totalSize))
    } yield {
      fractions
        .foldLeft((Point.zero, List.empty[SpaceFragment])) { case ((start, fragments), fraction) =>
          val length    = FragmentLength((fraction * SpaceSize).toLong)
          val fragment  = SpaceFragment.ofLength(start, length)
          val gap       = Gen.oneOf(Gen.const(SpaceFraction.zero), genSpaceFraction(max = 0.1)).sample.get
          val nextStart = fragment.end.next.add(gap.length)
          (nextStart, fragment :: fragments)
        }
        ._2
        .reverse
    }
  }

  val genExperimentId = Gen.uuid.map(_.toString).map(ExperimentId(_))

  val genExperiment: SpaceFragment => Gen[ExperimentRun] = { fragment =>
    for {
      id    <- Gen.uuid.map(_.toString)
      now    = Instant.now()
      period = TimePeriod(now, None) // placeholder period
    } yield ExperimentRun(ExperimentId(id), period, fragment)
  }

  val genExperimentList: Gen[List[ExperimentRun]] =
    genNonOverlappingFragments.flatMap { fragments =>
      Gen.sequence[List[ExperimentRun], ExperimentRun](fragments.map(genExperiment))
    }

  val genTestSpace: Gen[TestSpace] =
    for {
      id   <- Gen.uuid.map(_.toString).map(TestSpaceId(_))
      exps <- genExperimentList
    } yield TestSpace(id, exps)

}
