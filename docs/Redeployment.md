# Redeployment of GluonW box

This is a step-by-step guide on redeployment of GluonW box.
Whenever there is a change in the contract, the GluonW box should be redeployed. We've written a script to make this easy.

1. Get the current GluonW Box Id from explorer, [mainnet](https://explorer.ergoplatform.com) or [testnet](https://testnet.ergoplatform.com)
    1. The best way to retrieve the right box is using this api, replacing {p1} to the GluonWBox NFT
    ```
   // mainnet
   https://api.ergoplatform.com/api/v1/boxes/byTokenId/{p1}
   
   // testnet
   https://api-testnet.ergoplatform.com/api/v1/boxes/unspent/byTokenId/{p1}
   ```   
    2. You can retrieve the NFT id from [reference.conf](../modules/common/src/main/resources/reference.conf) under tokens
   ```conf
   tokens = {
        MAINNET  = {
            gluonWNft = "d596ef0352bb4c3003b214d85002f94180bd7e7b7070e26d990bd039be574a14"
            neutron = "28e021a9f48b9ff43ba42e23280faf2761704988f0e27f71f3a604a681da8aad"
            proton = "79cb9717129c34b9f9c68c35813d71c885aef1a7a129e3b35aaa7738f15e8818"
        }

        TESTNET  = {
            gluonWNft = "1c9739b90e1a7fb650183e2337973757027691c973bc1f27d9a487e690d28a40"
            neutron = "b7106b754712c4fc45aa2a845ba652a3d7795aa32298b7b453623086d4ae9a14"
            proton = "00fad905aa7210590094193b2743b163149eee95c0eeac8732fa69fb5bf77c44"
        }
   }
    ```
    3. Make sure you check the file to get the right nft id
2. When you get the api, you'll get a box object, the boxId is at the top.
3. Go to [BoxCreation.scala](../modules/gluonw-base/src/main/scala/gluonw/tools/BoxCreation.scala)
4. On line 78, change runTx to MUTATE
```scala
    val runTx: String = MUTATE
```
5. Change the boxId on line 134:
```scala
      case MUTATE => {
        val boxIdToMutate: String =
          "5778651e6e9a3748da4158b644ee649d4794e2057f3b999d9f21fb8652d12cb9"
```
6. Run the program. (This is pre-multisig)
   