# Gluon-Ergo-SDK: plan for the SDK 2.0.0, `@stabilitynexus/gluon-ergo-sdk` (contract v3 only)

*Plan for the maintainers of [StabilityNexus/Gluon-Ergo-SDK](https://github.com/StabilityNexus/Gluon-Ergo-SDK),
written on 2026-10-12 against its `main` at `782f461`. It is meant to be executed step by step by a developer or a
coding agent. A companion plan, `UI_PLAN.md`, builds the new
[Gluon-Ergo-UI](https://github.com/StabilityNexus/Gluon-Ergo-UI) on the SDK version this plan produces.
`UI_PLAN_ALTERNATIVE_WITHOUT_SDK.md` and `UI_PLAN_THIRD_WAY.md` are variants that do without a separate SDK repository,
and `comparison.md` compares the three routes.*

## 0. Scope, and how to use this plan

**Scope.** This plan creates the SDK 2.0.0 for the Gluon v3 reactor contract, published as a new npm package that the
maintainers own, `@stabilitynexus/gluon-ergo-sdk`, and nothing else.

- 2.0.0 supports **the v3 contract only**. It does not support the v2 contract or any other earlier reactor, and it
  keeps no backward compatibility with 1.x, in its API or in its behavior.
- **1.x is not touched.** The npm package `gluon-ergo-sdk`, versions 1.0.0 to 1.0.3, stays as published, and nothing
  in 1.x is fixed, changed or deprecated by this plan. Step 0 (§3) only preserves the 1.x source on a branch.
- **A new package.** 2.x is published as `@stabilitynexus/gluon-ergo-sdk`, in the npm organization `stabilitynexus`
  that the maintainers own (`NPM_GUIDE.md`). Its versions start at 2.0.0, continuing the 1.x line under the new name.
- Where this plan describes what 1.x does, it says what 2.0.0 replaces. It never says what to change in 1.x.
- **Ergo mainnet only.** The SDK serves no other network.
- **License: GPL-3.0.** The 2.0.0 package declares `"license": "GPL-3.0-only"` (PR 1), and the repository keeps its
  `License.md`.

**How to use this plan.**
- Do Step 0, then the pull requests of §6 in order. Each lists what to do and when it is done.
- **The contracts repository** below means
  [DjedAlliance/Gluon-Ergo-Backend-Contracts](https://github.com/DjedAlliance/Gluon-Ergo-Backend-Contracts), branch
  `main`. Its files are the reference for every rule the SDK must follow:

  | File in the contracts repository | What it gives you |
  |---|---|
  | `README.md`, section "The Contract" | Box layout, the six transaction types, input and output positions, fees |
  | `contracts/Gluon.es` | The v3 contract itself |
  | `lib/math.ts` | BigInt mirror of the v3 arithmetic, in the contract's order of operations |
  | `lib/reactor.ts` | Reading and writing the reactor's registers |
  | `lib/transactions.ts` | Fleet-based builders for all six transactions, including the oracle-fee and UI-fee rules |
  | `lib/node.ts` | Node client that copes with a stale blockchain index and with mempool races |
  | `lib/simulate.ts` | `consensusIssues()`: the consensus rules the mock chain does not check |
  | `test/helpers.ts`, `test/*.test.ts` | Mock-chain setup and 374 tests that run the real contract |
  | `test/fixtures/dort-buyback-box.json` | The live USD-oracle buyback box, for tests |
  | `deployments/mainnet.json`, `Deployments.md` | Every deployed reactor: ids, ErgoTree, constants, genesis registers |
  | `scripts/compile.ts` | Prints the ErgoTree of a deployment config (`node scripts/compile.ts config/dollar.json`) |

  The `lib/*.ts` files are tested against the real contract. They are the code to port (§5.1, D1).
- Line references to this repository are to `main` at `782f461`.

## 1. Summary

The Gluon reactor contract is now at v3. Two v3 reactors will be bootstrapped on Ergo mainnet from the contracts
repository: Gluon Gold and Gluon Dollar. The SDK 2.0.0, `@stabilitynexus/gluon-ergo-sdk`, is the SDK for them. It:

- serves several deployments, each described by data, with no deployment-specific constant in the code;
- implements all six v3 transactions: fission, fusion, beta decay in both directions, Adjust Peg and Update Treasury;
- builds its transactions with Fleet SDK, by porting the contracts repository's tested code;
- is tested by executing its transactions against the real contract on a simulated chain;
- runs in browsers and in Node.

## 2. Background

### 2.1 Deployments the SDK serves

| Deployment | Recorded when |
|---|---|
| `gluon-gold-v3` | the v3 Gold reactor is bootstrapped |
| `gluon-dollar-v3` | the v3 Dollar reactor is bootstrapped |

Each deployment gets a record in `deployments/mainnet.json` of the contracts repository when its bootstrap script
runs. Until then, the SDK is developed and tested against synthetic records and the simulated chain (§8).

`deployments/mainnet.json` also holds the records of reactors that are not v3: `gluon-gold-pre-v2`, `gluon-gold-v2`,
`gluon-dollar-pre-v3-a` and `gluon-dollar-pre-v3-b`. The SDK does not serve them. Its sync script (PR 2) drops every
record whose genesis R9 is not a `(Long, Long)` pair, which is what tells a v3 reactor from every earlier one.

### 2.2 The v3 contract in brief

All arithmetic is integer arithmetic on BigInt, with truncating division, in the contract's order of operations.
`ONE = 10⁹`. Appendix A has every formula; the contracts repository's README ("The Contract") has the full layout.

- **Price.** The target price of one neutron is `oracle R4 × peg factor / 10⁹`, with the peg factor in R9. The initial
  peg factor fixes the unit: 0.001 for the GoldPool, which quotes nanoERG per kg of gold, so that one neutron is one
  gram; 1 for the USD Pool, which quotes nanoERG per USD.
- **Healthy range.** Fission, fusion and beta decays need the fusion ratio `q` to be within [0.5, 0.98]. Beta decays
  must also leave it there. Prices use `qNorm = min(q / (q + 1 − 0.99), q)`.
- **Adjust Peg.** Anyone may submit it while `q` is outside the healthy range. It multiplies the peg factor by 99/100
  if `q > 0.98` and by 101/100 if `q < 0.5`, and changes nothing else.
- **Update Treasury.** The current treasury, a single-signature key in R5, replaces R5 with another key. The
  transaction must have a box of the treasury's address as `INPUTS(1)`.
- **Oracle.** Fission, fusion, beta decays and Adjust Peg require the oracle pool box as data input 0, holding
  `_OraclePoolNFT` as its first token and created fewer than 35 blocks before the block that includes the transaction.
- **Positions.** Every transaction spends the reactor as `INPUTS(0)` and recreates it as `OUTPUTS(0)`. It has the
  oracle pool box as data input 0 and at least two inputs: the contract reads data input 0 and `INPUTS(1)` in every
  transaction, whether or not the transaction uses them.
- **Fees.** Fission and fusion move a principal value of ERG; beta decays move the value of the protons or neutrons.
  Up to three fees are paid in separate outputs after `OUTPUTS(1)`, in this order, and a fee of zero has no output:
  oracle fee 0.1 % (beta decays only), dev fee 0.5 % decaying to zero, UI fee 0.4 % (only if the reactor input sets
  context variable 0 to a key). Each output receives exactly its fee plus `_MinFee`.

### 2.3 Oracle buyback top-ups

A beta decay that owes an oracle fee spends the oracle's buyback box as its **last input** and recreates it as
`OUTPUTS(2)` with the same script and first token and with the fee plus `_MinFee` added to its value. Oracle-core v2
buyback contracts, such as the USD oracle's DORT buyback (`dcce07af…`), also require:
- context variable 0 = `Int 1` (serialized `0402`) on the buyback input;
- R4 of the new box = `Coll[Byte]` holding the id of the box it replaces.

Without both, they reject the transaction. The contracts repository verified this against the live box
(`test/oracle-fee.test.ts`).

## 3. Step 0: preserve 1.x

Do this first, before any change for 2.0.0.

```sh
git clone https://github.com/StabilityNexus/Gluon-Ergo-SDK.git && cd Gluon-Ergo-SDK
git branch v1 db7a1e91e46f454ed07f0463b40549bcff8d76a1   # the commit npm records as the source of 1.0.3
git tag v1.0.3 db7a1e91e46f454ed07f0463b40549bcff8d76a1
git push origin v1 v1.0.3
```

- **Why `db7a1e9`.** The npm registry records `gitHead` for each published version: 1.0.0 and 1.0.1 at `9ff37de`,
  1.0.2 at `2288086`, 1.0.3 at `db7a1e9`. The repository has no tags.
- **`v1` is a frozen snapshot.** This plan adds no commit to it. Do not publish from it, change it or fix anything in it
  as part of this plan.
- One commit on `main` is in no release: `baccb5b` (2026-05-27, "correct GLUON_DOLLAR contract ErgoTree"). It changes two
  lines of `src/consts.ts`. It stays in `main`'s history and is not applied to `v1`.
- Optionally protect `v1` on GitHub (Settings → Branches): block force pushes and deletion.
- Do not unpublish, deprecate or re-tag any 1.x version on npm.

The 2.0.0 work happens on `main`, which becomes the 2.0 line (PR 1). The 1.x source stays available on `v1`.

## 4. Design requirements

2.0.0 is designed to meet these requirements. Each follows from the v3 contract or from how the chain behaves.

| # | Requirement | Why | Where |
|---|---|---|---|
| R1 | Reactor ErgoTree, token ids, oracle and buyback NFTs, `_MinFee`, buyback contract and initial peg factor come from a deployment record. Nothing deployment-specific is a module constant. | Several reactors, each with its own constants | D2, PR 2 |
| R2 | v3 arithmetic, in the contract's order of operations: peg-adjusted price, `qNorm`, φ₁ = 1, healthy range, Adjust Peg | The contract checks amounts to the unit | Appendix A, PR 3 |
| R3 | R9 is a `(Long, Long)` pair, written and read as such | v3 box layout | Appendix B, PR 3 |
| R4 | Amounts and fees are `bigint` end to end | A fee one nanoERG off makes the contract reject the transaction; JavaScript numbers lose precision above 2⁵³ | D4 |
| R5 | A fee output exists only if its fee is above zero, in the order oracle, dev, UI. The buyback box is spent only if the oracle fee is above zero. The dev fee goes to the treasury in R5. The UI fee sets context variable 0 on the reactor input. | Output positions are checked by index | §5.4, PR 5 |
| R6 | The buyback top-up of §2.3 | Oracle-core v2 buyback contracts | PR 5 |
| R7 | Box lookups are verified against the UTXO set and the mempool, and an ambiguous result is an error | The node's blockchain index can list spent boxes as unspent, and the oracle pool box can have competing successors in the mempool (both seen on 2026-10-11) | §5.4, PR 4 |
| R8 | Scripts see `HEIGHT = height + 1`; outputs get creation height `height` | Volume buckets, `lastBucketBlock` and the oracle-age check use `HEIGHT` | §5.4 |
| R9 | The reactor is `INPUTS(0)`, the buyback box is the last input, and only the inputs a transaction needs are spent | Positions are checked by index; a wallet's UTXOs should not all be consolidated | §5.4, PR 5 |
| R10 | Addresses are encoded and checked for mainnet; the treasury and UI-fee addresses are single-signature | A wallet can be switched to another network, and would then sign transactions the chain rejects | §5.4 |
| R11 | Health and oracle age are checked before a transaction is built, with errors a UI can show | The contract only answers "rejected" | §5.4, PR 6 |
| R12 | The tests execute the real contract | The contract is the specification | §8 |

## 5. Target design

### 5.1 Decisions

- **D1. Port the contracts repository's code to Fleet SDK, and use no ergo-lib-wasm.** `lib/math.ts`, `lib/reactor.ts`,
  `lib/transactions.ts` and the lookup rules of `lib/node.ts` implement every v3 rule, and the contracts repository
  tests them by executing transactions against the real contract. Fleet is plain TypeScript with BigInt amounts, so
  there is no wasm to initialize in browsers or in Node.
- **D2. Deployments are data.** Each deployment is a `GluonDeployment` object built from the contracts repository's
  records.
- **D3. One chain snapshot, then pure functions.** `fetchChainState()` reads the height, the reactor, the oracle and the
  buyback box once. Quotes, statistics and builders are pure functions of that snapshot, so what a user sees is what
  gets built.
- **D4. BigInt at the API boundary.** Amounts are `bigint` in base units: nanoERG for ERG, and the token's smallest unit
  for neutrons and protons. Formatting is the caller's job.
- **D5. 2.0.0 is a new SDK, not an upgrade.** It has no 1.x API, no compatibility layer and no support for v2 reactors.

### 5.2 Package layout

```
src/
  index.ts              public exports
  deployments/
    types.ts            GluonDeployment, TokenInfo
    fromRecord.ts       parses one record of deployments/mainnet.json
    data/mainnet.ts     generated by scripts/sync-deployments.ts: the v3 records, as a TypeScript module
    index.ts            DEPLOYMENTS, getDeployment()
  constants.ts          protocol constants
  math.ts               port of lib/math.ts
  reactor.ts            port of lib/reactor.ts
  oracle.ts             oracle price and age
  node.ts               node client (port of the lookup rules of lib/node.ts)
  select.ts             input selection
  transactions.ts       port of lib/transactions.ts
  client.ts             GluonClient
  errors.ts             GluonError and its codes
scripts/
  sync-deployments.ts
test/
  helpers.ts            port of the contracts repository's test/helpers.ts
  fixtures/             contract source and trees, record samples, the DORT buyback box
  *.test.ts
```

The generated `data/mainnet.ts` is a TypeScript module rather than JSON, so that the package needs no JSON import
attributes and works the same in every bundler and in Node.

### 5.3 Public API

`Box`, `Amount` and `EIP12UnsignedTransaction` are Fleet's types (`@fleet-sdk/common`).

```ts
// deployments
export interface TokenInfo { id: string; name: string; decimals: number; emission: bigint }
export interface GluonDeployment {
  name: string;                    // "gluon-gold-v3", "gluon-dollar-v3"
  status: "live" | "superseded" | "unused"; // informational
  peg: string;                     // "1 gram of gold", "1 USD"
  ergoTree: string;                // reactor contract
  address: string;
  tokens: { nft: TokenInfo; neutron: TokenInfo; proton: TokenInfo };  // token name = ticker (EIP-4 R4)
  minFee: bigint;                  // _MinFee
  oraclePoolNft: string;           // _OraclePoolNFT
  oracleBuybackNft: string;        // _OracleBuybackNFT
  oracleFeeTree: string;           // _OracleFeePk: ErgoTree of the buyback contract
  initialPegFactor: bigint;        // peg factor at genesis, ×10⁹: second element of the genesis R9
}
export const DEPLOYMENTS: readonly GluonDeployment[];   // every bundled v3 record
export function getDeployment(name: string): GluonDeployment;       // throws UNKNOWN_DEPLOYMENT
export function deploymentFromRecord(record: unknown): GluonDeployment; // throws INVALID_DEPLOYMENT

// client
export interface GluonClientOptions {
  deployment: GluonDeployment | string;  // a deployment, or the name of a bundled one
  nodeUrl: string;                 // HTTPS node serving the blockchain index API (/blockchain/…) and allowing browser requests
  uiFeeAddress?: string;           // single-signature address that receives the optional 0.4 % UI fee
  minerFee?: bigint;               // default 1_100_000n (Fleet's RECOMMENDED_MIN_FEE_VALUE)
  fetch?: typeof fetch;            // injectable, for tests
}

export interface ReactorState {    // the contents of a reactor box (Appendix B)
  value: bigint; neutrons: bigint; protons: bigint;       // value; tokens in the box
  totalNeutrons: bigint; totalProtons: bigint;            // R4
  treasury: string;                                       // R5: public key (hex)
  devFeeRepaid: bigint; maxDevFee: bigint;                // R6
  volumePlus: bigint[]; volumeMinus: bigint[];            // R7, R8
  lastBucketBlock: bigint; pegFactor: bigint;             // R9
}

export interface ChainState {
  height: number;                  // node height; transactions are built for HEIGHT = height + 1
  reactor: Box<bigint>;            // current reactor box, mempool-aware
  oracle: Box<bigint>;             // oracle pool box, mempool-aware
  buyback?: Box<bigint>;           // oracle buyback box, if one exists
  state: ReactorState;             // parsed reactor
}

export class GluonClient {
  readonly deployment: GluonDeployment;
  constructor(options: GluonClientOptions);
  fetchChainState(): Promise<ChainState>;

  snapshot(chain: ChainState): ProtocolSnapshot;
  quoteFission(chain: ChainState, erg: bigint): ActionQuote;
  quoteFusion(chain: ChainState, erg: bigint): ActionQuote;
  quoteBetaDecayPlus(chain: ChainState, protons: bigint): ActionQuote;
  quoteBetaDecayMinus(chain: ChainState, neutrons: bigint): ActionQuote;
  maxBetaDecayPlus(chain: ChainState): bigint;   // largest proton amount whose quote succeeds
  maxBetaDecayMinus(chain: ChainState): bigint;  // largest neutron amount whose quote succeeds
  quoteAdjustPeg(chain: ChainState): AdjustPegQuote;

  buildFission(chain: ChainState, args: { erg: bigint } & Funding): EIP12UnsignedTransaction;
  buildFusion(chain: ChainState, args: { erg: bigint } & Funding): EIP12UnsignedTransaction;
  buildBetaDecayPlus(chain: ChainState, args: { protons: bigint } & Funding): EIP12UnsignedTransaction;
  buildBetaDecayMinus(chain: ChainState, args: { neutrons: bigint } & Funding): EIP12UnsignedTransaction;
  buildAdjustPeg(chain: ChainState, args: Funding): EIP12UnsignedTransaction;
  buildUpdateTreasury(chain: ChainState, args: { newTreasuryAddress: string } & Funding): EIP12UnsignedTransaction;
}

export interface Funding {
  userBoxes: Box<Amount>[];        // the wallet's UTXOs (EIP-12 boxes from get_utxos())
  changeAddress: string;           // receives released tokens and change (EIP-12 get_change_address())
}

export interface ProtocolSnapshot {
  deployment: string;
  height: number;                  // node height; scripts see height + 1
  reserve: bigint;                 // box value − _MinFee, nanoERG: the total value locked
  neutronsInCirculation: bigint; protonsInCirculation: bigint;
  oraclePrice: bigint;             // R4 of the oracle box: nanoERG per oracle unit (per kg of gold, per USD)
  oracleAge: number;               // (height + 1) − the oracle box's creation height
  pegFactor: bigint;               // R9, ×10⁹
  initialPegFactor: bigint;
  pegDrift: number;                // pegFactor / initialPegFactor
  pegUnitPrice: bigint;            // nanoERG per peg unit as defined at launch: oraclePrice × initialPegFactor / 10⁹
                                   // (per gram of gold, per USD)
  targetPrice: bigint;             // nanoERG the contract targets per whole neutron: oraclePrice × pegFactor / 10⁹
  q: bigint;                       // fusion ratio, ×10⁹
  qNorm: bigint;                   // ratio the contract uses for prices, ×10⁹
  healthy: boolean;                // 0.5 ≤ q ≤ 0.98
  adjustPeg?: { direction: "down" | "up"; steps: number };  // present when unhealthy
  neutronPrice: bigint;            // nanoERG per whole neutron (10^decimals base units), at qNorm
  protonPrice: bigint;             // nanoERG per whole proton
  reserveRatio: number;            // percent: 100 / q
  protonLeverage?: number;         // q / (1 − q), while q < 0.99 (Appendix A)
  crashCushion: { untilDepeg: number; untilAdjustment: number }; // fractions, 0.25 = 25 % (Appendix A)
  volumes: { plus: bigint[]; minus: bigint[] }; // 14 buckets of 720 blocks, index 0 = the bucket that contains HEIGHT
  devFee: { repaid: bigint; max: bigint };
  treasuryAddress: string;
  betaDecaysAvailable: boolean;    // false when no buyback box exists
}

export interface ActionQuote {
  ergIn: bigint; ergOut: bigint;   // nanoERG into or out of the reactor, before fees
  neutronsIn: bigint; neutronsOut: bigint; protonsIn: bigint; protonsOut: bigint;
  principal: bigint;               // the value the fees are computed on, nanoERG
  fees: { dev: bigint; ui: bigint; oracle: bigint; outputsMinFee: bigint; miner: bigint; total: bigint };
                                   // outputsMinFee = _MinFee × number of fee outputs; total = the sum of all
  next: ReactorState;              // the reactor after the transaction
  qAfter: bigint;                  // fusion ratio after it, ×10⁹
}

export interface AdjustPegQuote {
  allowed: boolean;                // false when healthy
  direction?: "down" | "up";
  steps: number;                   // transactions needed to become healthy at the current oracle price
  nextPegFactor?: bigint;
}

// pure functions, for analytics over historical boxes (the UI's history and fee charts)
export { readReactor, reactorRegisters } from "./reactor.js";
export { oraclePrice, oracleAge } from "./oracle.js";
export * as math from "./math.js";  // snapshot, fees, fission, fusion, betaDecayPlus/Minus, adjustPeg, pegAdjustments, shiftVolumes

export class GluonError extends Error { readonly code: GluonErrorCode }
export type GluonErrorCode =
  | "REACTOR_UNHEALTHY"            // fission, fusion or beta decay while q is outside [0.5, 0.98]
  | "WOULD_LEAVE_HEALTHY_RANGE"    // a beta decay that would end outside the range
  | "REACTOR_HEALTHY"              // Adjust Peg while healthy
  | "ORACLE_STALE"                 // oracle box 35 or more blocks old at HEIGHT
  | "BOX_AMBIGUOUS"                // several unspent boxes hold the token (competing mempool updates): retry after the next block
  | "BOX_NOT_FOUND"
  | "BUYBACK_UNAVAILABLE"          // a beta decay owes an oracle fee but no buyback box exists
  | "AMOUNT_TOO_SMALL"             // the transaction would move no tokens
  | "AMOUNT_TOO_LARGE"             // the reactor cannot pay out or release that amount
  | "INSUFFICIENT_FUNDS"           // the wallet's boxes cannot cover the amounts and fees
  | "INVALID_ADDRESS"              // not a mainnet address, or not single-signature where that is required
  | "UNKNOWN_DEPLOYMENT"           // no bundled deployment with that name
  | "INVALID_DEPLOYMENT"           // a record that is malformed or not a v3 reactor
  | "NODE_ERROR";                  // the node did not answer, or answered with an error
```

A `ChainState` is a plain object, so callers can also build one from historical boxes.

### 5.4 Semantics the implementation must respect

- **Heights.** `ChainState.height` is the node's current height. Everything the contract evaluates with `HEIGHT` uses
  `height + 1`: bucket shifts, `lastBucketBlock` and the oracle-age check. Outputs get creation height `height`.
- **Lookups.** Find the reactor, oracle and buyback boxes as `lib/node.ts` does:
  1. Ask the blockchain index for unspent boxes holding the token, with `includeUnconfirmed=true&excludeMempoolSpent=true`.
  2. Keep only the boxes that `POST /utxo/withPool/byIds` also returns: this drops stale index entries.
  3. Drop boxes that `GET /transactions/unconfirmed/inputs/byBoxId/{id}` reports as spent in the mempool.
  4. Require the token to be the box's first token. For the reactor and the buyback, also require the deployment's
     ErgoTree (`ergoTree`, `oracleFeeTree`).
  5. Require exactly one box. Several mean competing mempool updates: throw `BOX_AMBIGUOUS`. None: `BOX_NOT_FOUND`
     (for the buyback box, leave `buyback` undefined instead).

  The user's own boxes come from the wallet, not from these lookups. The client runs in browsers, so step 2 is a
  cross-origin POST with a JSON body, which the browser preflights. Both public nodes tested on 2026-10-12
  (`node.sigmaspace.io`, `node.ergopool.io`) allow it (`access-control-allow-origin: *`, `content-type` allowed). The
  UI plan's GitHub Pages guide has a command to check any other node.
- **Inputs.** Select the user's inputs with the `selectInputs` function of `src/select.ts`, then add them to the
  transaction builder with `ensureInclusion`, in this fixed order: the reactor, the selected user boxes, the buyback box
  (last). Fleet places boxes it selects itself after the ones marked `ensureInclusion` (`BoxSelector.select` in
  `@fleet-sdk/core` 0.12), which would move the buyback box ahead of the user's boxes. `lib/transactions.ts` asserts
  both positions; keep those assertions.
  `selectInputs(userBoxes, { nanoErgs, tokens })`:
  - for each required token, take the boxes holding it, largest amount first, until it is covered;
  - then add boxes by largest value until the ERG requirement (outputs plus miner fee) is covered;
  - for Update Treasury, take boxes of the treasury's address first;
  - leave either no change at all, or change worth at least `SAFE_MIN_BOX_VALUE` (1 000 000 nanoERG, enough for a
    change box with a few dozen tokens), adding boxes until it is;
  - throw `INSUFFICIENT_FUNDS` if the requirements cannot be met.

  Fleet 0.12 cannot fix the change itself: with every input fixed by `ensureInclusion` it has no box to add, and it
  turns an ERG-only change below the minimum box value into an output that nodes reject (`consensusIssues()` reports it).
- **Fees.** Use `fees()` from `lib/math.ts` exactly. Create a fee output only when its fee is above zero. Spend the
  buyback box only when the oracle fee is above zero. Read the treasury from R5. Set the UI-fee variable only when
  `uiFeeAddress` is set.
- **Health.** Refuse fission, fusion and beta decays while unhealthy (`REACTOR_UNHEALTHY`). Refuse a beta decay whose
  `qAfter` is outside [0.5, 0.98] (`WOULD_LEAVE_HEALTHY_RANGE`). `maxBetaDecayPlus` and `maxBetaDecayMinus` search for
  the largest amount whose quote succeeds, within what the reactor can release (`AMOUNT_TOO_LARGE` otherwise).
- **Oracle age.** Throw `ORACLE_STALE` when `height + 1 − oracle.creationHeight` is 35 or more, or negative. This
  applies to every transaction except Update Treasury.
- **Mainnet only.** Encode and decode addresses for Ergo mainnet. Reject a UI-fee, treasury or change address that is
  not a mainnet address, and a UI-fee or treasury address that is not P2PK (`INVALID_ADDRESS`). The constructor
  validates `uiFeeAddress`.

## 6. Work plan

Do Step 0 (§3) first. Each of the following steps is one pull request. Run `npm run typecheck` and `npm test` before
merging each one.

### PR 1: Clean start and tooling

1. On `main`, delete the 1.x sources and files that only they need: everything in `src/`, `next.config.js`, `blog.md`,
   `.npmignore` and the old `README.md` text (rewritten in PR 7). They remain on `v1`. Keep `COPYRIGHT.md`,
   `License.md`, `.coderabbit.yaml` and `.gitignore`.
2. Replace `package.json`:
   - `"name": "@stabilitynexus/gluon-ergo-sdk"`, `"version": "2.0.0-next.0"`, and
     `"publishConfig": { "tag": "next", "access": "public" }`: a scoped package is private unless published with
     public access, and pre-releases must not take the `latest` tag (§7);
   - `"repository": { "type": "git", "url": "git+https://github.com/StabilityNexus/Gluon-Ergo-SDK.git" }`, which npm
     requires to match the repository that publishes;
   - `"license": "GPL-3.0-only"`, the SPDX identifier for GPL-3.0 (`GPL-3.0-or-later` is the alternative if the
     maintainers want the "or later" option);
   - `"type": "module"`, `"files": ["dist"]` and an `exports` map with `import` and `types` entries;
   - exact dependency versions (no `^`). Runtime: `@fleet-sdk/core` 0.12.0, `@fleet-sdk/serializer` 0.11.0,
     `@fleet-sdk/common` 0.10.0, `@fleet-sdk/crypto` 0.11.0 and `json-bigint` (it parses node responses without
     rounding amounts, in every browser). Dev: `@fleet-sdk/compiler` 0.12.0, `@fleet-sdk/mock-chain` 0.12.0,
     `vitest`, `typescript`.
3. `tsconfig.json`: `"module": "NodeNext"`, `"moduleResolution": "NodeNext"`, `strict`, `declaration`; write relative
   imports with `.js` extensions. Add `tsconfig.build.json` (target ES2020, `src` only) for the published build.
4. Scripts: `"build": "tsc -p tsconfig.build.json"`, `"test": "vitest run --testTimeout=30000"`,
   `"typecheck": "tsc --noEmit"`, `"sync-deployments": "node scripts/sync-deployments.ts"`. Running `.ts` scripts
   directly needs Node 24 (built-in type stripping): add `.nvmrc` with `24` and `"engines": { "node": ">=24" }` for
   development tooling only. The published `dist/` targets browsers and older Node versions.
5. CI: copy the contracts repository's `.github/workflows/ci.yml` (actions pinned by commit SHA) and add the build.

**Done when** CI runs green on a placeholder test.

### PR 2: Deployments as data

1. `scripts/sync-deployments.ts`:
   - downloads `deployments/mainnet.json` from the contracts repository at a ref given with `--ref` (default `main`);
   - keeps only the v3 records: those whose `genesis.registers.R9` decodes (Fleet serializer `parse`) to a pair;
   - writes them to `src/deployments/data/mainnet.ts` as `export default [...] as const`, adding a `source` field with
     the repository, the ref and the commit.

   Commit the output. While no v3 deployment is recorded, the list is empty.
2. `src/deployments/fromRecord.ts` maps a record to a `GluonDeployment`:

   | `GluonDeployment` field | Record field |
   |---|---|
   | `name`, `status`, `peg`, `ergoTree`, `address` | same names |
   | `tokens.{nft,neutron,proton}` | `tokens.*`, with `emission` as `BigInt` |
   | `minFee` | `BigInt(constants._MinFee)` |
   | `oraclePoolNft`, `oracleBuybackNft`, `oracleFeeTree` | `constants._OraclePoolNFT`, `constants._OracleBuybackNFT`, `constants._OracleFeePk` |
   | `initialPegFactor` | second element of the decoded `genesis.registers.R9` |

   Throw `INVALID_DEPLOYMENT` for a record whose R9 is not a pair, or that lacks a field.
3. `src/deployments/index.ts`: `DEPLOYMENTS` holds every record of the data file, and `getDeployment(name)` finds one by
   name or throws `UNKNOWN_DEPLOYMENT`.
4. Tests:
   - a synthetic v3 record (compile `test/fixtures/Gluon.es` with `@fleet-sdk/compiler`, as §8.2 describes, and fill
     in the other fields) parses to the expected `GluonDeployment`, with its initial peg factor;
   - a record whose R9 is a plain `Long` throws `INVALID_DEPLOYMENT`;
   - the sync filter, run on a copy of the current `deployments/mainnet.json` (vendored as
     `test/fixtures/mainnet-records.json`), drops all four of its records.

**Done when** the tests pass. When the contracts repository records a v3 deployment, rerunning the sync script adds it
with no code change.

### PR 3: Math and reactor state

1. Port `lib/math.ts` to `src/math.ts` as it is: constants, `snapshot`, `fees`, `fission`, `fusion`, `betaDecayPlus`,
   `betaDecayMinus`, `adjustPeg`, `pegAdjustments`, `shiftVolumes`, `genesisSupplies`. Add `crashCushion` and
   `protonLeverage` (Appendix A).
2. Port `lib/reactor.ts` to `src/reactor.ts`: `readReactor(box, deployment)` also checks the box's ErgoTree and the
   token ids against the deployment. The R5 check (a 33-byte key) stays.
3. Tests, with hand-computed expectations:
   - genesis supplies: q = 0.6 and a proton price of 1 ERG, for the Gold and Dollar parameters;
   - fission and fusion amounts, and the dev fee's linear decay;
   - bucket shifting at 0, 1, 13, 14 and 30 elapsed buckets;
   - a beta decay in each direction, including the volume fee;
   - Adjust Peg's 1 % steps, its rounding, and a chain of five steps from q = 1.03;
   - `crashCushion` and `protonLeverage`.

   The strongest check of the math is the contract itself: §8.2 runs the builders against it.

**Done when** every formula of Appendix A has a test.

### PR 4: Node client and lookups

1. `src/node.ts`: parse responses with `json-bigint` (`useNativeBigInt: true`); implement `height()` and
   `unspentByTokenId(tokenId)` with the lookup steps of §5.4; time out after 25 s or more. Failures throw
   `NODE_ERROR`.
2. `fetchChainState()` (in `client.ts`): height, reactor, oracle and buyback box (missing allowed), fetched in
   parallel, then `readReactor`.
3. Tests with an injected `fetch`:
   - a stale index entry is dropped;
   - a box spent in the mempool is dropped;
   - two live candidates give `BOX_AMBIGUOUS`;
   - a reactor box with the wrong ErgoTree is ignored;
   - a missing buyback box gives `buyback: undefined`, not an error;
   - an HTTP error gives `NODE_ERROR`.

**Done when** those cases pass.

### PR 5: Transaction builders

1. Port `lib/transactions.ts` to `src/transactions.ts`. Keep the output layout and fee order, the buyback top-up
   (`R4` = id of the spent box, variable 0 = `Int 1`), the UI-fee variable on the reactor input, the treasury read
   from R5 and the position assertions.
   Change: `recipient` is the caller's `changeAddress`; inputs come from `selectInputs` (§5.4) instead of being all the
   caller's boxes; outputs get creation height `chain.height`.
2. `src/select.ts`: `selectInputs` as specified in §5.4.
3. Every builder returns `EIP12UnsignedTransaction` (Fleet's `toEIP12Object()`), with full input and data-input boxes
   and the context extensions, which Nautilus needs to sign.
4. Update Treasury requires the first selected input to be a box of the current treasury's address, as the contract
   identifies the transaction by `INPUTS(1)`.

**Done when** the mock-chain tests of §8.2 pass for every builder.

### PR 6: GluonClient

1. `src/client.ts` implements the class of §5.3 on top of PRs 2 to 5.
2. `src/errors.ts`: `GluonError` with the codes of §5.3. Every refusal in §5.4 throws one of them, with a message a UI
   can show.
3. `snapshot()` fills every `ProtocolSnapshot` field (Appendix A). `volumes` are shifted to the current `HEIGHT` with
   `shiftVolumes(…, nDays, 0n)`, so a bucket that is more than a day old is no longer shown as today's.
   `betaDecaysAvailable` is false when `chain.buyback` is undefined.
4. Tests: one per error code, and `snapshot()` against hand-computed values.

### PR 7: Documentation and a first pre-release

1. Rewrite `README.md`: installation; choosing a deployment; the snapshot, quote and build flow; an EIP-12 signing
   example with Nautilus; error codes. State at the top that **2.0.0 supports Gluon v3 reactors only, and that
   reactors of earlier contract versions need the 1.x SDK** (`npm install gluon-ergo-sdk@1`, the old package). Link
   the contracts repository's README and `Deployments.md`.
2. Add `CHANGELOG.md`.
3. Set up publishing, as `NPM_GUIDE.md` describes: the npm organization, the first version (`2.0.0-next.0`) published
   by hand, the trusted publisher, the lock-down, and the GitHub environment `npm`. Add
   `.github/workflows/release.yml`, the release workflow of the guide's §7, for the tags `v*`.
4. Release `2.0.0-next.1`: set the version, commit, tag `v2.0.0-next.1` and push; the workflow publishes (§7).

### PR 8: After each v3 deployment

1. Run `npm run sync-deployments -- --ref <commit of the contracts repository that records the deployment>`.
2. Add a mock-chain test for the new deployment's exact ErgoTree.
3. Release the next pre-release (a tag). After the first mainnet v3 deployment is recorded and tested, release 2.0.0
   (§7).

## 7. Release plan

- **The package** is new: `@stabilitynexus/gluon-ergo-sdk`, owned by the maintainers' npm organization, `stabilitynexus`
  (`NPM_GUIDE.md`). `gluon-ergo-sdk` 1.x stays as it is.
- **Pre-releases** are `2.0.0-next.N`, published with the `next` dist-tag. `publishConfig.tag` in `package.json` makes
  `npm publish` do this by default.
- **Publication:** the first version, `2.0.0-next.0`, by hand, because npm can add a trusted publisher only to a
  package that exists; every later one by `.github/workflows/release.yml`, from a tag `v<version>`, through npm trusted
  publishing: with provenance, and with no npm token stored anywhere (`NPM_GUIDE.md`).
- **2.0.0** is released when the first mainnet v3 deployment is recorded in the SDK and its mock-chain test passes.
  Remove `publishConfig.tag` in that release's commit, so that 2.0.0 becomes `latest` (§10, Q1).
- The UI depends on an exact pre-release version until 2.0.0 exists.

## 8. Test plan

### 8.1 Unit tests

Math, register encoding, record parsing and the sync filter, node-client filters, input selection and error codes, as
listed in PRs 2 to 6.

### 8.2 Contract tests on a simulated chain

Port the contracts repository's `test/helpers.ts`. Each test:
1. creates a Fleet `MockChain` holding a reactor box, an oracle box and a buyback box;
2. builds a transaction with the SDK;
3. executes it on the mock chain, which runs the real contract through `sigmastate-js`;
4. checks the consensus rules the mock chain skips, with `consensusIssues()` ported from the contracts repository's
   `lib/simulate.ts`.

Contract trees to use:

| Tree | How to get it |
|---|---|
| v3, Dollar constants | Contracts repository: `node scripts/compile.ts config/dollar.json`. The constants are final, so this is the production tree. Save the hex as `test/fixtures/gluon-dollar-v3.ergotree`. |
| v3, test constants | Compile `contracts/Gluon.es` with `@fleet-sdk/compiler` (`version: 0`, `segregateConstants: true`) and the test constants of the contracts repository's `test/helpers.ts`. Vendor the source, at a pinned commit, as `test/fixtures/Gluon.es`. |

If the contract's source changes, re-vendor both: the mock-chain tests then show whether the builders still match.

Cases (both pegs: a per-kg Gold oracle with peg factor 0.001, a per-USD Dollar oracle with peg factor 1):
- each builder is accepted: fission, fusion, both beta decays (with and without an oracle fee), Adjust Peg in both
  directions, Update Treasury, and the UI fee;
- changing a token amount, a fee value or R9 by one unit is rejected;
- a beta decay against the real DORT buyback box (`test/fixtures/dort-buyback-box.json`, from the contracts repository)
  is accepted, and the same transaction without the buyback's R4 is rejected;
- positions: the reactor is the first input and the buyback box the last, and the fee outputs follow §2.2, for every
  combination of zero and non-zero fees;
- input selection: only the needed boxes are spent, the buyback box stays last, and no change box is below the
  minimum value (a wallet whose boxes would leave dust change gets one more box, or `INSUFFICIENT_FUNDS`).

### 8.3 Live, read-only checks

A script `scripts/smoke.ts <deployment>` that:
1. fetches the chain state;
2. prints the snapshot;
3. builds every allowed transaction with a fictitious funding box;
4. sends each to the node's `/transactions/check`.

Expect "missing input" errors only; any other rejection is a bug. The node rejects the fictitious input before it
evaluates any script, so this checks structure and serialization against live boxes, not the contract's rules (§8.2
covers those). The contracts repository's bootstrap dry run did this on 2026-10-11.

### 8.4 Live checks on the first mainnet deployment

The first mainnet bootstrap is also the first live test, and a small seed (at most 10 ERG, which the bootstrap script
enforces) keeps its cost low. Once it is recorded and bundled:
1. run `scripts/smoke.ts` (§8.3) against it;
2. run fission, fusion and both beta decays for real, with small amounts, from a wallet.

Adjust Peg can only be run live when a reactor actually leaves its healthy range, which takes an oracle price move of
tens of percent. Until then, the mock-chain tests of §8.2 are its test.

## 9. Coordination

- **Order of work.** The SDK can be built, tested and pre-released before any v3 reactor exists: its tests use the
  simulated chain and the Dollar tree, which is already final. The UI plan can start from any pre-release, with a
  fake client, and can run live once a pre-release bundles the first mainnet record.
- **Contracts repository, optional:** an export of golden test vectors from `lib/math.ts`.

## 10. Open questions (defaults in bold)

1. When is 2.0.0 released, and made `latest`? **When the first mainnet v3 deployment is recorded and bundled (§7).**
2. Bundle the deployment records in the package, or fetch them at runtime? **Bundle.** `deploymentFromRecord` serves
   anything else.
3. Should the SDK chain several Adjust Peg steps offline? **No:** one step per transaction, rebuilt from fresh
   mempool-aware state, as the contracts repository's `adjust-peg.ts --repeat` does.
4. Should `nodeUrl` have a default? **No:** the caller passes an HTTPS node explicitly.

## Appendix A: Formulas

Notation: integer division truncates; `ONE = 10⁹`; `R = value − _MinFee`; `Sn`, `Sp` = neutrons and protons in
circulation (total supply in R4 minus the amount in the box); `HEIGHT = height + 1`. Exactly as `lib/math.ts`.

```
P      = oraclePrice × pegFactor / ONE                    target price of one whole neutron, nanoERG
q      = Sn × P / R
qNorm  = min(ONE × q / (q + ONE − 990_000_000), q)
healthy = 500_000_000 ≤ q ≤ 980_000_000

Fission (M nanoERG in):   neutronsOut = M × Sn × (ONE − 1_000_000) / R / ONE      (protons: the same with Sp)
Fusion (M nanoERG out):   neutronsIn  = M × Sn × ONE / (R × (ONE − 5_000_000))   (protons: the same with Sp)

valueOfProtons(a)  = a × ((ONE − qNorm) × R / Sp) / ONE
valueOfNeutrons(a) = a × (qNorm × R / Sn) / ONE

fees(principal):
  oracle = principal × 1 / 1000                              beta decays only
  dev    = (5 × principal / 1000) × (max − repaid) / max     if repaid < max, else 0     (R6 = (repaid, max))
  ui     = 4 × principal / 1000                              if a UI key is set
principal = M for fission and fusion; valueOfProtons(M) for beta decay +; valueOfNeutrons(M) for beta decay −
All four actions: devFeeRepaid' = devFeeRepaid + dev.

nDays = min((HEIGHT − lastBucketBlock) / 720, 14)
shift(v, n, x) = n = 0 ? [v0 + x, v1 … v13] : [x, 0 × (n − 1), v0 … v(13 − n)]
Beta decay + (M protons in):
  plus' = shift(R7, nDays, valueOfProtons(M)); minus' = shift(R8, nDays, 0)
  volume = max(0, Σplus' − Σminus');  φβ = 5_000_000 + ONE × volume / R
  neutronsOut = (M × (ONE − φβ) / Sp) × ((ONE − qNorm) × Sn / ONE) / qNorm
Beta decay − (M neutrons in):
  minus' = shift(R8, nDays, valueOfNeutrons(M)); plus' = shift(R7, nDays, 0)
  volume = max(0, Σminus' − Σplus')
  protonsOut = (M × (ONE − φβ) / Sn) × (qNorm × Sp / ONE) / (ONE − qNorm)
Both beta decays: lastBucketBlock' = HEIGHT / 720 × 720;  qAfter = Sn' × P / R must be healthy.

Adjust Peg: q > 980_000_000 → pegFactor × 99 / 100;  q < 500_000_000 → pegFactor × 101 / 100.
```

**Derived values** for `ProtocolSnapshot`:

```
pegUnitPrice  = oraclePrice × initialPegFactor / ONE
neutronPrice  = valueOfNeutrons(10^neutron.decimals)        per whole token
protonPrice   = valueOfProtons(10^proton.decimals)
reserveRatio  = 100 / q                                      percent, with q as a fraction
crashCushion.untilDepeg      = max(0, 1 − q / 0.99)          fraction by which ERG can fall against the peg before
                                                             neutrons are worth less than the peg
crashCushion.untilAdjustment = max(0, 1 − q / 0.98)          fall before the reactor leaves its healthy range
protonLeverage = q / (1 − q)                                 while q < 0.99
```

**Why `protonLeverage = q / (1 − q)`.** Let `e` be the price of ERG in the pegged asset and `N` the neutron supply's
value in that asset, so that `q = N / (R·e)`. Measured in ERG, a proton is worth `(R − N/e) / Sp = R·(1 − q) / Sp`. The
elasticity of that value to `e` is `(N/e) / (R − N/e) = q / (1 − q)`. At `q = 0.6` a proton's price in ERG rises 1.5 %
for each 1 % that ERG rises against the pegged asset. This is the figure the legacy UI shows. Within the healthy range
`qNorm = q`, so the formula holds there and up to `q = 0.99`.

## Appendix B: Reactor box encoding

| | Type | Meaning |
|---|---|---|
| Value | | reserve + `_MinFee` |
| Token 0 / 1 / 2 | | reactor NFT (1) / neutrons not in circulation / protons not in circulation |
| R4 | `(Long, Long)` | total supply of neutrons, of protons |
| R5 | `SigmaProp` | treasury key: serialized `08cd` + a 33-byte public key |
| R6 | `(Long, Long)` | dev fees repaid, maximum (nanoERG) |
| R7 / R8 | `Coll[Long]` | beta decay + / − value per bucket: 14 buckets of 720 blocks |
| R9 | `(Long, Long)` | first block of the current bucket; peg factor ×10⁹ |

Serialized type bytes: `Long` = `05`, `(Long, Long)` = `59`, `Coll[Long]` = `11`, `SigmaProp` = `08`. The context variable for
the oracle-core top-up is `{ "0": "0402" }`. The UI-fee variable is `{ "0": SSigmaProp(SGroupElement(key)) }` on the
reactor input.
