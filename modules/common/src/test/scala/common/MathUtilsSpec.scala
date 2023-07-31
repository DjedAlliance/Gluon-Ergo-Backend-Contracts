package common

import commons.math.MathUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MathUtilsSpec extends AnyWordSpec with Matchers {
  "~=" should {
    "get right precision" in {
      assert(!MathUtils.~=(1000, 1001, 5))
      assert(!MathUtils.~=(1000, 1010, 5))
      assert(MathUtils.~=(1000, 1000.01, 5))
      assert(MathUtils.~=(1000, 1000.001, 6))
      assert(MathUtils.~=(1_000_000, 1_000_001, 5))
    }
  }
}
