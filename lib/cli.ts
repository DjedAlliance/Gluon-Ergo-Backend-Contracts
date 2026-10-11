import { existsSync } from "node:fs";
import type { Box, SignedTransaction } from "@fleet-sdk/common";
import { Network } from "@fleet-sdk/core";
import type { ErgoHDKey } from "@fleet-sdk/wallet";
import { type DeploymentConfig, type NetworkConfig, type NetworkName, formatUnits, isTodo, readNetworks } from "./config.ts";
import { MAX_ORACLE_AGE } from "./math.ts";
import { ErgoNode } from "./node.ts";

// Helpers shared by the scripts in scripts/.

/** Loads .env, which holds only secrets (N2). */
export function loadEnvironment(): void {
  if (existsSync(".env")) process.loadEnvFile(".env");
}

/** The node of the deployment's network, from config/networks.json. */
export function nodeFor(config: DeploymentConfig): { node: ErgoNode; network: NetworkConfig } {
  const network = readNetworks()[config.network];
  if (isTodo(network.nodeUrl)) throw new Error(`set the ${config.network} node URL in config/networks.json`);
  return { node: new ErgoNode(network.nodeUrl), network };
}

/** The P2PK address of a key on a network. */
export function addressOf(key: ErgoHDKey, network: NetworkName): string {
  return key.address.encode(network === "mainnet" ? Network.Mainnet : Network.Testnet);
}

export function erg(nanoerg: bigint): string {
  return `${formatUnits(nanoerg)} ERG`;
}

/**
 * The unspent box whose first token is `tokenId` (and whose ErgoTree is `ergoTree`, if given): the box the
 * contract finds by that token. Unconfirmed boxes count, and boxes spent in the mempool do not.
 */
export async function uniqueBox(node: ErgoNode, tokenId: string, what: string, ergoTree?: string): Promise<Box<bigint>> {
  const boxes = (await node.unspentByTokenId(tokenId)).filter(
    (b) => b.assets[0]?.tokenId === tokenId && (ergoTree === undefined || b.ergoTree === ergoTree),
  );
  if (boxes.length > 1) {
    throw new Error(
      `${boxes.length} unspent ${what} boxes hold token ${tokenId}: unconfirmed transactions compete to replace the box, so try again after the next block`,
    );
  }
  if (boxes.length === 0) throw new Error(`no unspent ${what} box holds token ${tokenId} first`);
  return boxes[0];
}

/** Throws unless the contract would accept `oracle` in a transaction built at chain height `height`. */
export function requireFreshOracle(oracle: Box<bigint>, height: number): void {
  const age = height + 1 - oracle.creationHeight;
  if (age >= MAX_ORACLE_AGE) {
    throw new Error(`the oracle box ${oracle.boxId} is ${age} blocks old and the contract accepts at most ${MAX_ORACLE_AGE - 1}; wait for the oracle's next update`);
  }
}

/** The fewest boxes, largest first, that hold at least `amount` nanoERG. */
export function selectFunds(boxes: Box<bigint>[], amount: bigint): Box<bigint>[] {
  const selected: Box<bigint>[] = [];
  let total = 0n;
  for (const box of [...boxes].sort((a, b) => (a.value < b.value ? 1 : a.value > b.value ? -1 : 0))) {
    if (total >= amount) break;
    selected.push(box);
    total += box.value;
  }
  if (total < amount) throw new Error(`insufficient funds: ${erg(total)} available, ${erg(amount)} needed`);
  return selected;
}

/** Has the node validate a signed transaction and, if `submit`, broadcasts it. Returns the transaction id. */
export async function checkOrSubmit(node: ErgoNode, tx: SignedTransaction, submit: boolean): Promise<string> {
  if (!submit) {
    const id = await node.checkTransaction(tx);
    console.log(`The node accepts transaction ${id}. Run again with --submit to broadcast it.`);
    return id;
  }
  const id = await node.submitTransaction(tx);
  console.log(`Submitted transaction ${id}.`);
  return id;
}

/** Runs a script's main function, printing errors without a stack trace. */
export function run(main: () => Promise<void>): void {
  main().catch((e: unknown) => {
    console.error(`error: ${e instanceof Error ? e.message : String(e)}`);
    process.exit(1);
  });
}
