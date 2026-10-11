import { readFileSync } from "node:fs";
import { compile } from "@fleet-sdk/compiler";
import type { ErgoTree } from "@fleet-sdk/core";
import { SByte, SColl, SLong } from "@fleet-sdk/serializer";

export const CONTRACT_SOURCE = readFileSync(new URL("../contracts/Gluon.es", import.meta.url), "utf8");

/** Values of the contract's compile-time constants (see the contract header). */
export interface GluonConstants {
  /** `_MinFee`: nanoERG kept in the reactor box on top of the reserve. */
  minFee: bigint;
  /** `_OracleFeePk`: ErgoTree (hex) of the oracle buyback contract, which receives the oracle fee. */
  oracleFeeTree: string;
  /** `_OraclePoolNFT`: NFT of the oracle pool box. */
  oraclePoolNft: string;
  /** `_OracleBuybackNFT`: NFT of the oracle buyback box. */
  oracleBuybackNft: string;
}

/**
 * ErgoTree version 0 with segregated constants (header 0x10), as in every Gluon reactor deployed so far.
 * Fleet defaults to version 1, which yields a different tree and address, so the version is explicit.
 */
export const COMPILER_OPTIONS = { version: 0, segregateConstants: true } as const;

export function namedConstants(c: GluonConstants) {
  return {
    _MinFee: SLong(c.minFee),
    _OracleFeePk: SColl(SByte, c.oracleFeeTree),
    _OraclePoolNFT: SColl(SByte, c.oraclePoolNft),
    _OracleBuybackNFT: SColl(SByte, c.oracleBuybackNft),
  };
}

export function compileGluon(constants: GluonConstants, network: "mainnet" | "testnet" = "mainnet"): ErgoTree {
  return compile(CONTRACT_SOURCE, { ...COMPILER_OPTIONS, network, map: namedConstants(constants) });
}
