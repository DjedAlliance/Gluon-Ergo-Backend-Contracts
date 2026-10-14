import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { compileGluon } from "../lib/compile.ts";
import {
  type DeploymentConfig,
  bootstrapParameters,
  contractConstants,
  formatUnits,
  isTodo,
  parseDecimal,
  readDeployment,
  readNetworks,
  todoFields,
  validateDeployment,
} from "../lib/config.ts";

const gold = readDeployment(new URL("../config/gold.json", import.meta.url));
const dollar = readDeployment(new URL("../config/dollar.json", import.meta.url));
const dortBuyback = JSON.parse(readFileSync(new URL("fixtures/dort-buyback-box.json", import.meta.url), "utf8"));
const tokenTodos = ["nft", "neutron", "proton"].flatMap((t) => [`tokens.${t}.ticker`, `tokens.${t}.name`, `tokens.${t}.description`]);

const MAINNET_P2PK = "9huM5LVa7b8uAUGgzDGDCHUdRe4L1j4SQ6kHXC1JVVbL1RSNCmd";
const TESTNET_P2PK = "3WyCSytuGL8xYFprU4jgDpBLK6NHdejaPwQG8Qex66dMMiT6xfra";

describe("Shipped deployment configs", () => {
  it("are well formed and have no invalid values", () => {
    expect(validateDeployment(gold)).toEqual([]);
    expect(validateDeployment(dollar)).toEqual([]);
  });

  it("leave open exactly the values still to be decided", () => {
    expect(todoFields(gold)).toEqual([
      "oracle.buybackNft",
      "oracle.buybackContractAddress",
      "treasuryAddress",
      "bootstrap.seedErg",
      ...tokenTodos,
    ]);
    expect(todoFields(dollar)).toEqual(["treasuryAddress", "bootstrap.seedErg", ...tokenTodos]);
  });

  it("peg Gold to one gram (peg factor 0.001 on a per-kg oracle) and Dollar to one USD", () => {
    expect(parseDecimal(gold.bootstrap.initialPegFactor)).toBe(1_000_000n);
    expect(parseDecimal(dollar.bootstrap.initialPegFactor)).toBe(1_000_000_000n);
  });

  it("point the Dollar deployment at the live DORT buyback contract", () => {
    const constants = contractConstants(dollar);
    expect(constants.oracleFeeTree).toBe(dortBuyback.ergoTree);
    expect(constants.oracleBuybackNft).toBe(dortBuyback.assets[0].tokenId);
    expect(compileGluon(constants).toHex().slice(0, 2)).toBe("10");
  });

  it("refuse to compile Gold until its oracle buyback is decided (D10)", () => {
    expect(() => contractConstants(gold)).toThrow(/oracle\.buybackNft, oracle\.buybackContractAddress/);
  });

  it("refuse to bootstrap while the seed is TODO", () => {
    expect(() => bootstrapParameters(dollar)).toThrow(/bootstrap\.seedErg/);
  });

  it("have a usable mainnet node", () => {
    expect(isTodo(readNetworks().mainnet.nodeUrl)).toBe(false);
  });
});

describe("Config validation", () => {
  const withChange = (change: (c: DeploymentConfig) => void) => {
    const c = structuredClone(dollar);
    change(c);
    return validateDeployment(c);
  };

  it.each([
    ["a seed above 10 ERG (D11)", (c: DeploymentConfig) => void (c.bootstrap.seedErg = "10.000000001"), /seedErg: must be at most 10/],
    ["a seed of zero", (c: DeploymentConfig) => void (c.bootstrap.seedErg = "0"), /seedErg: must be greater than 0/],
    ["an initial fusion ratio outside the healthy range", (c: DeploymentConfig) => void (c.bootstrap.initialFusionRatio = "0.99"), /healthy range/],
    ["a peg factor with more than 9 decimals", (c: DeploymentConfig) => void (c.bootstrap.initialPegFactor = "0.0000000001"), /at most 9 decimals/],
    ["a treasury that is not a single-signature address (N5)", (c: DeploymentConfig) => void (c.treasuryAddress = c.oracle.buybackContractAddress), /single-signature/],
    ["a treasury on another network", (c: DeploymentConfig) => void (c.treasuryAddress = TESTNET_P2PK), /not a mainnet address/],
    ["a network other than mainnet", (c: DeploymentConfig) => void Object.assign(c, { network: "testnet" }), /network: must be "mainnet"/],
    ["a buyback NFT that is not 32 bytes of hex", (c: DeploymentConfig) => void (c.oracle.buybackNft = "dcce07af"), /32 bytes/],
    ["a ticker with spaces", (c: DeploymentConfig) => void (c.tokens.neutron.ticker = "Gluon Dollar"), /without spaces/],
    ["an emission that overflows a Long", (c: DeploymentConfig) => void (c.bootstrap.tokenEmission = "10000000000"), /tokenEmission: must be at most/],
  ])("rejects %s", (_, change, problem) => {
    expect(withChange(change).join("\n")).toMatch(problem);
  });

  it("accepts a complete config", () => {
    const problems = withChange((c) => {
      c.treasuryAddress = MAINNET_P2PK;
      c.bootstrap.seedErg = "10";
      for (const t of [c.tokens.nft, c.tokens.neutron, c.tokens.proton]) {
        t.ticker = "TEST";
        t.name = "Test";
        t.description = "Test token.";
      }
    });
    expect(problems).toEqual([]);
  });
});

describe("Decimals", () => {
  it.each([
    ["0.001", 1_000_000n],
    ["1", 1_000_000_000n],
    ["10", 10_000_000_000n],
    ["0.6", 600_000_000n],
  ])("parses %s", (text, value) => {
    expect(parseDecimal(text)).toBe(value);
    expect(formatUnits(value)).toBe(text);
  });
});
