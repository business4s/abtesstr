package abtesstr

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SpaceFragmentTest extends AnyFreeSpec with ScalaCheckPropertyChecks with Generators {

  "length" in {
    assert(SpaceFragment(Point(0L), Point(0L)).length == 1)
    assert(SpaceFragment(Point(0L), Point(1L)).length == 2)
    assert(SpaceFragment(Point(0L), Point.max).length == SpaceSize)
  }
  "ofLength" in {
    assert(SpaceFragment(Point(0L), Point(0L)).length == 1)
    assert(SpaceFragment(Point(0L), Point(1L)).length == 2)
    assert(SpaceFragment(Point(0L), Point.max).length == SpaceSize)
  }

  "length == ofLength" in {
    forAll { (start: Point, length: FragmentLength) =>
      if (start <= SpaceSize - length) { 
        assert(SpaceFragment.ofLength(start, length).length == length)
      }
    }
  }
  
  "wholeSpace" in {
    assert(SpaceFragment.wholeSpace.length == SpaceSize)
    assert(SpaceFragment.wholeSpace.spaceFraction == 1.0)
    assert(SpaceFragment.wholeSpace.start == Point.zero)
    assert(SpaceFragment.wholeSpace.end == Point.max)
  }
}
