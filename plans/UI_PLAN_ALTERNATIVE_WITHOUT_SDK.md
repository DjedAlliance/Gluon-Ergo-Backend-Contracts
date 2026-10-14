# Gluon-Ergo-UI: alternative plan without the SDK (v3-only static UI on GitHub Pages, with its own Fleet library)

*Plan for the maintainers of [StabilityNexus/Gluon-Ergo-UI](https://github.com/StabilityNexus/Gluon-Ergo-UI), written
on 2026-10-12 against its `main` at `e4e30ab`. It is meant to be executed step by step by a developer or a coding
agent. It is an alternative to the pair `SDK_PLAN.md` + `UI_PLAN.md` and to `UI_PLAN_THIRD_WAY.md`, and it does not
depend on any of them: it is self-contained. `comparison.md` discusses the pros and cons of the three routes.*

## 0. Scope, and how to use this plan

**Scope.** This plan abandons the SDK. The new UI implements **its own library of functions** to read data from the
blockchain and create transactions, using Fleet SDK, inside the UI repository. The UI is a **static, client-side site
hosted on GitHub Pages**, for the Gluon **v3** contract, and Gluon Gold v3 and Gluon Dollar v3 each get their own
deployment, in their own fork of the repository (§3). The two forks differ only in their env/config files.

- **No SDK work at all.** The `StabilityNexus/Gluon-Ergo-SDK` repository and the npm package `gluon-ergo-sdk` are left
  exactly as they are: no 2.0.0, no change to 1.x. The new UI does not use the package.
- The new version knows **v3 deployments only**, on **Ergo mainnet only**. It has no v2 code path, no support for SDK
  1.x and no testnet support.
- **The legacy UI is the `mainnet` branch.** It is preserved, not changed, and keeps being deployed to Vercel by its
  existing project. Nothing in this plan fixes or improves it.
- **No server code, no secrets and no price feed.** The CoinGecko-based price, and everything that depends on it, is
  removed (§5.1, UD9).
- **No UI fee.** The sites charge no interface fee (§5.1, UD13).
- Where this plan describes what the current code does, it says what the new version replaces. It never says what to
  change in the legacy UI.

**How to use this plan.**
- Do Step 0, then the pull requests of §6 in order. They go into `main` of the upstream repository. Each lists what to
  do and when it is done. §6, PR 10 creates the two forks.
- **The contracts repository** below means
  [DjedAlliance/Gluon-Ergo-Backend-Contracts](https://github.com/DjedAlliance/Gluon-Ergo-Backend-Contracts), branch
  `main`. Its files are the reference for every rule the library must follow, and the code to port:

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

  The `lib/*.ts` files are tested against the real contract. They are the code the library ports (UD10).
- **Prerequisite for running live:** the first mainnet bootstrap of the contracts repository, which records a
  deployment. Until then the library and the UI are built and tested against synthetic records, the simulated chain and
  a fake client.
- The current code, tagged `pre-v3` in Step 0, is the starting point for porting UI files (§6).
- Line references are to `main` at `e4e30ab`.

## 1. Summary

New Gluon reactors run contract v3: Gluon Gold, pegged to one gram of gold, and Gluon Dollar, pegged to one US dollar.
v3 adds two things users will see:
- a **healthy range** for the fusion ratio: outside it, fission, fusion and transmutations are refused;
- **Adjust Peg**, a transaction anyone can submit while the reactor is outside its healthy range. It moves the peg
  factor by 1 % per transaction until the reactor is healthy again, and the neutron's target price is the oracle price
  times the current peg factor.

The new UI:
- contains its own library, `src/lib/gluon`, which reads the chain and builds all six v3 transactions with Fleet SDK,
  ported from the contracts repository's tested code and tested by executing its transactions against the real
  contract on a simulated chain;
- takes every id, decimal, ticker and unit from the deployment each site serves;
- shows the reactor's health, gates trading accordingly, and offers Adjust Peg;
- is a static site that reads the chain from the visitor's browser, so GitHub Pages can host it;
- is published from two forks of the repository, one per deployment, that differ only in one env file.

## 2. Step 0: the legacy UI is `mainnet`

Do this first, before any change for v3.

**Facts** (checked on 2026-10-12):
- `mainnet` is `98e902d9697a681e115d3b0534d3c80923ef12b9` (2026-03-01). The README says Vercel deploys it. It vendors
  the SDK in `lib/gluon-gold-sdk` (a `file:` dependency) and is 52 commits behind `main`.
- `main` is `e4e30abc753372a6e438bcc98f1e476661986bc0` (2026-09-03). It depends on npm `gluon-ergo-sdk ^1.0.2`, locked
  at 1.0.3.

**Decision.** `mainnet` is the legacy branch. No new branch is created for the legacy UI. `main` becomes the v3 line.

- **`mainnet` stays as it is**, deployed to Vercel by the existing project. Optionally protect it on GitHub (Settings →
  Branches): block force pushes and deletion. Never merge `main` into it.
- **Check the existing Vercel project(s)** (Settings → Environments → Production → Branch Tracking; older dashboards:
  Settings → Git → Production Branch). Each must track `mainnet`. If one tracks `main`, switch it to `mainnet` before
  PR 1 lands, or the first v3 commit would be deployed over the legacy site.
- **Tag the last commit of the old design on `main`**, so that porting has a stable reference:

  ```sh
  git clone https://github.com/StabilityNexus/Gluon-Ergo-UI.git && cd Gluon-Ergo-UI
  git tag pre-v3 e4e30abc753372a6e438bcc98f1e476661986bc0
  git push origin pre-v3
  ```

  Where this plan says **port** a UI file, take it from `pre-v3` (`git show pre-v3:<path>`): it is the newest version
  of the same design as `mainnet`.
- If GitHub Pages is enabled on the upstream repository, disable it (Settings → Pages). Upstream does not deploy.
- **The SDK repository needs no step.** It stays as it is.

## 3. The two deployments

| Deployment | Serves | Repository (a fork of upstream) | Site |
|---|---|---|---|
| Gold | `gluon-gold-v3` | `StabilityNexus/Gluon-Ergo-UI-Gold` | https://gold.gluon.stability.nexus/ |
| Dollar | `gluon-dollar-v3` | `StabilityNexus/Gluon-Ergo-UI-Dollar` | https://dollar.gluon.stability.nexus/ |

(Without a custom domain, a site would be at `https://stabilitynexus.github.io/<repository>/` and would need
`NEXT_PUBLIC_BASE_PATH`; the build supports that for previews and as a fallback.)

GitHub Pages publishes one site per repository, so each deployment has a repository of its own. They work like this:

- **Upstream** is `StabilityNexus/Gluon-Ergo-UI`. It holds the shared code, including the library, on `main`, and the
  legacy UI on `mainnet`. It has no deployment configuration and does not publish a site.
- **Each fork** is a fork of upstream. It has the same code as upstream `main`, plus **one file of its own: the
  committed `.env.production`**. That file, and the repository's Pages settings, are the only differences between the
  Gold fork and the Dollar fork. Changes to the code are made in upstream and merged into both forks (§8.8).
- **One build serves exactly one deployment**, chosen by `NEXT_PUBLIC_GLUON_DEPLOYMENT`. There is no selector and no
  code for several deployments in one site.
- A later v3 reactor (another peg) is added by: a record in the library's deployment data
  (`node scripts/sync-deployments.ts`), an entry in the UI's presentation config (§5.5), and a new fork (§8.3).

## 4. Starting point: what is reused and what is replaced

The new version keeps the current app's layout, components and charts. What changes is the data layer, which assumes
the v2 Gold reactor throughout, and the hosting model. This table says what the current code does and what replaces
it; none of it is a fix list for the legacy UI.

| Area | Current code (`main`) | New version |
|---|---|---|
| Chain logic | the npm package `gluon-ergo-sdk` 1.x; `new sdk.Gluon()` in 7 places, each with the SDK's built-in reactor | the library `src/lib/gluon` (§5.2), with one `GluonClient` in `GluonProvider` |
| Reactor and token ids | hard-coded in `lib/constants/token.ts` and 6 other files | `deployment.tokens` |
| Units and labels | oracle price ÷ 1000, a `0.66` cushion, "GAU"/"GAUC" literals | `ProtocolSnapshot` fields; tickers from the token names |
| Amounts | JavaScript numbers (`Number(...)`) | `bigint`, with `amounts.ts` |
| Wallet UTXOs | all passed to the SDK | the library selects what it needs |
| Environment | `NEXT_PUBLIC_DEPLOYMENT` (a network), two node variables, about 25 presentation variables, a server-side API key | §5.4: a few `NEXT_PUBLIC_*` variables in `.env.production`; presentation in code; no secrets |
| USD price of ERG | `/api/getErgPrice` (CoinGecko, with an API key), used by `MyStats`, `GluonStats` and `FeeBreakdownPanel` | none: removed (UD9) |
| Server code | API routes in `src/pages/api/`, and a CSP set by `headers()` in `next.config.ts` | none: a static export (§5.3) |
| History and fee charts | assume a `Long` R9, two contract addresses and only four kinds of transaction | v3 records; Adjust Peg and Update Treasury transitions |
| wasm | `ergo-lib-wasm-*`, used by `NodeService.getCtx` | none |
| Hosting | `vercel.json` (npm), and a GitHub starter workflow for Pages that looks for `bun.lockb` while the repository has `bun.lock` | the workflows of §8.4 |
| Site identity | hard-coded `https://gluon.fi` (canonical link) and `https://www.gluon.gold/` (SEO tags) | `NEXT_PUBLIC_SITE_URL` |
| Tests | none | Vitest: library tests (unit, and contract tests on a simulated chain) and UI tests |

**Reused as it is:** the layout, navigation and theme machinery, UI primitives and icons, `ErgoProvider`, the wallet
connector, the home page, the `resolveAssetPath` helper, IndexedDB and transaction-listener utilities, and the
presentation code of the charts.

## 5. Target design

### 5.1 Decisions

- **UD1. One build, one deployment.** `NEXT_PUBLIC_GLUON_DEPLOYMENT` names a deployment in the library's data. Nothing
  in the UI chooses between deployments.
- **UD2. One library client, in a React context.** `GluonProvider` owns the client and the latest chain state and
  snapshot. It polls every 30 s while the tab is visible, refreshes on demand and after every submitted transaction, and
  keeps the last good state while a refresh fails. Components read it with `useGluon()`; nothing else instantiates the
  client.
- **UD3. Everything on-chain comes from the deployment:** token ids, decimals, tickers (the token's EIP-4 name),
  contract and oracle. Presentation (colors, labels) is in code, keyed by deployment name (§5.5).
- **UD4. The library computes; the UI formats.** Quotes and transactions come from `client.quote*` and `client.build*`.
  Amounts stay `bigint` in base units until display.
- **UD5. The UI never offers what the contract would reject:** health, oracle age and the buyback box gate every action.
- **UD6. Static and client-side only.** The site is exported to plain files (`output: "export"`) and published on
  GitHub Pages. All data comes from the visitor's browser: the Ergo node and the explorer API, both of which allow
  browser requests. There is no server code and there are no secrets.
- **UD7. Fail fast.** The build validates the configuration, so a fork with a missing or wrong `.env.production` fails
  its build instead of publishing the wrong reactor.
- **UD8. The wallet must be on mainnet.**
- **UD9. No external price feed.** The site shows no USD value of ERG, and nothing depends on CoinGecko or its API key.
  Removed: `pages/api/getErgPrice.ts`, the `COINGECKO_API_KEY` variable, the `MyStats` portfolio component, the ERG
  price card and its polling in `GluonStats`, and the USD amounts of the fee panel. Prices stay in ERG and in the
  pegged asset, from the oracle.
- **UD10. Port the contracts repository's code to Fleet SDK, and use no ergo-lib-wasm.** `lib/math.ts`,
  `lib/reactor.ts`, `lib/transactions.ts` and the lookup rules of `lib/node.ts` implement every v3 rule, and the
  contracts repository tests them by executing transactions against the real contract. Fleet is plain TypeScript with
  BigInt amounts, so there is no wasm to initialize in the browser.
- **UD11. The library is built as if it were a package.** It lives in one folder, `src/lib/gluon/`, imports nothing from
  the rest of the UI (§5.2), and has its own tests. The UI is its only user today; the boundary keeps it extractable
  into a package later, as a `git mv`, if a second user appears.
- **UD12. Vitest is the one test runner.** The library's contract tests run the real contract through
  `sigmastate-js`, which the contracts repository runs under Node with Vitest. One runner serves the library and the UI.
- **UD13. No UI fee.** The contract allows an optional 0.4 % fee for the interface that builds a transaction. These
  sites charge none: the UI creates its library client without `uiFeeAddress`, so quotes have `fees.ui = 0` and no
  transaction has an interface-fee output. There is no fee address to configure. The library keeps the option, because
  it is part of the contract's transactions and of the code it ports; the UI does not use it.

### 5.2 The library (`src/lib/gluon`)

#### Design requirements

Each follows from the v3 contract or from how the chain behaves.

| # | Requirement | Why |
|---|---|---|
| R1 | Reactor ErgoTree, token ids, oracle and buyback NFTs, `_MinFee`, buyback contract and initial peg factor come from a deployment record. Nothing deployment-specific is a module constant. | Several reactors, each with its own constants |
| R2 | v3 arithmetic, in the contract's order of operations: peg-adjusted price, `qNorm`, φ₁ = 1, healthy range, Adjust Peg (Appendix D) | The contract checks amounts to the unit |
| R3 | R9 is a `(Long, Long)` pair, written and read as such (Appendix E) | v3 box layout |
| R4 | Amounts and fees are `bigint` end to end | A fee one nanoERG off makes the contract reject the transaction; JavaScript numbers lose precision above 2⁵³ |
| R5 | A fee output exists only if its fee is above zero, in the order oracle, dev, UI. The buyback box is spent only if the oracle fee is above zero. The dev fee goes to the treasury in R5. The UI fee sets context variable 0 on the reactor input. | Output positions are checked by index |
| R6 | The buyback top-up of §5.2 "Oracle buyback top-ups" | Oracle-core v2 buyback contracts |
| R7 | Box lookups are verified against the UTXO set and the mempool, and an ambiguous result is an error | The node's blockchain index can list spent boxes as unspent, and the oracle pool box can have competing successors in the mempool (both seen on 2026-10-11) |
| R8 | Scripts see `HEIGHT = height + 1`; outputs get creation height `height` | Volume buckets, `lastBucketBlock` and the oracle-age check use `HEIGHT` |
| R9 | The reactor is `INPUTS(0)`, the buyback box is the last input, and only the inputs a transaction needs are spent | Positions are checked by index; a wallet's UTXOs should not all be consolidated |
| R10 | Addresses are encoded and checked for mainnet; the treasury and UI-fee addresses are single-signature | A wallet can be switched to another network, and would then sign transactions the chain rejects |
| R11 | Health and oracle age are checked before a transaction is built, with errors a UI can show | The contract only answers "rejected" |
| R12 | The tests execute the real contract | The contract is the specification |

#### The v3 contract in brief

All arithmetic is integer arithmetic on BigInt, with truncating division, in the contract's order of operations.
`ONE = 10⁹`. Appendix D has every formula; the contracts repository's README ("The Contract") has the full layout.

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

#### Oracle buyback top-ups

A beta decay that owes an oracle fee spends the oracle's buyback box as its **last input** and recreates it as
`OUTPUTS(2)` with the same script and first token and with the fee plus `_MinFee` added to its value. Oracle-core v2
buyback contracts, such as the USD oracle's DORT buyback (`dcce07af…`), also require:
- context variable 0 = `Int 1` (serialized `0402`) on the buyback input;
- R4 of the new box = `Coll[Byte]` holding the id of the box it replaces.

Without both, they reject the transaction. The contracts repository verified this against the live box
(`test/oracle-fee.test.ts`).

#### Layout and boundary

```
src/lib/gluon/
  index.ts              the library's public exports
  types.ts              GluonDeployment, TokenInfo, ReactorState, ChainState, ProtocolSnapshot, ActionQuote, …
  errors.ts             GluonError and its codes
  deployments/
    fromRecord.ts       parses one record of deployments/mainnet.json
    data.ts             generated by scripts/sync-deployments.ts: the v3 records, as a TypeScript module
    index.ts            DEPLOYMENTS, getDeployment()
  math.ts               port of lib/math.ts
  reactor.ts            port of lib/reactor.ts
  oracle.ts             oracle price and age
  node.ts               node client (port of the lookup rules of lib/node.ts)
  select.ts             input selection
  transactions.ts       port of lib/transactions.ts
  client.ts             GluonClient
  __tests__/            Vitest tests: unit tests, and contract tests on a simulated chain
    helpers.ts          port of the contracts repository's test/helpers.ts
    fixtures/           contract source and trees, record samples, the DORT buyback box
scripts/
  sync-deployments.ts
  smoke.ts
```

**The boundary rule (UD11).** Files in `src/lib/gluon/` import only from `@fleet-sdk/*`, `json-bigint` and each other.
They never import from the rest of `src`, from React or from Next.js, and they read no `process.env`: everything
(node URL, UI-fee address, `fetch`) arrives as an option. Enforce it in `eslint.config.mjs`:

```js
{
  files: ["src/lib/gluon/**/*.ts"],
  ignores: ["src/lib/gluon/__tests__/**"],
  rules: {
    "no-restricted-imports": ["error", {
      patterns: [{ group: ["@/*", "../../*", "react", "react-dom", "next", "next/*"], message: "src/lib/gluon must not depend on the rest of the UI" }],
    }],
  },
},
```

**`CODEOWNERS` is enforced** (the maintainers decided so). PR 1 adds `.github/CODEOWNERS`:

```
# Code that must match the contract, and the files that guard or ship it: every change needs an owner's review.
/src/lib/gluon/          @<owner-1> @<owner-2>
/scripts/                @<owner-1> @<owner-2>
/eslint.config.mjs       @<owner-1> @<owner-2>
/.github/                @<owner-1> @<owner-2>
```

Replace the placeholders with the GitHub handles of the maintainers who know the contract: at least two, so that an
owner's own change can be approved by the other. GitHub only honors an owner who has write access to the repository (a
team must be visible and have write access). Once PR 1 is merged, a repository admin protects `main` of upstream
(Settings → Branches, or Settings → Rules → Rulesets):
- require a pull request before merging, with at least one approval and **Require review from Code Owners**;
- require the `check` job of `ci.yml` to pass;
- block force pushes and deletion;
- do not allow bypassing these rules, so that administrators are bound too.

With these on, no UI pull request can change fee or transaction code, the boundary rule or the workflows without an
owner's approval. The rules are repository settings, not code: check them again after any change of the repository's
settings or ownership.

**Node must be able to run the library**, because `scripts/smoke.ts` and `scripts/sync-deployments.ts` import it
directly. So, inside `src/lib/gluon/`: relative imports carry the `.ts` extension (`import { fees } from "./math.ts"`),
and no TypeScript-only runtime syntax is used (no enums, parameter properties or namespaces), which is what Node 24's
type stripping supports. `tsconfig.json` sets `"allowImportingTsExtensions": true` (it requires `noEmit`, which Next's
tsconfig has) and `"erasableSyntaxOnly": true`, which makes the compiler enforce the second rule.

#### Public API

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
  uiFeeAddress?: string;           // single-signature mainnet address of the optional 0.4 % UI fee (the Gluon UI sets none, UD13)
  minerFee?: bigint;               // default 1_100_000n (Fleet's RECOMMENDED_MIN_FEE_VALUE)
  fetch?: typeof fetch;            // injectable, for tests
}

export interface ReactorState {    // the contents of a reactor box (Appendix E)
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
  protonLeverage?: number;         // q / (1 − q), while q < 0.99 (Appendix D)
  crashCushion: { untilDepeg: number; untilAdjustment: number }; // fractions, 0.25 = 25 % (Appendix D)
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

// pure functions, for analytics over historical boxes (the history and fee charts)
export { readReactor, reactorRegisters } from "./reactor.ts";
export { oraclePrice, oracleAge } from "./oracle.ts";
export * as math from "./math.ts";  // snapshot, fees, fission, fusion, betaDecayPlus/Minus, adjustPeg, pegAdjustments, shiftVolumes

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

A `ChainState` is a plain object, so the UI can also build one from historical boxes. Because the library has one user,
it exports only what the UI uses: add to the API when the UI needs more, and change it freely.

#### Rules the implementation must respect

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

  The user's own boxes come from the wallet, not from these lookups. Step 2 is a cross-origin POST with a JSON body,
  which the browser preflights. Both public nodes tested on 2026-10-12 (`node.sigmaspace.io`, `node.ergopool.io`)
  allow it from a `github.io` origin (`access-control-allow-origin: *`, `content-type` allowed); §8.6 has a command to
  check any other node.
- **Inputs.** Select the user's inputs with `selectInputs` (`select.ts`), then add them to the transaction builder with
  `ensureInclusion`, in this fixed order: the reactor, the selected user boxes, the buyback box (last). Fleet places
  boxes it selects itself after the ones marked `ensureInclusion` (`BoxSelector.select` in `@fleet-sdk/core` 0.12),
  which would move the buyback box ahead of the user's boxes. `lib/transactions.ts` asserts both positions; keep those
  assertions. `selectInputs(userBoxes, { nanoErgs, tokens })`:
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

#### Dependencies and tests

- **Runtime** (exact versions, no `^`): `@fleet-sdk/core` 0.12.0, `@fleet-sdk/serializer` 0.11.0,
  `@fleet-sdk/common` 0.10.0, `@fleet-sdk/crypto` 0.11.0 and `json-bigint` (it parses node responses without rounding
  amounts, in every browser).
- **Development:** `@fleet-sdk/compiler` 0.12.0, `@fleet-sdk/mock-chain` 0.12.0 and `vitest`. They bring `sigmastate-js`,
  the JavaScript build of the interpreter Ergo nodes run. None of it is in the site's bundle.
- **Contract tests** (R12): each creates a Fleet `MockChain` holding a reactor box, an oracle box and a buyback box,
  builds a transaction with the library, executes it on the mock chain, and checks the consensus rules the mock chain
  skips with `consensusIssues()` ported from the contracts repository's `lib/simulate.ts`. Contract trees:

  | Tree | How to get it |
  |---|---|
  | v3, Dollar constants | Contracts repository: `node scripts/compile.ts config/dollar.json`. The constants are final, so this is the production tree. Save the hex as `fixtures/gluon-dollar-v3.ergotree`. |
  | v3, test constants | Compile `contracts/Gluon.es` with `@fleet-sdk/compiler` (`version: 0`, `segregateConstants: true`) and the test constants of the contracts repository's `test/helpers.ts`. Vendor the source, at a pinned commit, as `fixtures/Gluon.es`. |

  If the contract's source changes, re-vendor both: the tests then show whether the builders still match.

### 5.3 Changes that make the site static

1. **`next.config.ts`:**

   ```ts
   import type { NextConfig } from "next";

   const nextConfig: NextConfig = {
     output: "export",                       // `next build` writes the site to out/
     trailingSlash: true,                    // /swap/ is out/swap/index.html, which GitHub Pages serves
     images: { unoptimized: true },          // the image optimizer needs a server
     basePath: process.env.NEXT_PUBLIC_BASE_PATH || "",  // "/<repo>" for https://<owner>.github.io/<repo>/
     reactStrictMode: true,
   };
   export default nextConfig;
   ```

   Remove the wasm webpack settings, `transpilePackages` and `headers()`: a static export supports no custom headers,
   rewrites or redirects.
2. **Delete `src/pages/api/`** (`getErgPrice.ts`, `hello.ts`): API routes need a server. Add no `getServerSideProps`,
   `getInitialProps` or middleware.
3. **Content-Security-Policy as a meta tag** in `_document.tsx`, since the host sends no headers:
   `<meta httpEquiv="Content-Security-Policy" content="script-src 'self' 'unsafe-inline'; object-src 'none'; base-uri 'self'" />`.
   (`'unsafe-inline'` stays for the inline theme script. A meta tag cannot set `frame-ancestors`.)
4. **Asset paths.** Pass every absolute path to a file in `public/` through `resolveAssetPath(path,
   process.env.NEXT_PUBLIC_BASE_PATH)`, including the `<link>` tags of `_document.tsx` (manifest, apple-touch-icon).
   Make the paths in `public/manifest.json` relative (`"start_url": "./"`, `"src": "logo/gluon.png"`), so that the
   manifest works under any base path.
5. **Site identity.** Delete the hard-coded `<link rel="canonical" href="https://gluon.fi">` of `_document.tsx` and the
   default `url` (`https://www.gluon.gold/`) of `SEO.tsx`. Build the canonical link and the `og:url`, `og:image` and
   `twitter:*` URLs from `NEXT_PUBLIC_SITE_URL` (an absolute URL, which social crawlers need); omit them if it is unset.
6. **`public/.nojekyll`:** an empty file, so that the exported `_next/` folder is never processed by Jekyll.
7. **`src/pages/404.tsx`:** a short "page not found" page; GitHub Pages serves the exported `404.html` for unknown URLs.
8. **Scripts** (`package.json`): `"dev": "next dev --webpack"`, `"build": "next build --webpack"`,
   `"typecheck": "tsc --noEmit"`, `"lint": "eslint ."` (Next.js 16 removed `next lint`), `"test": "vitest run"`. The two
   scripts of `scripts/` are run directly with `node` (Node 24).
9. Delete `vercel.json` and `.github/workflows/nextjs.yml`.

### 5.4 Configuration

The configuration is the `.env.production` file of each fork. Every value is public: it ends up in the browser bundle.
There are no secrets.

| Variable | Required | Meaning |
|---|---|---|
| `NEXT_PUBLIC_GLUON_DEPLOYMENT` | yes | The deployment this site serves, e.g. `gluon-gold-v3`. Must be a deployment of the library's `DEPLOYMENTS`. |
| `NEXT_PUBLIC_NODE_URL` | yes | HTTPS URL of an Ergo node that serves the blockchain index API (`/blockchain/…`) and allows browser requests (CORS). `http://localhost…` is accepted for development. |
| `NEXT_PUBLIC_SITE_URL` | recommended | The site's public URL, with its base path and a trailing slash, e.g. `https://gold.gluon.stability.nexus/`. Used for the canonical link and the social tags. |
| `NEXT_PUBLIC_BASE_PATH` | no | `/<repository name>` when the site is served from `https://<owner>.github.io/<repository>/`. Empty (the default, and the setting of both sites) when it has its own domain. |
| `NEXT_PUBLIC_EXPLORER_URL` | no | Base URL for transaction and token links. Default `https://explorer.ergoplatform.com`; links are `…/en/transactions/<id>`. |
| `NEXT_PUBLIC_EXPLORER_API_URL` | no | Explorer API for the history charts. Default `https://api.ergoplatform.com/api/v1`. |
| `NEXT_PUBLIC_YIELD_CHART_MIN_APY` | no | Threshold of the yield chart (`GaucYieldChart.tsx:209`); unchanged. |

Upstream has `.env.example`, with the same variables, and no `.env.production` (Appendix B). Next.js loads
`.env.production` automatically during `next build`; for development, copy `.env.example` to `.env.local`.

**Validation.** `src/config/env.ts` (in the UI, outside the library: it reads `process.env`) parses and validates the
variables when it is imported, and throws `Gluon UI configuration error: …` with a message that names the variable: the
deployment name exists in the library; the node URL is HTTPS (or localhost); `NEXT_PUBLIC_BASE_PATH` is empty or starts
with `/` and does not end with one. `pages/_app.tsx` imports it, so `next build` fails when the configuration is
invalid, because every page is pre-rendered. At build time it also logs `Gluon UI: <deployment>, node <url>`.

### 5.5 Presentation

`src/config/presentation.ts` holds what a site looks like, per deployment name:

```ts
export interface Presentation {
  protocolName: string;   // navbar and page titles: "GLUON GOLD"
  pegAsset: string;       // "Gold" | "USD": also keys the transmute action types
  pegUnit: string;        // "g of gold" | "USD": one neutron targets "1 <pegUnit>" at the initial peg factor
  favicon: string;        // "/logo/gluon.png" | "/logo/gluon-green.png"
  theme: { primary: string; glow: string; stableToken: string; volatileToken: string; /* … */ };
                          // the 17 keys of today's tokenConfig.theme
}
export const PRESENTATION: Record<string, Presentation> = {
  "gluon-gold-v3": { /* … */ },
  "gluon-dollar-v3": { /* … */ },
};
```

Copy the theme values and favicons from `.env.gold` (Gold) and `.env.dollar` (Dollar) as they are on `pre-v3`.
Tickers are not here: the neutron and proton tickers are `deployment.tokens.neutron.name` and `.proton.name`, which the
bootstrap script sets from the contracts repository's config.

`src/config/tokenConfig.ts` keeps its exported shape, so that components keep working, but is built from the deployment
and its presentation instead of from environment variables.

### 5.6 UI modules

| Module | Role |
|---|---|
| `src/config/env.ts` (new) | §5.4 |
| `src/config/presentation.ts` (new), `src/config/tokenConfig.ts` (rebuilt) | §5.5 |
| `src/lib/providers/GluonProvider.tsx` (new) | The context of UD2: `{ deployment, presentation, client, chain?, snapshot?, status: "loading" \| "ready" \| "error", error?: GluonError, refresh() }` |
| `src/lib/hooks/useGluon.ts` (new) | `useGluon()` reads the context |
| `src/lib/components/blockchain/reactor/ReactorHealth.tsx` (new) | Fusion ratio, healthy band, peg factor and drift, oracle age (§5.7) |
| `src/lib/components/blockchain/reactor/AdjustPegPanel.tsx` (new) | The Adjust Peg flow (§5.7) |
| `src/lib/components/blockchain/reactor/NetworkGuard.tsx` (new) | The wallet-network check (§5.7) |
| `src/lib/utils/amounts.ts` (new) | `toBaseUnits(text, decimals): bigint` and `formatUnits(value, decimals): string`, without floating point |
| `src/lib/utils/error-handler.ts` | Maps `GluonError.code` to the messages of §5.8 |

### 5.7 User experience

**Prices and the peg.** Show:
- "1 ⟨neutron ticker⟩ targets ⟨`pegDrift`⟩ ⟨`pegUnit`⟩", for example "0.990 g of gold" after one downward adjustment,
  with its ERG price `targetPrice`;
- the oracle's price of one peg unit (`pegUnitPrice`);
- the market prices `neutronPrice` and `protonPrice`.

Never claim "pegged to 1 USD" once the peg factor has drifted. All amounts are in ERG, in tokens, or in the pegged
asset: there is no USD value of ERG (UD9).

**Reactor health.** `ReactorHealth` shows the fusion ratio `q` as a percentage on a bar with the healthy band 50–98 %,
the peg factor and its drift ("peg factor 99.0 % of initial"), and the oracle price's age in blocks. The swap card
follows this table:

| State | Condition | What the swap card does |
|---|---|---|
| Healthy | `snapshot.healthy` | Normal. Transmutation MAX buttons use `maxBetaDecayPlus/Minus`. |
| Unhealthy, q above 98 % | `adjustPeg.direction = "down"` | Fission, fusion and transmutations disabled. Banner: "The neutrons in circulation are worth more than 98 % of this reactor's reserve. Trading resumes after the peg factor is lowered, 1 % per step (⟨steps⟩ steps at the current price). Anyone can do this." Show the Adjust Peg panel. |
| Unhealthy, q below 50 % | `adjustPeg.direction = "up"` | Same, with "less than 50 %" and "raised" |
| Oracle stale | `oracleAge ≥ 35` | Actions disabled. "Waiting for the next oracle update." Retried on the next poll. |
| Buyback unavailable | `!betaDecaysAvailable` | Transmutations disabled: "The oracle's buyback box is unavailable." Fission and fusion work as usual. |
| Ambiguous state | last refresh failed with `BOX_AMBIGUOUS` | Actions disabled. "The reactor or the oracle is being updated; retrying." |
| Node unreachable | last refresh failed with `NODE_ERROR` | Actions disabled, last good data shown, "Reconnecting to the node." |

**Adjust Peg flow.** `AdjustPegPanel`, shown only when unhealthy:
1. Show the direction, the steps needed (`quoteAdjustPeg`) and the cost: one miner fee per step, paid by the user, with
   no protocol fee.
2. Button "Adjust peg (step 1 of N)": `client.buildAdjustPeg(chain, { userBoxes, changeAddress })`, then the wallet's
   `sign_tx` and `submit_tx`, then `refresh()`.
3. After the refresh shows the new reactor box (mempool-aware; compare the reactor's `transactionId` with the submitted
   id, retrying every 2 s for up to 60 s), offer the next step. Stop when healthy. Do not sign several steps at once:
   each step needs the box the previous one created.
4. Errors: `REACTOR_HEALTHY` means someone else finished it ("the reactor is healthy again"). `ORACLE_STALE` and
   `BOX_AMBIGUOUS`: retry later. A wallet rejection: stop silently.

**Wallet network.** `NetworkGuard` reads the wallet's change address. A mainnet P2PK address starts with `9`. If it does
not, show a blocking banner ("Your wallet is not connected to Ergo mainnet. Switch it to mainnet.") and disable
actions. The library also throws `INVALID_ADDRESS` for such an address.

**No UI fee (UD13).** The client is created without `uiFeeAddress`, and the fee breakdown has no interface-fee row.

### 5.8 Error messages

| `GluonError.code` | Message |
|---|---|
| `REACTOR_UNHEALTHY` | "The reactor is outside its healthy range. Trading resumes after the peg is adjusted." |
| `WOULD_LEAVE_HEALTHY_RANGE` | "This amount would push the reactor out of its healthy range. The maximum is ⟨max⟩." |
| `REACTOR_HEALTHY` | "The reactor is healthy; no peg adjustment is needed." |
| `ORACLE_STALE` | "The oracle price is too old. Waiting for the next oracle update (usually a few minutes)." |
| `BOX_AMBIGUOUS` | "The reactor or the oracle is being updated right now. Please retry in a minute." |
| `BOX_NOT_FOUND` | "Could not find the reactor or oracle on the network. Check the node." |
| `BUYBACK_UNAVAILABLE` | "Transmutations are paused: the oracle's buyback box is unavailable." |
| `AMOUNT_TOO_SMALL` | "The amount is too small to produce any tokens." |
| `AMOUNT_TOO_LARGE` | "The reactor cannot pay out or release that amount." |
| `INSUFFICIENT_FUNDS` | "Not enough ERG or tokens in your wallet for this transaction and its fees." |
| `INVALID_ADDRESS` | "Your wallet is not connected to Ergo mainnet." |
| `NODE_ERROR` | "Could not reach the Ergo node. Retrying." |
| `UNKNOWN_DEPLOYMENT`, `INVALID_DEPLOYMENT` | configuration errors: caught at build time (UD7); at run time, a generic error |

## 6. Work plan

Each step is one pull request into `main` of the upstream repository. Run `bun run typecheck`, `bun run lint`,
`bun run test` and `bun run build` before merging each one. Where a step says **port**, start from the file named, on
`pre-v3` for UI files (`git show pre-v3:<path> > <path>`) or in the contracts repository for library files, and change
its data layer; the presentation code usually stays.

### PR 1: Foundation

1. **Dependencies.** Remove `gluon-ergo-sdk` 1.x, `ergo-lib-wasm-browser` and `ergo-lib-wasm-nodejs`. Replace
   `@fleet-sdk/core` `^0.8.1` by the exact runtime and development dependencies of §5.2, "Dependencies and tests". Add
   `.nvmrc` with `24` and `"engines": { "node": ">=24" }`. Regenerate `bun.lock`.
2. **Static export.** Apply §5.3: `next.config.ts`, the deletion of `src/pages/api/`, the CSP meta tag, asset paths,
   site identity, `public/.nojekyll`, `404.tsx`, the scripts, and the deletion of `vercel.json` and `nextjs.yml`.
3. **Remove what depends on the SDK 1.x API, and the CoinGecko features.** The chain logic is rewritten, so delete
   every file that imports `gluon-ergo-sdk`, `ergo-lib-wasm-*` or the two `useGluon*` hooks, and the routes that render
   them. They are ported back in PRs 5 to 8 (as of `e4e30ab`), except `MyStats.tsx`, which is removed for good (UD9):
   - swap: `components/blockchain/swap/*`, `functions/reactor/*`, `pages/swap/index.tsx`;
   - dashboard: `components/blocks/dashboard/*`, `pages/reactor/index.tsx`;
   - history and fees: `hooks/useGluonFeeBreakdown.ts`, `hooks/useGluonTransactionHistory.ts`.

   Hide the navigation entries of those pages (`TopNavbar.tsx`, `BottomNavbar.tsx`, `ReactorSidebar.tsx`) until their
   PR. `pages/history` (the user's own transactions, from IndexedDB) does not depend on the chain logic and stays.
4. **Configuration.** Add `src/config/env.ts` (§5.4) and `src/config/presentation.ts` (§5.5), import `env.ts` in
   `pages/_app.tsx`, and rebuild `src/config/tokenConfig.ts`. Delete `.env.gold` and `.env.dollar`; rewrite
   `.env.example` (Appendix B); in `.gitignore`, drop the exceptions for the two deleted files and keep `.env*` ignored
   (the forks add `.env.production` with `git add -f`, §8.3). `env.ts` can only be finished once the library has
   `getDeployment` (PR 2); until then, validate everything but the deployment name.
5. **Vitest.** `vitest.config.ts`: Node as the default environment, a 30 s test timeout, the React plugin
   (`@vitejs/plugin-react`, already a dev dependency) and the alias `@` → `src`. UI component tests opt into jsdom with
   a `// @vitest-environment jsdom` comment at the top of the file; library tests stay on Node.
6. **The boundary.** The ESLint rule and `.github/CODEOWNERS` of §5.2, "Layout and boundary". After PR 1 is merged, a
   repository admin turns on the branch protection described there, so that PRs 2 to 10 need a code-owner review.
7. **Provider, utilities and a fake client.** Add `GluonProvider` and `useGluon` (§5.6) and mount the provider in
   `lib/providers/Providers.tsx`. Add `amounts.ts`. In `lib/constants/token.ts`, keep `ACTION_TYPES` and remove
   `TOKEN_ADDRESS`; `erg-converter.ts` takes decimals from the deployment. Rewrite `lib/utils/node-service.ts` without
   wasm, keeping what the transaction listener uses (height, unconfirmed-transaction lookups). `test/fakeClient.ts`
   implements the `GluonClient` surface of §5.2 with fixed `ChainState`, snapshots and quotes, and records the calls it
   receives. Until PR 4 lands the real `GluonClient`, declare the types of §5.2 in `src/lib/gluon/types.ts` and let the
   provider depend on that interface.
8. **CI and publishing.** Add `.github/workflows/ci.yml` and `.github/workflows/pages.yml` as in §8.4, with the actions
   pinned by commit SHA. Add `bun run build` to `ci.yml` as soon as the library bundles a deployment.
9. **Tests.** `env.ts`: valid and invalid node URLs, a malformed base path. `GluonProvider`, with the fake client:
   polling, keeping the last good state on error, pausing while the tab is hidden. `amounts.ts`: round trips for 0, 3
   and 9 decimals and for values above 2⁵³.

**Done when** the checks above pass, and the home page renders with a deployment's presentation (with a fake or
synthetic deployment until PR 2). `bun run build` writes `out/` once a deployment is bundled.

### PR 2: Library: deployments, math and reactor state

1. `scripts/sync-deployments.ts` (Node 24):
   - downloads `deployments/mainnet.json` from the contracts repository at a ref given with `--ref` (default `main`);
   - keeps only the v3 records: those whose `genesis.registers.R9` decodes (Fleet serializer `parse`) to a pair, which
     is what tells a v3 reactor from every earlier one (the file also holds `gluon-gold-pre-v2`, `gluon-gold-v2`,
     `gluon-dollar-pre-v3-a` and `-b`);
   - writes them to `src/lib/gluon/deployments/data.ts` as `export default [...] as const`, adding a `source` field with
     the repository, the ref and the commit.

   Commit the output. While no v3 deployment is recorded, the list is empty.
2. `deployments/fromRecord.ts` maps a record to a `GluonDeployment`:

   | `GluonDeployment` field | Record field |
   |---|---|
   | `name`, `status`, `peg`, `ergoTree`, `address` | same names |
   | `tokens.{nft,neutron,proton}` | `tokens.*`, with `emission` as `BigInt` |
   | `minFee` | `BigInt(constants._MinFee)` |
   | `oraclePoolNft`, `oracleBuybackNft`, `oracleFeeTree` | `constants._OraclePoolNFT`, `constants._OracleBuybackNFT`, `constants._OracleFeePk` |
   | `initialPegFactor` | second element of the decoded `genesis.registers.R9` |

   It throws `INVALID_DEPLOYMENT` for a record whose R9 is not a pair, or that lacks a field. `deployments/index.ts`:
   `DEPLOYMENTS` holds every record of the data file, and `getDeployment(name)` finds one by name or throws
   `UNKNOWN_DEPLOYMENT`. Finish `env.ts` with it.
3. Port `lib/math.ts` to `math.ts` as it is: constants, `snapshot`, `fees`, `fission`, `fusion`, `betaDecayPlus`,
   `betaDecayMinus`, `adjustPeg`, `pegAdjustments`, `shiftVolumes`, `genesisSupplies`. Add `crashCushion` and
   `protonLeverage` (Appendix D).
4. Port `lib/reactor.ts` to `reactor.ts`: `readReactor(box, deployment)` also checks the box's ErgoTree and the token ids
   against the deployment. The R5 check (a 33-byte key) stays. Add `oracle.ts` (`oraclePrice`, `oracleAge`).
5. Tests, with hand-computed expectations:
   - a synthetic v3 record (compile `fixtures/Gluon.es` with `@fleet-sdk/compiler`, as §5.2 describes, and fill in the
     other fields) parses to the expected `GluonDeployment`; a record whose R9 is a plain `Long` throws
     `INVALID_DEPLOYMENT`; the sync filter, run on a copy of the current `deployments/mainnet.json` (vendored as
     `fixtures/mainnet-records.json`), drops all four of its records;
   - genesis supplies: q = 0.6 and a proton price of 1 ERG, for the Gold and Dollar parameters;
   - fission and fusion amounts, and the dev fee's linear decay;
   - bucket shifting at 0, 1, 13, 14 and 30 elapsed buckets;
   - a beta decay in each direction, including the volume fee;
   - Adjust Peg's 1 % steps, its rounding, and a chain of five steps from q = 1.03;
   - `crashCushion` and `protonLeverage`.

**Done when** every formula of Appendix D has a test.

### PR 3: Library: node client, lookups and input selection

1. `node.ts`: parse responses with `json-bigint` (`useNativeBigInt: true`); implement `height()` and
   `unspentByTokenId(tokenId)` with the lookup steps of §5.2; time out after 25 s or more. Failures throw `NODE_ERROR`.
2. `fetchChainState()` (in `client.ts`, finished in PR 4): height, reactor, oracle and buyback box (missing allowed),
   fetched in parallel, then `readReactor`.
3. `select.ts`: `selectInputs` as specified in §5.2.
4. Tests with an injected `fetch`: a stale index entry is dropped; a box spent in the mempool is dropped; two live
   candidates give `BOX_AMBIGUOUS`; a reactor box with the wrong ErgoTree is ignored; a missing buyback box gives
   `buyback: undefined`, not an error; an HTTP error gives `NODE_ERROR`. For `selectInputs`: only the needed boxes are
   chosen, and `INSUFFICIENT_FUNDS` is thrown when they cannot cover the requirements.

**Done when** those cases pass.

### PR 4: Library: transaction builders, the client and contract tests

1. Port `lib/transactions.ts` to `transactions.ts`. Keep the output layout and fee order, the buyback top-up (`R4` = id
   of the spent box, variable 0 = `Int 1`), the UI-fee variable on the reactor input, the treasury read from R5 and the
   position assertions.
   Change: `recipient` is the caller's `changeAddress`; inputs come from `selectInputs` instead of being all the caller's
   boxes; outputs get creation height `chain.height`. Every builder returns `EIP12UnsignedTransaction` (Fleet's
   `toEIP12Object()`), with full input and data-input boxes and the context extensions, which Nautilus needs to sign.
   Update Treasury requires the first selected input to be a box of the current treasury's address.
2. `client.ts`: `GluonClient` as in §5.2, and `errors.ts`. Every refusal in "Rules the implementation must respect"
   throws a `GluonError` with a message the UI can show. `snapshot()` fills every `ProtocolSnapshot` field (Appendix D);
   `volumes` are shifted to the current `HEIGHT` with `shiftVolumes(…, nDays, 0n)`, so a bucket that is more than a day
   old is no longer shown as today's; `betaDecaysAvailable` is false when `chain.buyback` is undefined.
3. Port the contracts repository's `test/helpers.ts` and `lib/simulate.ts` (`consensusIssues`) into
   `__tests__/helpers.ts`, and write the contract tests of §5.2, "Dependencies and tests". Cases (both pegs: a per-kg
   Gold oracle with peg factor 0.001, a per-USD Dollar oracle with peg factor 1):
   - each builder is accepted: fission, fusion, both beta decays (with and without an oracle fee), Adjust Peg in both
     directions, Update Treasury, and the UI fee;
   - changing a token amount, a fee value or R9 by one unit is rejected;
   - a beta decay against the real DORT buyback box (`fixtures/dort-buyback-box.json`, from the contracts repository) is
     accepted, and the same transaction without the buyback's R4 is rejected;
   - positions: the reactor is the first input and the buyback box the last, and the fee outputs follow §5.2, "The v3
     contract in brief", for every combination of zero and non-zero fees;
   - input selection: only the needed boxes are spent, the buyback box stays last, and no change box is below the
     minimum value (a wallet whose boxes would leave dust change gets one more box, or `INSUFFICIENT_FUNDS`).
4. One test per error code, and `snapshot()` against hand-computed values.
5. `scripts/smoke.ts <deployment> <node URL>` (Node 24): fetches the chain state, prints the snapshot, builds every
   allowed transaction with a fictitious funding box, and sends each to the node's `/transactions/check`. Expect
   "missing input" errors only; any other rejection is a bug. The node rejects the fictitious input before it evaluates
   any script, so this checks structure and serialization against live boxes, not the contract's rules (the contract
   tests cover those).

**Done when** the contract tests pass for every builder.

### PR 5: The swap card

**Port** `ReactorSwap.tsx`, `FeeBreakdownPanel.tsx`, `functions/reactor/*` and `pages/swap/index.tsx`.

1. `ReactorSwap` takes `client`, `chain` and `snapshot` from `useGluon()`; it initializes nothing.
2. Each `calculate*Amounts` becomes a call to `client.quote*`: parse input with `toBaseUnits(text, decimals)` using the
   token's `decimals` from the deployment, format with `formatUnits`, and never convert an amount to `number`. Fusion's
   maximum ERG comes from the wallet's balances and `quoteFusion` (take the token ratio for 1 ERG, then verify the
   result with `quoteFusion`).
3. Each `handle*Swap` becomes: `refresh()`; `client.build*(chain, { …, userBoxes: await getUtxos(), changeAddress: await
   getChangeAddress() })`, both from `useErgo()` (`ErgoProvider.tsx:330-349,378-388`); `sign_tx`, then `submit_tx`;
   `refresh()`. The library selects the inputs it needs.
4. Token ids, decimals and tickers come from `deployment.tokens`: `defaultTokens` (`utils.ts`) becomes a function of the
   deployment; drop the `gau`/`gauc` legacy fields. `ACTION_TYPES` stays, keyed by `pegAsset`.
5. Health gating (§5.7): disable the controls per the state table; MAX for transmutations = min(balance,
   `maxBetaDecayPlus/Minus`).
6. The fee panel shows `quote.fees` (dev, oracle, `outputsMinFee`, miner) **in ERG only**: drop the `ergPriceUSD`
   property and the USD amounts, and show no interface fee (UD13).
7. `error-handler.ts` maps the codes of §5.8; explorer links use `…/en/transactions/<id>`.
8. Tests, with the fake client: each state of §5.7 enables and disables the right controls; each `handle*Swap` calls
   `build*` with the wallet's UTXOs and change address; the MAX buttons.

**Done when**, on the first mainnet deployment (§7.3), a small fission and fusion succeed from the UI. Until a
deployment exists, the fake-client tests are the check.

### PR 6: Reactor health, Adjust Peg and the network guard

1. `ReactorHealth`, `AdjustPegPanel` and `NetworkGuard` (§5.7), on the swap page; `ReactorHealth` also on the reactor
   dashboard when PR 7 adds it.
2. Tests, with the fake client: a healthy reactor shows no panel; an unhealthy one shows the direction and steps; one
   step calls `buildAdjustPeg`, signs, submits, and waits for the new reactor box; `REACTOR_HEALTHY` after a step ends
   the flow; a wallet rejection leaves the panel usable; a non-mainnet address blocks actions.

**Done when** those tests pass. Adjust Peg can only be run live when a real reactor leaves its healthy range (§7.3).

### PR 7: Dashboard

**Port** `GluonStats.tsx`, `ProtonAPYBadge.tsx`, `ReserveRatioChart.tsx`, `GaucLeverageChart.tsx`,
`GaucYieldChart.tsx`, `CumulativeFeesChart.tsx`, `VolumeChart.tsx`, `GluonExplainer.tsx` and `pages/reactor/index.tsx`,
reading `snapshot` from `useGluon()`. **Do not port `MyStats.tsx`**, and leave out the ERG price card, the ERG price
polling and the USD figures of `GluonStats.tsx` (UD9).

| Statistic | Source |
|---|---|
| Price of one peg unit | `pegUnitPrice` |
| Target price of one neutron, with the peg factor's drift | `targetPrice`, `pegFactor`, `pegDrift` |
| Neutron and proton price | `neutronPrice`, `protonPrice` |
| Total value locked | `reserve` |
| Fusion ratio | `q` |
| Reserve ratio | `reserveRatio` |
| Price crash cushion | `crashCushion.untilDepeg`; also `untilAdjustment` ("fall before trading pauses") |
| Proton leverage | `protonLeverage`, shown as "—" when undefined |
| Circulating supplies | `neutronsInCirculation`, `protonsInCirculation` |
| Volumes of 1, 7 and 14 days, and per-day series | `volumes.plus`, `volumes.minus` (sum the first 1, 7 or 14 buckets) |
| Health, and the oracle's age | `healthy`, `oracleAge` (the `ReactorHealth` component) |

Also in this PR:
- Token names come from the deployment, and the peg-price card's label from the presentation.
- `GluonExplainer.tsx` gets a paragraph on the healthy range and Adjust Peg.
- Tests: `GluonStats` with fixed snapshots for Gold at peg drift 1 and 0.99, and for Dollar.

**Done when** the dashboard shows the right figures for the first mainnet deployment.

### PR 8: History and fee charts

**Port** `useGluonTransactionHistory.ts` and `useGluonFeeBreakdown.ts`, and the charts that use them.

1. **Parameterize by the deployment:** the reactor NFT, token ids and decimals, and the oracle pool NFT. A v3 reactor
   keeps one contract and one NFT for its whole life, so there is no address lineage and no migration handling.
2. **Explorer API** from `NEXT_PUBLIC_EXPLORER_API_URL` (or its default). Include the deployment's name in the cache
   keys (for example `gluon_box_history_v10:<deployment>`), so that deployments served from one origin, such as
   `localhost` in development, do not share caches.
3. **Decode** explorer boxes into the node's box shape (registers as plain hex strings), then use the library's pure
   functions: `readReactor(box, deployment)`, `oraclePrice` and `math.*`. Price each historical box with its own peg
   factor and the oracle box nearest its height.
4. **Classify** each transition by the box's deltas: fission, fusion and beta decays as in the current hook, plus
   **Adjust Peg** (value, tokens, R4–R8 and R9's first element unchanged; the peg factor changed) and **Update Treasury**
   (only R5 changed). The last two carry no fees; plot them as markers rather than as "unknown".
5. Reconstruct fees per transition with `math.*`, in the categories the hook has today (dev fee, dilution, oracle fee),
   in ERG.
6. Tests with recorded explorer responses of a real v3 reactor, as fixtures: one transition of each type that has
   occurred, with the expected category and fee amounts; for Adjust Peg, a hand-built box sequence.

**Done when** the charts render for the first mainnet deployment.

### PR 9: Documentation and clean-up

1. `README.md`: what the app is; the configuration (§5.4); running locally (`.env.local`); the library and its boundary
   (§5.2); the two deployments and how the forks relate to upstream (§3, §8); Adjust Peg in a few sentences. Replace the
   badge link, which points to another project, with the addresses of the two sites. No CoinGecko step.
2. `docs/github-pages.md`: §8 of this plan, with the final repository names and domains.
3. Check that nothing of the old version is left: `src` has no literal token tickers, no `0.66`, no kg-to-gram `÷ 1000`,
   no `gluon-gold-v2`, no `COINGECKO`, no `gluon-ergo-sdk`, no `NEXT_PUBLIC_DEPLOYMENT` or `NEXT_PUBLIC_NODE` (without
   `_URL`), no `/api/` call, no use of `uiFeeAddress` outside `src/lib/gluon`, and `package.json` has no unused
   dependency.
4. Run the static checks of §7.2 locally.

### PR 10: The two deployments

Done by the maintainers once the first mainnet bootstraps of the contracts repository are recorded:

1. In upstream, run `node scripts/sync-deployments.ts --ref <commit of the contracts repository that records the
   deployments>`, add a contract test for each new deployment's exact ErgoTree (§7.1), and merge.
2. Create the forks and publish the sites: §8.3 and §8.5, once per fork.
3. Verify each site: §8.7.

## 7. Test plan

### 7.1 Automated tests (Vitest)

- **Library unit tests:** math, register encoding, record parsing and the sync filter, node-client filters, input
  selection and error codes, as listed in PRs 2 to 4.
- **Library contract tests**, on a simulated chain, as listed in PR 4. When a deployment is recorded, add a test that
  builds each transaction against a reactor box with that deployment's exact ErgoTree.
- **UI tests**, with the fake client: PRs 1, 5, 6 and 7.
- All of them run in CI.

### 7.2 Static checks

- **The exported site, locally:** with a valid `.env.local`, run `bun run build`, then serve `out/` with any static
  server (for example `bunx serve out`) and open it. Check that the home page, `/swap/`, `/reactor/` and `/history/`
  load, that a direct visit to a deep link works, and that an unknown URL shows the 404 page.
- **A base path:** build with `NEXT_PUBLIC_BASE_PATH=/test`, copy `out/` to a folder named `test` inside an empty
  directory, serve that directory, and check that the logo, favicon, manifest and chunks load under `/test/`.
- **No server code:** `out/` contains only files, and nothing in `src` calls `/api/`.

### 7.3 Mainnet checks

There is no testnet. The first mainnet bootstrap, with a small seed, is the first live test. After it, with the
deployment recorded and bundled, run `node scripts/smoke.ts <deployment> <node URL>` (PR 4), and then, with small
amounts:

1. A fission, a fusion and both transmutations from the UI. Check the received amounts against the quote, and the fee
   outputs in the explorer.
2. The MAX buttons, including a transmutation capped by the healthy range.
3. The dashboard and the charts.
4. The wallet-network banner, with a wallet that is not on mainnet.

**Adjust Peg and oracle staleness** can only be seen when they actually happen: the oracle price must move by tens of
percent for a reactor to leave its healthy range, and the oracle must miss updates to become stale. Until then, the
library's contract tests cover the transactions, and the fake-client tests cover the UI's reaction.

## 8. GitHub Pages guide

### 8.1 How the sites are published

Upstream holds the code; each fork builds and publishes one site (§3). A push to a fork's `main` runs the workflow of
§8.4: it type-checks, tests, builds the static site with the fork's `.env.production`, and deploys it to the fork's
GitHub Pages site. The workflow does nothing in upstream.

### 8.2 Before you start

1. **Rights.** You can create repositories in the `StabilityNexus` organization and change their settings.
2. **The code.** Upstream `main` has PRs 1 to 9 merged, and the library's data includes the deployment you are
   publishing. This prints its live snapshot, from a checkout of upstream with `bun install` done:
   `node scripts/smoke.ts gluon-gold-v3 https://node.sigmaspace.io`.
3. **A node** for `NEXT_PUBLIC_NODE_URL` that passes the checks of §8.6.
4. **DNS access** for `stability.nexus`, to add one `CNAME` record per site (§8.3, step 7) and, recommended, the `TXT`
   record that verifies `gluon.stability.nexus` for the organization.
5. The repositories are public: GitHub Pages is available for public repositories on every plan.

### 8.3 Create a fork and publish its site

Repeat for each deployment (Gold, Dollar).

1. **Fork.** On `StabilityNexus/Gluon-Ergo-UI`: **Fork → Create a new fork**. Owner `StabilityNexus`; repository name
   `Gluon-Ergo-UI-Gold` (Gold) or `Gluon-Ergo-UI-Dollar` (Dollar); tick **Copy the `main` branch only**, so that the
   legacy `mainnet` branch is not copied.
   (GitHub allows a fork in the same organization as its parent, under a different name. `gh repo fork` does the same;
   see `gh repo fork --help` for the name and branch options of your version.)
2. **Enable workflows.** Forks start with workflows disabled: Actions tab → **I understand my workflows, go ahead and
   enable them**.
3. **Pages source.** Settings → Pages → Build and deployment → Source: **GitHub Actions**.
4. **Deployment branch.** Settings → Environments → `github-pages`: deployment branches limited to `main`. GitHub
   creates this environment when Pages is set up with Actions or on the first run; if it is not there yet, do this step
   after step 6.
5. **The fork's one file.** Clone the fork, create `.env.production` as in §8.5, and commit it. The file is ignored by
   `.gitignore`, so add it with `git add -f .env.production`. Push to `main`.
6. **Watch the run.** Actions → *Deploy to GitHub Pages*: the `build` job type-checks, tests and builds; the `deploy`
   job publishes. The deployment's URL appears on the run's summary and in Settings → Pages.
7. **Custom domain.** Settings → Pages → Custom domain: `gold.gluon.stability.nexus` (Gold) or
   `dollar.gluon.stability.nexus` (Dollar), and save. At the DNS provider, add a `CNAME` record from that name to
   `stabilitynexus.github.io` (the organization's address, without the repository name). When GitHub's DNS check
   passes, tick **Enforce HTTPS**. With this publishing method a `CNAME` file in the repository is ignored: the setting
   is what counts. `NEXT_PUBLIC_SITE_URL` in `.env.production` already holds the new address (§8.5).

   **Recommended: verify the domain first.** As an owner of the organization: organization Settings → Pages → Add a
   domain → `gluon.stability.nexus`, then create the `TXT` record GitHub shows (named
   `_github-pages-challenge-StabilityNexus.gluon.stability.nexus`) and click Verify. A verified domain can be used only
   by repositories of the organization, so nobody else can attach `gold.gluon.stability.nexus` or
   `dollar.gluon.stability.nexus` to another repository, for instance if a `CNAME` record is ever left dangling.
   Verification covers a domain's immediate subdomains only, which is why the domain to verify is `gluon.stability.nexus`
   (verifying `stability.nexus` would not cover `gold.gluon.stability.nexus`). Do not add a wildcard DNS record.
8. Run the verification of §8.7.

### 8.4 The workflows

Both files are in upstream from PR 1 and reach the forks by merge. The actions are pinned by commit SHA (their newest
releases on 2026-10-12); update the pins when updating the actions.

`.github/workflows/pages.yml`:

```yaml
name: Deploy to GitHub Pages

on:
  push:
    branches: [main]
  workflow_dispatch:

permissions:
  contents: read

concurrency:
  group: pages
  cancel-in-progress: false

jobs:
  build:
    # Only the deployment forks publish: upstream has no .env.production.
    if: github.repository != 'StabilityNexus/Gluon-Ergo-UI'
    runs-on: ubuntu-24.04
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
      - uses: actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0
        with:
          node-version-file: .nvmrc
      - uses: oven-sh/setup-bun@0c5077e51419868618aeaa5fe8019c62421857d6 # v2.2.0
      - run: bun install --frozen-lockfile
      - run: bun run typecheck
      - run: bun run test
      - run: bun run build            # reads .env.production; writes the static site to out/
      - uses: actions/upload-pages-artifact@fc324d3547104276b827a68afc52ff2a11cc49c9 # v5.0.0
        with:
          path: out

  deploy:
    needs: build
    runs-on: ubuntu-24.04
    permissions:
      pages: write
      id-token: write
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - id: deployment
        uses: actions/deploy-pages@368f82528645a54fb793d4d04e342629a3f51346 # v5.0.1
```

`.github/workflows/ci.yml` (runs in upstream and in the forks):

```yaml
name: CI

on:
  push:
  pull_request:

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  check:
    runs-on: ubuntu-24.04
    timeout-minutes: 20
    steps:
      - uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
      - uses: actions/setup-node@949feb2413d6458794dcd2491c4babbbce0c15c1 # v7.1.0
        with:
          node-version-file: .nvmrc
      - uses: oven-sh/setup-bun@0c5077e51419868618aeaa5fe8019c62421857d6 # v2.2.0
      - name: Upstream must not contain deployment configuration
        if: github.repository == 'StabilityNexus/Gluon-Ergo-UI'
        run: test ! -f .env.production
      - name: Use the example configuration where a repository has none
        run: test -f .env.production || cp .env.example .env.production
      - run: bun install --frozen-lockfile
      - run: bun run typecheck
      - run: bun run lint
      - run: bun run test
      - run: bun run build
```

Notes:
- The `build` job is skipped in upstream, so `deploy`, which needs it, is skipped too.
- Pushes to a fork run both workflows. The CI check also builds, with the fork's own `.env.production`.
- The tests include the library's contract tests, which run the real contract on a simulated chain; they take a few
  tens of seconds.
- If the upstream repository is renamed, update the repository name in the two `if:` conditions.
- The newest actions may have moved on; check each action's releases before pinning a newer SHA.

### 8.5 The `.env.production` of each fork

These files are the only difference between the forks.

Gold (`StabilityNexus/Gluon-Ergo-UI-Gold`):

```sh
NEXT_PUBLIC_GLUON_DEPLOYMENT=gluon-gold-v3
NEXT_PUBLIC_NODE_URL=https://node.sigmaspace.io
NEXT_PUBLIC_SITE_URL=https://gold.gluon.stability.nexus/
```

Dollar (`StabilityNexus/Gluon-Ergo-UI-Dollar`):

```sh
NEXT_PUBLIC_GLUON_DEPLOYMENT=gluon-dollar-v3
NEXT_PUBLIC_NODE_URL=https://node.sigmaspace.io
NEXT_PUBLIC_SITE_URL=https://dollar.gluon.stability.nexus/
```

Neither file sets `NEXT_PUBLIC_BASE_PATH`: both sites have their own domain. Without a custom domain (a preview, or a
fallback), a Gold site would be served from `https://stabilitynexus.github.io/Gluon-Ergo-UI-Gold/`, and its
`.env.production` would set `NEXT_PUBLIC_BASE_PATH=/Gluon-Ergo-UI-Gold` and
`NEXT_PUBLIC_SITE_URL=https://stabilitynexus.github.io/Gluon-Ergo-UI-Gold/`. A fork whose `.env.production` is missing
or invalid fails its build (UD7).

### 8.6 Check the node

Run these for the value of `NEXT_PUBLIC_NODE_URL`, with `ORIGIN` set to the site's origin
(`https://gold.gluon.stability.nexus` or `https://dollar.gluon.stability.nexus`):

```sh
NODE=https://node.sigmaspace.io
ORIGIN=https://gold.gluon.stability.nexus
# 1. The blockchain index is on and in sync (indexedHeight equals fullHeight):
curl -s "$NODE/blockchain/indexedHeight"
# 2. Browsers may call the node. The library makes one cross-origin POST with a JSON body, which browsers preflight:
curl -s -D - -o /dev/null -X OPTIONS "$NODE/utxo/withPool/byIds" -H "Origin: $ORIGIN" \
  -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type" \
  | grep -i "^HTTP\|access-control"
# 3. A plain GET carries the CORS header too:
curl -s -D - -o /dev/null -H "Origin: $ORIGIN" "$NODE/info" | grep -i "^HTTP\|access-control-allow-origin"
```

Expected: JSON with equal heights; `HTTP/2 200` with an `access-control-allow-origin` of `*` or `$ORIGIN` and an
`access-control-allow-headers` that includes `Content-Type`; the same `access-control-allow-origin` on the GET. On
2026-10-12, `https://node.sigmaspace.io` and `https://node.ergopool.io` passed all three from the origin
`https://stabilitynexus.github.io`, and the explorer API at `https://api.ergoplatform.com/api/v1` returned
`access-control-allow-origin: *` for GET requests. A `*` does not depend on the origin; run the checks again with the
sites' own origins before launch.

### 8.7 Verify a published site

1. The workflow run is green, and its summary shows the site's URL.
2. Open the site: the name, colors and favicon are the deployment's; the token tickers are the on-chain names (compare
   with `Deployments.md` of the contracts repository); the dashboard numbers load.
3. In the page source: the CSP meta tag is present, and the canonical link and `og:url` are the site's address.
4. In the browser's developer tools (Network): requests go to the node's URL and the explorer API, with no CORS errors
   and no request to any other host.
5. Open a deep link directly (`/swap/`): it loads. Open an unknown path: the 404 page appears.
6. Connect Nautilus. With a wallet that is not on mainnet, the wallet-network banner appears.
7. The mainnet checks of §7.3, with small amounts.

### 8.8 Keep the forks in sync

In each fork, once: `git remote add upstream https://github.com/StabilityNexus/Gluon-Ergo-UI.git`. For each change:

```sh
git fetch upstream
git switch main
git merge upstream/main      # GitHub's "Sync fork" button does the same
git push origin main         # the push publishes the site
```

- **The rule that keeps merges conflict-free:** a fork never edits a shared file. Its only file of its own is
  `.env.production`, which upstream never contains (the upstream CI fails if it does). Make every change in upstream
  first, including changes to the library and to the deployment data.
- After any change to the library or its data, merge into **both** forks and verify each site (§8.7).
- When upstream adds a required variable, add it to each fork's `.env.production`; until then the fork's build fails.

### 8.9 Day-two operations

- **Change a fork's configuration:** edit `.env.production` and push.
- **Roll back:** `git revert` the offending merge in the fork and push. The workflow publishes the reverted state.
- **Change a site's domain:** Settings → Pages → Custom domain, the DNS record, and `NEXT_PUBLIC_SITE_URL`.
- **Add a deployment (another v3 reactor):** `node scripts/sync-deployments.ts`, an entry in `presentation.ts`, a
  contract test for its ErgoTree, then a new fork (§8.3).
- **Retire a site:** disable Pages in the fork (Settings → Pages) and archive the repository.

### 8.10 Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| The workflow does not run in a fork | Workflows are disabled in new forks: enable them (Actions tab). |
| `pages.yml` shows both jobs skipped | The repository is upstream, or its name is the one in the `if:` condition. |
| The `deploy` job fails | Settings → Pages → Source must be **GitHub Actions**; and the `github-pages` environment must allow `main`. |
| The build fails with `Gluon UI configuration error: …` | `.env.production` is missing or invalid: the message names the variable. |
| The site loads but images, styles or scripts return 404 | The base path is wrong: set `NEXT_PUBLIC_BASE_PATH=/<repository>` for a `github.io/<repository>/` address, or leave it empty for a custom domain. |
| A direct visit to a page returns 404 | `trailingSlash: true` and the exported `404.html` are required (§5.3); check that `out/swap/index.html` exists. |
| The custom domain shows a certificate warning or 404 | DNS has not propagated, the `CNAME` target is wrong (it must not include the repository name), or **Enforce HTTPS** is not available yet. |
| The site shows an initialization error | The node is unreachable or blocks browsers: run §8.6. |
| An old version is still shown | GitHub's cache can take a few minutes: hard-reload, and check the run's commit. |

### 8.11 Limits and terms

- **Size and traffic:** a published Pages site is limited to 1 GB, with a soft bandwidth limit of about 100 GB a month.
  The 10-builds-an-hour limit does not apply to sites published with a custom workflow. This site is a few megabytes.
- **Terms:** GitHub's terms for Pages say it is not intended as a host for a site whose main purpose is facilitating
  commercial transactions or offering commercial software as a service. A protocol front end could be read either way.
  The maintainers have decided that Pages is acceptable for these sites (§10). **If GitHub objects,** nothing else
  changes: `out/` is a plain static site, and moving a fork to another static host replaces only the `deploy` job of
  §8.4.

### 8.12 The legacy site

The Vercel project(s) that serve the current UI keep deploying `mainnet` and are outside this plan (Step 0).

## 9. Definition of done

- All pull requests of §6 are merged in upstream, and upstream's CI is green.
- Both forks exist, each with its `.env.production`, and each site passes §8.7 at its own address (§3).
- `mainnet` is unchanged since Step 0, and the existing Vercel project still deploys it.
- The SDK repository and the npm package are unchanged.
- Branch protection on upstream `main` requires a code-owner review (§5.2).
- The code has no v2 path, no use of `gluon-ergo-sdk`, no testnet support, no server code, no CoinGecko dependency and
  no UI fee (PR 9, step 3), and `src/lib/gluon` passes the boundary rule.

## 10. Decisions and open items

**Decided by the maintainers (2026-10-12).**
1. The forks are `StabilityNexus/Gluon-Ergo-UI-Gold` and `StabilityNexus/Gluon-Ergo-UI-Dollar`.
2. The sites are https://gold.gluon.stability.nexus/ and https://dollar.gluon.stability.nexus/.
3. There is no UI fee (UD13).
4. GitHub Pages is acceptable for these sites (§8.11).
5. Porting starts from `pre-v3` (`main` at `e4e30ab`), the newest version of the legacy design.
6. Proton leverage is shown as `q / (1 − q)`, as in the legacy UI (Appendix D).
7. `CODEOWNERS` is enforced on `src/lib/gluon/` and the files that guard or ship it, through branch protection (§5.2).

**Decided by the maintainers (2026-10-13).**
1. The maintainers control the DNS of `stability.nexus`, for the `CNAME` and `TXT` records of §8.3, step 7.
2. The sites use `https://node.sigmaspace.io` as their node.
3. If an SDK is ever published, it will be a new npm package that the maintainers own, `@stabilitynexus/gluon-ergo-sdk`
   (`NPM_GUIDE.md`). This plan publishes nothing.

**Not decided; the default applies.** What happens to the SDK repository? **Nothing in this plan.** It stays at 1.0.3,
which serves v2 reactors only. The maintainers can decide later to archive it, to point its README to this UI, or to
extract `src/lib/gluon` into the new package (UD11).

**To confirm before PR 1:** who are the code owners? The GitHub handles for `.github/CODEOWNERS` (§5.2).

## Appendix A: File checklist

| Path | Action | PR |
|---|---|---|
| `package.json`, `bun.lock` | remove SDK 1.x and ergo-lib-wasm; Fleet, `json-bigint`, `@fleet-sdk/compiler`, `@fleet-sdk/mock-chain`, `vitest` at exact versions; `engines`; scripts | 1 |
| `.nvmrc` | new (`24`) | 1 |
| `next.config.ts` | static export (§5.3) | 1 |
| `vitest.config.ts` | new | 1 |
| `vercel.json`, `.github/workflows/nextjs.yml` | delete | 1 |
| `.github/workflows/ci.yml`, `pages.yml`, `.github/CODEOWNERS` | new (§8.4, §5.2) | 1 |
| `eslint.config.mjs` | the library boundary rule | 1 |
| `.env.gold`, `.env.dollar` | delete (values move to `presentation.ts`) | 1 |
| `.env.example`, `.gitignore` | rewrite; drop the two exceptions | 1 |
| `src/pages/api/*` | delete | 1 |
| `src/pages/_document.tsx`, `src/lib/components/layout/SEO.tsx`, `public/manifest.json` | CSP meta, asset paths, site identity | 1 |
| `public/.nojekyll`, `src/pages/404.tsx` | new | 1 |
| `src/config/env.ts`, `src/config/presentation.ts` | new | 1, 2 |
| `src/config/tokenConfig.ts` | rebuilt from deployment and presentation | 1 |
| `src/lib/providers/GluonProvider.tsx`, `src/lib/hooks/useGluon.ts`, `Providers.tsx` | new; mount | 1 |
| `src/lib/utils/amounts.ts`, `erg-converter.ts`, `node-service.ts` | new; decimals from the deployment; no wasm | 1 |
| `src/lib/constants/token.ts` | keep `ACTION_TYPES`; remove `TOKEN_ADDRESS` | 1 |
| `test/fakeClient.ts` | new | 1 |
| `src/lib/gluon/**` | new library (§5.2) | 2–4 |
| `scripts/sync-deployments.ts`, `scripts/smoke.ts` | new | 2, 4 |
| files that use the SDK 1.x API (PR 1, step 3) | delete in PR 1, then port | 1, 5–8 |
| `src/lib/components/blocks/dashboard/MyStats.tsx` | delete, not ported (UD9) | 1 |
| `components/blockchain/reactor/ReactorHealth.tsx`, `AdjustPegPanel.tsx`, `NetworkGuard.tsx` | new | 6 |
| `README.md`, `docs/github-pages.md` | rewrite; new | 9 |
| forks: `.env.production` | new, one per fork | 10 |

## Appendix B: `.env.example`

```sh
# The deployment this site serves: a v3 deployment in src/lib/gluon/deployments/data.ts.
NEXT_PUBLIC_GLUON_DEPLOYMENT=gluon-gold-v3

# HTTPS Ergo node that serves the blockchain index API and allows browser requests.
NEXT_PUBLIC_NODE_URL=https://node.sigmaspace.io

# The site's public URL, with its base path and a trailing slash.
NEXT_PUBLIC_SITE_URL=https://gold.gluon.stability.nexus/

# Optional.
# NEXT_PUBLIC_BASE_PATH=/Gluon-Ergo-UI-Gold
# NEXT_PUBLIC_EXPLORER_URL=
# NEXT_PUBLIC_EXPLORER_API_URL=
# NEXT_PUBLIC_YIELD_CHART_MIN_APY=
```

For development, copy it to `.env.local`. A fork commits its own `.env.production` instead; upstream never does.

## Appendix C: GitHub and Next.js documentation used

- Custom workflows for GitHub Pages, and custom domains: https://docs.github.com/en/pages
- Verifying a custom domain for GitHub Pages: https://docs.github.com/en/pages/configuring-a-custom-domain-for-your-github-pages-site/verifying-your-custom-domain-for-github-pages
- GitHub Pages limits: https://docs.github.com/en/pages/getting-started-with-github-pages/github-pages-limits
- GitHub's terms for additional products (Pages): https://docs.github.com/en/site-policy/github-terms/github-terms-for-additional-products-and-features
- Forks within one organization: https://github.blog/changelog/2022-06-27-improved-innersource-collaboration-and-enterprise-fork-policies/
- Next.js static exports: https://nextjs.org/docs/app/guides/static-exports
- Next.js 16 requirements and the removal of `next lint`: https://nextjs.org/docs/app/guides/upgrading/version-16

## Appendix D: Formulas

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

## Appendix E: Reactor box encoding

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
