package abtesstr

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.funsuite.AnyFunSuiteLike

class SpaceFractionTest extends AnyFreeSpec {

  "length" in {
    assert(SpaceFraction(0.0).length == 0)
    assert(SpaceFraction(1.0).length == SpaceSize)
    assert(SpaceFraction(0.5).length == 0.5 * SpaceSize)
  }

}
