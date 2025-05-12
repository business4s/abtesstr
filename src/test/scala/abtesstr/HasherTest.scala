package abtesstr

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class HasherTest extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks {

  "Hasher" - {
    "sha256" - {
      val hasher = Hasher.sha256      

      "should handle empty strings" in {
        val point = hasher.hash("")
        point should be >= 0L
        point should be < SpaceSize
      }

      "property-based tests" - {
        "should produce different results for different inputs" in {
          forAll { (s1: String, s2: String) =>
            if (s1 == s2) assert(hasher.hash(s1) == hasher.hash(s2))
            else assert(hasher.hash(s1) != hasher.hash(s2))
          }
        }
        "should always produce points within the valid range" in {
          forAll { (s: String) =>
            val point = hasher.hash(s)
            assert(point >= 0L)
            assert(point < SpaceSize)
          }
        }

        "should be deterministic" in {
          forAll { (s: String) =>
            hasher.hash(s) shouldBe hasher.hash(s)
          }
        }
      }
    }
  }
}
