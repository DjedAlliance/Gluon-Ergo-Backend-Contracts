// Testnet stand-ins for an oracle pool box and its buyback box, for rehearsing a deployment on testnet, where
// the mainnet oracles do not exist.
//
// The first run mints a pool NFT into an oracle box that holds the price given with --price (nanoERG per unit
// of the pegged asset) in R4, and a buyback NFT into a box that anyone may top up, as oracle-core v2 buyback
// boxes allow. With --submit, it writes both token ids and the buyback contract's address into the config.
// Later runs recreate the oracle box, with the price given with --price or else the same price. The contract
// rejects oracle boxes created more than 34 blocks earlier, so run it again before each rehearsal step.
//
// The oracle box belongs to the second address of the wallet in .env (EIP-3 index 1), so that the other
// scripts, which spend boxes of the first address, cannot spend it by accident. The first address pays.
// Usage: node scripts/testnet-oracle.ts <testnet config file> [--price <nanoERG>] [--submit]
import { writeFileSync } from "node:fs";
import { parseArgs } from "node:util";
import { compile } from "@fleet-sdk/compiler";
import {
  type ErgoUnsignedTransaction,
  OutputBuilder,
  RECOMMENDED_MIN_FEE_VALUE,
  SAFE_MIN_BOX_VALUE,
  TransactionBuilder,
} from "@fleet-sdk/core";
import { SLong } from "@fleet-sdk/serializer";
import { addressOf, checkOrSubmit, loadEnvironment, nodeFor, run, selectFunds, uniqueBox } from "../lib/cli.ts";
import { type DeploymentConfig, isTodo, parseDecimal, readDeployment } from "../lib/config.ts";
import { oraclePrice } from "../lib/reactor.ts";
import { keyFromEnvironment, signTransaction } from "../lib/signer.ts";

// Anyone may spend the buyback box, provided an output keeps its script and tokens and holds more ERG.
const STAND_IN_BUYBACK = `{
  sigmaProp(OUTPUTS.exists { (box: Box) =>
    box.propositionBytes == SELF.propositionBytes && box.tokens == SELF.tokens && box.value > SELF.value
  })
}`;

run(async () => {
  const { values, positionals } = parseArgs({
    allowPositionals: true,
    options: { price: { type: "string" }, submit: { type: "boolean", default: false } },
  });
  if (positionals.length !== 1) throw new Error("usage: node scripts/testnet-oracle.ts <testnet config file> [--price <nanoERG>] [--submit]");
  const path = positionals[0];
  const config = readDeployment(path);
  if (config.network !== "testnet") throw new Error("the stand-in oracle is for testnet configs only");
  const price = values.price === undefined ? undefined : parseDecimal(values.price, 0);
  if (price === 0n) throw new Error("the price must be positive");

  loadEnvironment();
  const payer = keyFromEnvironment(0);
  const operator = keyFromEnvironment(1);
  const payerAddress = addressOf(payer, "testnet");
  const operatorAddress = addressOf(operator, "testnet");
  const { node } = nodeFor(config);
  const height = await node.height();
  const sign = (tx: ErgoUnsignedTransaction) => signTransaction(node, "testnet", tx.toEIP12Object(), [payer, operator]);

  if (isTodo(config.oracle.poolNft)) {
    if (price === undefined) throw new Error("the first run creates the oracle box, so it needs --price");
    const buybackTree = compile(STAND_IN_BUYBACK, { version: 0, network: "testnet" });
    const mintOracle = new TransactionBuilder(height)
      .from(await node.unspentByAddress(payerAddress))
      .to(
        new OutputBuilder(SAFE_MIN_BOX_VALUE, operatorAddress)
          .mintToken({ amount: 1n })
          .setAdditionalRegisters({ R4: SLong(price).toHex() }),
      )
      .sendChangeTo(payerAddress)
      .payFee(RECOMMENDED_MIN_FEE_VALUE)
      .build();
    const mintBuyback = new TransactionBuilder(height)
      .from(mintOracle.change)
      .to(
        new OutputBuilder(SAFE_MIN_BOX_VALUE, buybackTree).mintToken({
          amount: 1n,
          name: "TestBuyback",
          description: `Stand-in oracle buyback NFT for the testnet deployment ${config.deployment}`,
          decimals: 0,
        }),
      )
      .sendChangeTo(payerAddress)
      .payFee(RECOMMENDED_MIN_FEE_VALUE)
      .build();
    const updated: DeploymentConfig = {
      ...config,
      oracle: {
        ...config.oracle,
        poolNft: mintOracle.outputs[0].assets[0].tokenId,
        buybackNft: mintBuyback.outputs[0].assets[0].tokenId,
        buybackContractAddress: buybackTree.toAddress().encode(),
      },
    };
    console.log(`Oracle box        ${operatorAddress}, price ${price} nanoERG
Pool NFT          ${updated.oracle.poolNft}
Buyback NFT       ${updated.oracle.buybackNft}
Buyback contract  ${updated.oracle.buybackContractAddress}`);
    const signed = [await sign(mintOracle), await sign(mintBuyback)];
    if (!values.submit) {
      await checkOrSubmit(node, signed[0], false);
      return;
    }
    for (const tx of signed) await checkOrSubmit(node, tx, true);
    writeFileSync(path, `${JSON.stringify(updated, null, 2)}\n`);
    console.log(`Wrote the oracle's NFTs and the buyback contract's address into ${path}.`);
    return;
  }

  const oracle = await uniqueBox(node, config.oracle.poolNft, "stand-in oracle");
  if (oracle.ergoTree !== operator.address.ergoTree) {
    throw new Error(`the oracle box does not belong to ${operatorAddress}, the second address of the wallet in .env`);
  }
  const newPrice = price ?? oraclePrice(oracle);
  const refresh = new TransactionBuilder(height)
    .from([oracle, ...selectFunds(await node.unspentByAddress(payerAddress), RECOMMENDED_MIN_FEE_VALUE + SAFE_MIN_BOX_VALUE)], {
      ensureInclusion: true,
    })
    .to(new OutputBuilder(oracle.value, operatorAddress).addTokens(oracle.assets).setAdditionalRegisters({ R4: SLong(newPrice).toHex() }))
    .sendChangeTo(payerAddress)
    .payFee(RECOMMENDED_MIN_FEE_VALUE)
    .build();
  console.log(`Oracle box ${oracle.boxId}: price ${oraclePrice(oracle)} → ${newPrice} nanoERG, created at height ${height}`);
  await checkOrSubmit(node, await sign(refresh), values.submit);
});
