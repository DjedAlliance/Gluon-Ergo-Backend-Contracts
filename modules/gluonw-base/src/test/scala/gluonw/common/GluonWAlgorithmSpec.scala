package gluonw.common

import gluonw.boxes.GluonWBoxConstants

import scala.util.Random

class GluonWAlgorithmSpec extends GluonWBase {
  client.setClient()
  val gluonWConstants: GluonWConstants = GluonWConstants()

  "GluonWAlgorithm: getVolume Function" should {
    val gluonWAlgorithm: GluonWAlgorithm = GluonWAlgorithm(gluonWConstants)

    var currentHeight: Long = 1000L
    var lastBlockHeight: Long = 900L
    var dayBlockHeight: Long = 720L
    val addHeight: Long = 100L
    var volumeListToAdd: List[Long] = List.fill(GluonWBoxConstants.BUCKETS)(0L)
    var volumeListToPreserved: List[Long] =
      List.fill(GluonWBoxConstants.BUCKETS)(0L)
    var volumeListToAddExpected: List[Long] = List.fill(14)(0L)
    var volumeListToPreservedExpected: List[Long] = List.fill(14)(0L)

    "Get Correct values in volume lists" in {
      (1 to 1000).foreach { _ =>
        val m: Long = new Random().between(1_000L, 1_000_000_000L)

        val (volumeListToAddResult, volumeListToPreservedResult) =
          gluonWAlgorithm.getVolumes(
            currentHeight = currentHeight,
            mVolumeInErgs = m,
            lastDayBlockHeight = dayBlockHeight,
            volumeListToAdd = volumeListToAdd,
            volumeListToPreserved = volumeListToPreserved
          )

        volumeListToAdd = volumeListToAddResult
        volumeListToPreserved = volumeListToPreservedResult

        val nDays =
          (currentHeight - dayBlockHeight) / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET

        lastBlockHeight = currentHeight
        currentHeight += addHeight

        // We get nDays to know how much we need to move forward
        if (nDays <= 0) {
          volumeListToAddExpected =
            List(volumeListToAddExpected.head + m) ++ volumeListToAddExpected
              .slice(1, volumeListToAddExpected.length)
        } else {
          volumeListToAddExpected = List(m) ++ volumeListToAddExpected.slice(
            0,
            volumeListToAddResult.length - 1
          )
          dayBlockHeight =
            (currentHeight / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET) * GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET
        }

        assert(
          volumeListToAddResult.equals(volumeListToAddExpected),
          s"volumeListToAddResult ${volumeListToAddResult.toString()}, volumeListToAddExpected ${volumeListToAddExpected.toString()}"
        )
        assert(
          volumeListToPreservedResult.equals(volumeListToPreservedExpected),
          s"volumeListToPreservedResult ${volumeListToPreservedResult}, volumeListToPreservedExpected ${volumeListToPreservedExpected}"
        )
      }
    }
  }

  "GluonWAlgorithm: getVolume Function (variable addHeight)" should {
    val gluonWAlgorithm: GluonWAlgorithm = GluonWAlgorithm(gluonWConstants)

    var currentHeight: Long = 1000L
    var lastBlockHeight: Long = 900L
    var dayBlockHeight: Long = 720L
    var volumeListToAdd: List[Long] = List.fill(GluonWBoxConstants.BUCKETS)(0L)
    var volumeListToPreserved: List[Long] =
      List.fill(GluonWBoxConstants.BUCKETS)(0L)
    var volumeListToAddExpected: List[Long] = List.fill(14)(0L)
    var volumeListToPreservedExpected: List[Long] = List.fill(14)(0L)

    "Get Correct values in volume lists, Max 2 days" in {
      (1 to 1000).foreach { _ =>
        var addHeight: Long = new Random().between(100L, 1_000L)
        val m: Long = new Random().between(1_000L, 1_000_000_000L)

        val (volumeListToAddResult, volumeListToPreservedResult) =
          gluonWAlgorithm.getVolumes(
            currentHeight = currentHeight,
            mVolumeInErgs = m,
            lastDayBlockHeight = dayBlockHeight,
            volumeListToAdd = volumeListToAdd,
            volumeListToPreserved = volumeListToPreserved
          )

        volumeListToAdd = volumeListToAddResult
        volumeListToPreserved = volumeListToPreservedResult

        val nDays: Int =
          ((currentHeight - dayBlockHeight) / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET).toInt

        lastBlockHeight = currentHeight
        currentHeight += addHeight

        // We get nDays to know how much we need to move forward
        if (nDays <= 0) {
          volumeListToAddExpected =
            List(volumeListToAddExpected.head + m) ++ volumeListToAddExpected
              .slice(1, volumeListToAddExpected.length)
        } else {
          volumeListToAddExpected = List(m) ++ List.fill(
            if ((nDays) < GluonWBoxConstants.BUCKETS) { nDays - 1 }
            else { GluonWBoxConstants.BUCKETS - 1 }
          )(0L) ++ volumeListToAddExpected.slice(
            0,
            GluonWBoxConstants.BUCKETS - nDays
          )
          dayBlockHeight =
            (currentHeight / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET) * GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET
        }

        assert(
          volumeListToAddResult.equals(volumeListToAddExpected),
          s"volumeListToAddResult ${volumeListToAddResult.toString()}, volumeListToAddExpected ${volumeListToAddExpected.toString()}"
        )
        assert(volumeListToAddResult.length == 14)
        assert(
          volumeListToPreservedResult.equals(volumeListToPreservedExpected),
          s"volumeListToPreservedResult ${volumeListToPreservedResult}, volumeListToPreservedExpected ${volumeListToPreservedExpected}"
        )
        assert(volumeListToPreservedResult.length == 14)
      }
    }

    "Get Correct values in volume lists" in {
      (1 to 1000).foreach { _ =>
        var addHeight: Long = new Random().between(100L, 15_000L)
        val m: Long = new Random().between(1_000L, 1_000_000_000L)

        val (volumeListToAddResult, volumeListToPreservedResult) =
          gluonWAlgorithm.getVolumes(
            currentHeight = currentHeight,
            mVolumeInErgs = m,
            lastDayBlockHeight = dayBlockHeight,
            volumeListToAdd = volumeListToAdd,
            volumeListToPreserved = volumeListToPreserved
          )

        volumeListToAdd = volumeListToAddResult
        volumeListToPreserved = volumeListToPreservedResult

        val nDays: Int =
          ((currentHeight - dayBlockHeight) / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET).toInt

        lastBlockHeight = currentHeight
        currentHeight += addHeight

        // We get nDays to know how much we need to move forward
        if (nDays <= 0) {
          volumeListToAddExpected =
            List(volumeListToAddExpected.head + m) ++ volumeListToAddExpected
              .slice(1, volumeListToAddExpected.length)
        } else {
          volumeListToAddExpected = List(m) ++ List.fill(
            if ((nDays) < GluonWBoxConstants.BUCKETS) { nDays - 1 }
            else { GluonWBoxConstants.BUCKETS - 1 }
          )(0L) ++ volumeListToAddExpected.slice(
            0,
            GluonWBoxConstants.BUCKETS - nDays
          )
          dayBlockHeight =
            (currentHeight / GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET) * GluonWBoxConstants.BLOCKS_PER_VOLUME_BUCKET
        }

        assert(
          volumeListToAddResult.equals(volumeListToAddExpected),
          s"volumeListToAddResult ${volumeListToAddResult.toString()}, volumeListToAddExpected ${volumeListToAddExpected.toString()}"
        )
        assert(volumeListToAddResult.length == 14)
        assert(
          volumeListToPreservedResult.equals(volumeListToPreservedExpected),
          s"volumeListToPreservedResult ${volumeListToPreservedResult}, volumeListToPreservedExpected ${volumeListToPreservedExpected}"
        )
        assert(volumeListToPreservedResult.length == 14)
      }
    }
  }
}
