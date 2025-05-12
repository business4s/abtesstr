package abtesstr

import org.scalacheck.{Arbitrary, Gen}

import java.time.Instant

trait Generators {

  given Arbitrary[Point]          = Arbitrary(Gen.chooseNum(0L, SpaceSize - 1).map(Point(_)))
  given Arbitrary[FragmentLength] = Arbitrary(Gen.chooseNum(1L, SpaceSize).map(FragmentLength(_)))
  
  def genSpaceFraction(max: Double = 1.0): Gen[SpaceFraction] = Gen.chooseNum(0.0, max).map(SpaceFraction(_))

  val genNonOverlappingFragments: Gen[List[SpaceFragment]] = {
    def loop(availableStart: Long, remaining: Long): Gen[List[SpaceFragment]] = {
      if (remaining < 1) Gen.const(Nil)
      else {
        for {
          length  <- Gen.chooseNum(1L, remaining.min(SpaceSize / 10)).map(FragmentLength(_)) // keep them small-ish
          fragment = SpaceFragment.ofLength(Point(availableStart), length)
          rest    <- loop(availableStart + length + 1, remaining - length - 1)               // leave 1 space between
        } yield fragment :: rest
      }
    }

    Gen.chooseNum(0L, SpaceSize / 2).flatMap(totalUsed => loop(0L, totalUsed))
  }

  val genExperiment: SpaceFragment => Gen[Experiment] = { fragment =>
    for {
      id    <- Gen.uuid.map(_.toString)
      now    = Instant.now()
      period = TimePeriod(now, None) // placeholder period
    } yield Experiment(ExperimentId(id), period, fragment)
  }

  val genExperimentList: Gen[List[Experiment]] =
    genNonOverlappingFragments.flatMap { fragments =>
      Gen.sequence[List[Experiment], Experiment](fragments.map(genExperiment))
    }

  val genTestSpace: Gen[TestSpace] =
    for {
      id    <- Gen.uuid.map(_.toString).map(TestSpaceId(_))
      exps <- genExperimentList
    } yield TestSpace(id, exps)

}
