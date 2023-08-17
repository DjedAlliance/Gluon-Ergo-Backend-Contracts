package gluonw.txs

import edge.registers.{IntRegister, LongRegister}
import gluonw.boxes.OracleBox
import gluonw.common.GluonWBase
import org.ergoplatform.sdk.{ErgoId, ErgoToken}

class OracleHackSpec extends GluonWBase {
  "Oracle Hack" should {
    val fakeOracleBox: OracleBox =
      OracleBox(
        value = 10000000L,
        epochIdRegister = new IntRegister(1396),
        priceRegister = new LongRegister(52594551964068L),
        tokens = Seq(
          ErgoToken(
            ErgoId.create(
              "001e182cc3f04aec4486c7a5018d198e9591a7cfb0b372f5f95fa3e5ddbd24d3"
            ),
            1
          ),
          ErgoToken(
            ErgoId.create(
              "56aeed3ba3f677ffb5462b0b1f83da3e1d06c8946ba978ef7e706221bac5e982"
            ),
            295
          )
        )
      )
  }
}
