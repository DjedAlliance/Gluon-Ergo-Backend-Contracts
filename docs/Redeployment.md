# Redeployment of GluonW box

This is a step-by-step guide on redeployment of GluonW box.
Whenever there is a change in the contract, the GluonW box should be redeployed. We've written a script to make this easy.

1. Get the current GluonW Box Id from explorer, [mainnet](https://explorer.ergoplatform.com) or [testnet](https://testnet.ergoplatform.com)
    1. The best way to retrieve the right box is using this api, replacing {p1} to the GluonWBox NFT
    ```
   // mainnet
   https://api.ergoplatform.com/api/v1/boxes/unspent/byTokenId/{p1}
   
   // testnet
   https://api-testnet.ergoplatform.com/api/v1/boxes/unspent/byTokenId/{p1}
   ```   
    2. You can retrieve the NFT id from [reference.conf](../modules/common/src/main/resources/reference.conf) under the tokens object. Make sure to select the correct id based on the network type.
2. When you get the api, you'll get a box object, the boxId is at the top.
3. Go to [BoxCreation.scala](../modules/gluonw-base/src/main/scala/gluonw/tools/BoxCreation.scala)
4. On line 28, set isMainnet to true or false depending of the network of the deployment.
```scala
   val isMainnet: Boolean = true // or false for testnet deployment.
```
5. On line 79, change runTx to MUTATE
```scala
    val runTx: String = MUTATE
```
6. Change the boxId on line 129:
```scala
      case MUTATE => {
        val boxIdToMutate: String =
          "current-gluon-box-id"
```
7. Run the program. (This is pre-multisig)
   