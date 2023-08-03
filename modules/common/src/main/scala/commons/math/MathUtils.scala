package commons.math

object MathUtils {

  def ~=(x: Double, y: Double, precision: Int) = {
    val threshold = 1.0 / math.pow(10, precision)
    val diff = (x - y).abs
    diff < threshold
  }
}
