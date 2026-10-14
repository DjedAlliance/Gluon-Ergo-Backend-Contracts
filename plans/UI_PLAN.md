# Gluon-Ergo-UI: plan for a v3-only static UI on GitHub Pages (on the SDK 2.0.0, `@stabilitynexus/gluon-ergo-sdk`)

*Plan for the maintainers of [StabilityNexus/Gluon-Ergo-UI](https://github.com/StabilityNexus/Gluon-Ergo-UI), written
on 2026-10-12 against its `main` at `e4e30ab`. It is meant to be executed step by step by a developer or a coding
agent. It depends on the SDK 2.0.0, **`@stabilitynexus/gluon-ergo-sdk`**, which `SDK_PLAN.md` specifies; read that
plan's §2 (background) and §5.3 (API) first. `UI_PLAN_ALTERNATIVE_WITHOUT_SDK.md` and `UI_PLAN_THIRD_WAY.md` are
variants of this plan that do without a separate SDK repository, and `comparison.md` compares the three routes.*

## 0. Scope, and how to use this plan

**Scope.** This plan creates a new version of the UI that works with the Gluon **v3** contract and the **SDK 2.0.0**
(`@stabilitynexus/gluon-ergo-sdk`), as a **static, client-side site hosted on GitHub Pages**. Gluon Gold v3 and Gluon
Dollar v3 each get their own deployment, in their own fork of the repository (§3), and the two forks differ only in
their env/config files.

- The new version knows **v3 deployments and SDK 2.0.0 only**, on **Ergo mainnet only**. It has no v2 code path, no
  support for SDK 1.x and no testnet support.
- **The legacy UI is the `mainnet` branch.** It is preserved, not changed, and keeps being deployed to Vercel by its
  existing project. Nothing in this plan fixes or improves it.
- **No server code, no secrets and no price feed.** The CoinGecko-based price, and everything that depends on it, is
  removed (§5.1, UD9).
- **No UI fee.** The sites charge no interface fee (§5.1, UD10).
- Where this plan describes what the current code does, it says what the new version replaces. It never says what to
  change in the legacy UI.

**How to use this plan.**
- Do Step 0, then the pull requests of §6 in order. They go into `main` of the upstream repository. Each lists what to
  do and when it is done. §6, PR 7 creates the two forks.
- **Prerequisites.**
  - SDK 2.0.0 pre-releases (`SDK_PLAN.md` §7). The UI can be built and tested against a fake client with any
    pre-release; it can only run live once a pre-release bundles a v3 deployment, which needs the first mainnet
    bootstrap of the contracts repository.
  - The current code, tagged `pre-v3` in Step 0, is the starting point for porting (§6).
- **References:**
  - `SDK_PLAN.md`: the API (`GluonClient`, `ChainState`, `ProtocolSnapshot`, `ActionQuote`, `GluonError` codes);
  - the contracts repository
    [DjedAlliance/Gluon-Ergo-Backend-Contracts](https://github.com/DjedAlliance/Gluon-Ergo-Backend-Contracts): its
    README ("The Contract") and `Deployments.md`.
- Line references are to `main` at `e4e30ab`.

## 1. Summary

New Gluon reactors run contract v3: Gluon Gold, pegged to one gram of gold, and Gluon Dollar, pegged to one US dollar.
v3 adds two things users will see:
- a **healthy range** for the fusion ratio: outside it, fission, fusion and transmutations are refused;
- **Adjust Peg**, a transaction anyone can submit while the reactor is outside its healthy range. It moves the peg
  factor by 1 % per transaction until the reactor is healthy again, and the neutron's target price is the oracle price
  times the current peg factor.

The new UI:
- is built on SDK 2.0.0, with one SDK client for the one deployment each site serves;
- takes every id, decimal, ticker and unit from that deployment;
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

  Where this plan says **port**, take the file from `pre-v3` (`git show pre-v3:<path>`): it is the newest version of the
  same design as `mainnet`.
- If GitHub Pages is enabled on the upstream repository, disable it (Settings → Pages). Upstream does not deploy.

## 3. The two deployments

| Deployment | Serves | Repository (a fork of upstream) | Site |
|---|---|---|---|
| Gold | `gluon-gold-v3` | `StabilityNexus/Gluon-Ergo-UI-Gold` | https://gold.gluon.stability.nexus/ |
| Dollar | `gluon-dollar-v3` | `StabilityNexus/Gluon-Ergo-UI-Dollar` | https://dollar.gluon.stability.nexus/ |

(Without a custom domain, a site would be at `https://stabilitynexus.github.io/<repository>/` and would need
`NEXT_PUBLIC_BASE_PATH`; the build supports that for previews and as a fallback.)

GitHub Pages publishes one site per repository, so each deployment has a repository of its own. They work like this:

- **Upstream** is `StabilityNexus/Gluon-Ergo-UI`. It holds the shared code on `main` and the legacy UI on `mainnet`. It
  has no deployment configuration and does not publish a site.
- **Each fork** is a fork of upstream. It has the same code as upstream `main`, plus **one file of its own: the
  committed `.env.production`**. That file, and the repository's Pages settings, are the only differences between the
  Gold fork and the Dollar fork. Changes to the code are made in upstream and merged into both forks (§8.8).
- **One build serves exactly one deployment**, chosen by `NEXT_PUBLIC_GLUON_DEPLOYMENT`. There is no selector and no
  code for several deployments in one site.
- A later v3 reactor (another peg) is added by: a new SDK release that bundles its record, an entry in the UI's
  presentation config (§5.4), and a new fork (§8.3).

## 4. Starting point: what is reused and what is replaced

The new version keeps the current app's layout, components and charts. What changes is the data layer, which assumes
the v2 Gold reactor throughout, and the hosting model. This table says what the current code does and what replaces
it; none of it is a fix list for the legacy UI.

| Area | Current code (`main`) | New version |
|---|---|---|
| SDK | `gluon-ergo-sdk` 1.x; `new sdk.Gluon()` in 7 places, each with the SDK's built-in reactor | `@stabilitynexus/gluon-ergo-sdk` 2.0.0; one `GluonClient` in `GluonProvider` |
| Reactor and token ids | hard-coded in `lib/constants/token.ts` and 6 other files | `deployment.tokens` |
| Units and labels | oracle price ÷ 1000, a `0.66` cushion, "GAU"/"GAUC" literals | `ProtocolSnapshot` fields; tickers from the token names |
| Amounts | JavaScript numbers (`Number(...)`) | `bigint`, with `amounts.ts` |
| Wallet UTXOs | all passed to the SDK | the SDK selects what it needs |
| Environment | `NEXT_PUBLIC_DEPLOYMENT` (a network), two node variables, about 25 presentation variables, a server-side API key | §5.3: a few `NEXT_PUBLIC_*` variables in `.env.production`; presentation in code; no secrets |
| USD price of ERG | `/api/getErgPrice` (CoinGecko, with an API key), used by `MyStats`, `GluonStats` and `FeeBreakdownPanel` | none: removed (UD9) |
| Server code | API routes in `src/pages/api/`, and a CSP set by `headers()` in `next.config.ts` | none: a static export (§5.2) |
| History and fee charts | assume a `Long` R9, two contract addresses and only four kinds of transaction | v3 records; Adjust Peg and Update Treasury transitions |
| wasm | `ergo-lib-wasm-*`, used by `NodeService.getCtx` | none |
| Hosting | `vercel.json` (npm), and a GitHub starter workflow for Pages that looks for `bun.lockb` while the repository has `bun.lock` | the workflows of §8.4 |
| Site identity | hard-coded `https://gluon.fi` (canonical link) and `https://www.gluon.gold/` (SEO tags) | `NEXT_PUBLIC_SITE_URL` |
| Tests | none | `bun test`, with a fake SDK client |

**Reused as it is:** the layout, navigation and theme machinery, UI primitives and icons, `ErgoProvider`, the wallet
connector, the home page, the `resolveAssetPath` helper, IndexedDB and transaction-listener utilities, and the
presentation code of the charts.

## 5. Target design

### 5.1 Decisions

- **UD1. One build, one deployment.** `NEXT_PUBLIC_GLUON_DEPLOYMENT` names a deployment bundled in the SDK. Nothing in
  the UI chooses between deployments.
- **UD2. One SDK client, in a React context.** `GluonProvider` owns the client and the latest chain state and snapshot.
  It polls every 30 s while the tab is visible, refreshes on demand and after every submitted transaction, and keeps
  the last good state while a refresh fails. Components read it with `useGluon()`; nothing else instantiates the SDK.
- **UD3. Everything on-chain comes from the deployment:** token ids, decimals, tickers (the token's EIP-4 name),
  contract and oracle. Presentation (colors, labels) is in code, keyed by deployment name (§5.4).
- **UD4. The SDK computes; the UI formats.** Quotes and transactions come from `client.quote*` and `client.build*`.
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
- **UD10. No UI fee.** The contract allows an optional 0.4 % fee for the interface that builds a transaction. These
  sites charge none: the UI creates its SDK client without `uiFeeAddress`, so quotes have `fees.ui = 0` and no
  transaction has an interface-fee output. There is no fee address to configure.

### 5.2 Changes that make the site static

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
   `"typecheck": "tsc --noEmit"`, `"lint": "eslint ."` (Next.js 16 removed `next lint`), `"test": "bun test"`.
9. Delete `vercel.json` and `.github/workflows/nextjs.yml`.

### 5.3 Configuration

The configuration is the `.env.production` file of each fork. Every value is public: it ends up in the browser bundle.
There are no secrets.

| Variable | Required | Meaning |
|---|---|---|
| `NEXT_PUBLIC_GLUON_DEPLOYMENT` | yes | The deployment this site serves, e.g. `gluon-gold-v3`. Must be a deployment of the SDK's `DEPLOYMENTS`. |
| `NEXT_PUBLIC_NODE_URL` | yes | HTTPS URL of an Ergo node that serves the blockchain index API (`/blockchain/…`) and allows browser requests (CORS). `http://localhost…` is accepted for development. |
| `NEXT_PUBLIC_SITE_URL` | recommended | The site's public URL, with its base path and a trailing slash, e.g. `https://gold.gluon.stability.nexus/`. Used for the canonical link and the social tags. |
| `NEXT_PUBLIC_BASE_PATH` | no | `/<repository name>` when the site is served from `https://<owner>.github.io/<repository>/`. Empty (the default, and the setting of both sites) when it has its own domain. |
| `NEXT_PUBLIC_EXPLORER_URL` | no | Base URL for transaction and token links. Default `https://explorer.ergoplatform.com`; links are `…/en/transactions/<id>`. |
| `NEXT_PUBLIC_EXPLORER_API_URL` | no | Explorer API for the history charts. Default `https://api.ergoplatform.com/api/v1`. |
| `NEXT_PUBLIC_YIELD_CHART_MIN_APY` | no | Threshold of the yield chart (`GaucYieldChart.tsx:209`); unchanged. |

Upstream has `.env.example`, with the same variables, and no `.env.production` (Appendix B). Next.js loads
`.env.production` automatically during `next build`; for development, copy `.env.example` to `.env.local`.

**Validation.** `src/config/env.ts` parses and validates the variables when it is imported, and throws
`Gluon UI configuration error: …` with a message that names the variable: the deployment name exists in the SDK; the
node URL is HTTPS (or localhost); `NEXT_PUBLIC_BASE_PATH` is empty or starts with `/` and does not end with one.
`pages/_app.tsx` imports it, so `next build` fails when the configuration is invalid, because every page is
pre-rendered. At build time it also logs `Gluon UI: <deployment>, node <url>`.

### 5.4 Presentation

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

### 5.5 Modules

| Module | Role |
|---|---|
| `src/config/env.ts` (new) | §5.3 |
| `src/config/presentation.ts` (new), `src/config/tokenConfig.ts` (rebuilt) | §5.4 |
| `src/lib/providers/GluonProvider.tsx` (new) | The context of UD2: `{ deployment, presentation, client, chain?, snapshot?, status: "loading" \| "ready" \| "error", error?: GluonError, refresh() }` |
| `src/lib/hooks/useGluon.ts` (new) | `useGluon()` reads the context |
| `src/lib/components/blockchain/reactor/ReactorHealth.tsx` (new) | Fusion ratio, healthy band, peg factor and drift, oracle age (§5.6) |
| `src/lib/components/blockchain/reactor/AdjustPegPanel.tsx` (new) | The Adjust Peg flow (§5.6) |
| `src/lib/components/blockchain/reactor/NetworkGuard.tsx` (new) | The wallet-network check (§5.6) |
| `src/lib/utils/amounts.ts` (new) | `toBaseUnits(text, decimals): bigint` and `formatUnits(value, decimals): string`, without floating point |
| `src/lib/utils/error-handler.ts` | Maps `GluonError.code` to the messages of §5.7 |

### 5.6 User experience

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
actions. The SDK also throws `INVALID_ADDRESS` for such an address.

**No UI fee (UD10).** The client is created without `uiFeeAddress`, and the fee breakdown has no interface-fee row.

### 5.7 Error messages

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
`bun test` and `bun run build` before merging each one. Where a step says **port**, start from the file on `pre-v3`
(`git show pre-v3:<path> > <path>`) and change its data layer; the presentation code usually stays.

### PR 1: Foundation

1. **Dependencies.** Add `@stabilitynexus/gluon-ergo-sdk` at an exact `2.0.0-next.N`. Remove `gluon-ergo-sdk` 1.x,
   `ergo-lib-wasm-browser`, `ergo-lib-wasm-nodejs` and `@fleet-sdk/core` (nothing in `src` uses it directly). Add
   `.nvmrc` with `24` and `"engines": { "node": ">=24" }`. Regenerate `bun.lock`.
2. **Static export.** Apply §5.2: `next.config.ts`, the deletion of `src/pages/api/`, the CSP meta tag, asset paths,
   site identity, `public/.nojekyll`, `404.tsx`, the scripts, and the deletion of `vercel.json` and `nextjs.yml`.
3. **Remove what depends on the SDK 1.x API, and the CoinGecko features.** The SDK's API changes completely, so delete
   every file that imports `gluon-ergo-sdk`, `ergo-lib-wasm-*` or the two `useGluon*` hooks, and the routes that render
   them. They are ported back in PRs 2 to 5 (as of `e4e30ab`), except `MyStats.tsx`, which is removed for good (UD9):
   - swap: `components/blockchain/swap/*`, `functions/reactor/*`, `pages/swap/index.tsx`;
   - dashboard: `components/blocks/dashboard/*`, `pages/reactor/index.tsx`;
   - history and fees: `hooks/useGluonFeeBreakdown.ts`, `hooks/useGluonTransactionHistory.ts`.

   Hide the navigation entries of those pages (`TopNavbar.tsx`, `BottomNavbar.tsx`, `ReactorSidebar.tsx`) until their
   PR. `pages/history` (the user's own transactions, from IndexedDB) does not depend on the SDK and stays.
4. **Configuration.** Add `src/config/env.ts` (§5.3) and `src/config/presentation.ts` (§5.4), import `env.ts` in
   `pages/_app.tsx`, and rebuild `src/config/tokenConfig.ts`. Delete `.env.gold` and `.env.dollar`; rewrite
   `.env.example` (Appendix B); in `.gitignore`, drop the exceptions for the two deleted files and keep `.env*` ignored
   (the forks add `.env.production` with `git add -f`, §8.3).
5. **Provider and utilities.** Add `GluonProvider` and `useGluon` (§5.5) and mount the provider in
   `lib/providers/Providers.tsx`. Add `amounts.ts`. In `lib/constants/token.ts`, keep `ACTION_TYPES` and remove
   `TOKEN_ADDRESS`; `erg-converter.ts` takes decimals from the deployment. Rewrite `lib/utils/node-service.ts` without
   wasm, keeping what the transaction listener uses (height, unconfirmed-transaction lookups).
6. **CI and publishing.** Add `.github/workflows/ci.yml` and `.github/workflows/pages.yml` as in §8.4, with the actions
   pinned by commit SHA. Add `bun run build` to `ci.yml` as soon as the SDK bundles a deployment.
7. **Test harness.** `bunfig.toml` with `[test] preload = ["./test/setup.ts"]`, where `test/setup.ts` registers jsdom's
   window and document as globals (Bun's runner has no DOM). `test/fakeClient.ts` implements the SDK's `GluonClient`
   surface (`SDK_PLAN.md` §5.3) with fixed `ChainState`, snapshots and quotes, and records the calls it receives.
8. **Tests.** `env.ts`: valid and invalid deployment names, a non-HTTPS node, a malformed base path. `GluonProvider`,
   with the fake client: polling, keeping the last good state on error, pausing while the tab is hidden. `amounts.ts`:
   round trips for 0, 3 and 9 decimals and for values above 2⁵³.

**Done when** the checks above pass; `bun run build` writes `out/` (once a deployment is bundled); and the exported
home page renders with the configured deployment's presentation and tickers.

### PR 2: The swap card

**Port** `ReactorSwap.tsx`, `FeeBreakdownPanel.tsx`, `functions/reactor/*` and `pages/swap/index.tsx`.

1. `ReactorSwap` takes `client`, `chain` and `snapshot` from `useGluon()`; it initializes nothing.
2. Each `calculate*Amounts` becomes a call to `client.quote*`: parse input with `toBaseUnits(text, decimals)` using the
   token's `decimals` from the deployment, format with `formatUnits`, and never convert an amount to `number`. Fusion's
   maximum ERG comes from the wallet's balances and `quoteFusion` (take the token ratio for 1 ERG, then verify the
   result with `quoteFusion`).
3. Each `handle*Swap` becomes: `refresh()`; `client.build*(chain, { …, userBoxes: await getUtxos(), changeAddress: await
   getChangeAddress() })`, both from `useErgo()` (`ErgoProvider.tsx:330-349,378-388`); `sign_tx`, then `submit_tx`;
   `refresh()`. The SDK selects the inputs it needs.
4. Token ids, decimals and tickers come from `deployment.tokens`: `defaultTokens` (`utils.ts`) becomes a function of the
   deployment; drop the `gau`/`gauc` legacy fields. `ACTION_TYPES` stays, keyed by `pegAsset`.
5. Health gating (§5.6): disable the controls per the state table; MAX for transmutations = min(balance,
   `maxBetaDecayPlus/Minus`).
6. The fee panel shows `quote.fees` (dev, oracle, `outputsMinFee`, miner) **in ERG only**: drop the `ergPriceUSD`
   property and the USD amounts, and show no interface fee (UD10).
7. `error-handler.ts` maps the codes of §5.7; explorer links use `…/en/transactions/<id>`.
8. Tests, with the fake client: each state of §5.6 enables and disables the right controls; each `handle*Swap` calls
   `build*` with the wallet's UTXOs and change address; the MAX buttons.

**Done when**, on the first mainnet deployment (§7.2), a small fission and fusion succeed from the UI. Until a
deployment exists, the fake-client tests are the check.

### PR 3: Reactor health, Adjust Peg and the network guard

1. `ReactorHealth`, `AdjustPegPanel` and `NetworkGuard` (§5.6), on the swap page; `ReactorHealth` also on the reactor
   dashboard when PR 4 adds it.
2. Tests, with the fake client: a healthy reactor shows no panel; an unhealthy one shows the direction and steps; one
   step calls `buildAdjustPeg`, signs, submits, and waits for the new reactor box; `REACTOR_HEALTHY` after a step ends
   the flow; a wallet rejection leaves the panel usable; a non-mainnet address blocks actions.

**Done when** those tests pass. Adjust Peg can only be run live when a real reactor leaves its healthy range (§7.2).

### PR 4: Dashboard

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

### PR 5: History and fee charts

**Port** `useGluonTransactionHistory.ts` and `useGluonFeeBreakdown.ts`, and the charts that use them.

1. **Parameterize by the deployment:** the reactor NFT, token ids and decimals, and the oracle pool NFT. A v3 reactor
   keeps one contract and one NFT for its whole life, so there is no address lineage and no migration handling.
2. **Explorer API** from `NEXT_PUBLIC_EXPLORER_API_URL` (or its default). Include the deployment's name in the cache
   keys (for example `gluon_box_history_v10:<deployment>`), so that deployments served from one origin, such as
   `localhost` in development, do not share caches.
3. **Decode** explorer boxes into the node's box shape (registers as plain hex strings), then use the SDK's pure
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

### PR 6: Documentation and clean-up

1. `README.md`: what the app is; the configuration (§5.3); running locally (`.env.local`); the two deployments and how
   the forks relate to upstream (§3, §8); Adjust Peg in a few sentences. Replace the badge link, which points to another
   project, with the addresses of the two sites. No CoinGecko step.
2. `docs/github-pages.md`: §8 of this plan, with the final repository names and domains.
3. Check that nothing of the old version is left: `src` has no literal token tickers, no `0.66`, no kg-to-gram `÷ 1000`,
   no `gluon-gold-v2`, no `COINGECKO`, no `uiFeeAddress`, no import from `gluon-ergo-sdk` (1.x), no
   `NEXT_PUBLIC_DEPLOYMENT` or `NEXT_PUBLIC_NODE` (without `_URL`), no `/api/` call, and `package.json` has no unused
   dependency.
4. Run the static checks of §7.1 locally.

### PR 7: The two deployments

Done by the maintainers once SDK 2.0.0 is released with both records (or with the record of the deployment being
published first):

1. In upstream, bump `@stabilitynexus/gluon-ergo-sdk` to `2.0.0` (exact) and merge.
2. Create the forks and publish the sites: §8.3 and §8.5, once per fork.
3. Verify each site: §8.7.

## 7. Test plan

### 7.1 Automated and static checks

- The tests listed in PRs 1 to 5, run by `bun test` against the fake client, and checked by CI.
- **The exported site, locally:** with a valid `.env.local`, run `bun run build`, then serve `out/` with any static
  server (for example `bunx serve out`) and open it. Check that the home page, `/swap/`, `/reactor/` and `/history/`
  load, that a direct visit to a deep link works, and that an unknown URL shows the 404 page.
- **A base path:** build with `NEXT_PUBLIC_BASE_PATH=/test`, copy `out/` to a folder named `test` inside an empty
  directory, serve that directory, and check that the logo, favicon, manifest and chunks load under `/test/`.
- **No server code:** `out/` contains only files, and nothing in `src` calls `/api/`.

### 7.2 Mainnet checks

There is no testnet. The first mainnet bootstrap, with a small seed, is the first live test; after it, with the
deployment recorded and bundled, and with small amounts:

1. A fission, a fusion and both transmutations. Check the received amounts against the quote, and the fee outputs in the
   explorer.
2. The MAX buttons, including a transmutation capped by the healthy range.
3. The dashboard and the charts.
4. The wallet-network banner, with a wallet that is not on mainnet.

**Adjust Peg and oracle staleness** can only be seen when they actually happen: the oracle price must move by tens of
percent for a reactor to leave its healthy range, and the oracle must miss updates to become stale. Until then, the
fake-client tests are their check here, and the SDK's mock-chain tests cover the transactions themselves.

## 8. GitHub Pages guide

### 8.1 How the sites are published

Upstream holds the code; each fork builds and publishes one site (§3). A push to a fork's `main` runs the workflow of
§8.4: it type-checks, tests, builds the static site with the fork's `.env.production`, and deploys it to the fork's
GitHub Pages site. The workflow does nothing in upstream.

### 8.2 Before you start

1. **Rights.** You can create repositories in the `StabilityNexus` organization and change their settings.
2. **The code.** Upstream `main` has PRs 1 to 6 merged and depends on `@stabilitynexus/gluon-ergo-sdk` 2.0.0, which
   bundles the deployment you are publishing. This prints its address, from a checkout of upstream with `bun install`
   done:

   ```sh
   node -e "import('@stabilitynexus/gluon-ergo-sdk').then(s => console.log(s.getDeployment('gluon-gold-v3').address))"
   ```
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
      - run: bun test
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
      - run: bun test
      - run: bun run build
```

Notes:
- The `build` job is skipped in upstream, so `deploy`, which needs it, is skipped too.
- Pushes to a fork run both workflows. The CI check also builds, with the fork's own `.env.production`.
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
# 2. Browsers may call the node. The SDK makes one cross-origin POST with a JSON body, which browsers preflight:
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
7. The mainnet checks of §7.2, with small amounts.

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
  first.
- After an SDK bump, merge into **both** forks and verify each site (§8.7).
- When upstream adds a required variable, add it to each fork's `.env.production`; until then the fork's build fails.

### 8.9 Day-two operations

- **Change a fork's configuration:** edit `.env.production` and push.
- **Roll back:** `git revert` the offending merge in the fork and push. The workflow publishes the reverted state.
- **Change a site's domain:** Settings → Pages → Custom domain, the DNS record, and `NEXT_PUBLIC_SITE_URL`.
- **Add a deployment (another v3 reactor):** its record in the SDK, an entry in `presentation.ts`, a new fork (§8.3).
- **Retire a site:** disable Pages in the fork (Settings → Pages) and archive the repository.

### 8.10 Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| The workflow does not run in a fork | Workflows are disabled in new forks: enable them (Actions tab). |
| `pages.yml` shows both jobs skipped | The repository is upstream, or its name is the one in the `if:` condition. |
| The `deploy` job fails | Settings → Pages → Source must be **GitHub Actions**; and the `github-pages` environment must allow `main`. |
| The build fails with `Gluon UI configuration error: …` | `.env.production` is missing or invalid: the message names the variable. |
| The site loads but images, styles or scripts return 404 | The base path is wrong: set `NEXT_PUBLIC_BASE_PATH=/<repository>` for a `github.io/<repository>/` address, or leave it empty for a custom domain. |
| A direct visit to a page returns 404 | `trailingSlash: true` and the exported `404.html` are required (§5.2); check that `out/swap/index.html` exists. |
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
- The code has no v2 path, no use of SDK 1.x, no testnet support, no server code, no CoinGecko dependency and no UI fee
  (PR 6, step 3).

## 10. Decisions and open items

**Decided by the maintainers (2026-10-12).**
1. The forks are `StabilityNexus/Gluon-Ergo-UI-Gold` and `StabilityNexus/Gluon-Ergo-UI-Dollar`.
2. The sites are https://gold.gluon.stability.nexus/ and https://dollar.gluon.stability.nexus/.
3. There is no UI fee (UD10).
4. GitHub Pages is acceptable for these sites (§8.11).
5. Porting starts from `pre-v3` (`main` at `e4e30ab`), the newest version of the legacy design.
6. Proton leverage is shown as `q / (1 − q)`, as in the legacy UI (`SDK_PLAN.md`, Appendix A).

**Decided by the maintainers (2026-10-13).**
1. The SDK is a new npm package that the maintainers own, `@stabilitynexus/gluon-ergo-sdk` (`NPM_GUIDE.md`); the old
   `gluon-ergo-sdk` keeps 1.x.
2. The maintainers control the DNS of `stability.nexus`, for the `CNAME` and `TXT` records of §8.3, step 7.
3. The sites use `https://node.sigmaspace.io` as their node.

Nothing is left to confirm.

## Appendix A: File checklist

| Path | Action | PR |
|---|---|---|
| `package.json`, `bun.lock` | SDK 2.0.0 (pre-release, then 2.0.0); remove SDK 1.x, ergo-lib-wasm, `@fleet-sdk/core`; `engines`; scripts | 1 |
| `.nvmrc` | new (`24`) | 1 |
| `next.config.ts` | static export (§5.2) | 1 |
| `vercel.json`, `.github/workflows/nextjs.yml` | delete | 1 |
| `.github/workflows/ci.yml`, `pages.yml` | new (§8.4) | 1 |
| `.env.gold`, `.env.dollar` | delete (values move to `presentation.ts`) | 1 |
| `.env.example`, `.gitignore` | rewrite; drop the two exceptions | 1 |
| `src/pages/api/*` | delete | 1 |
| `src/pages/_document.tsx`, `src/lib/components/layout/SEO.tsx`, `public/manifest.json` | CSP meta, asset paths, site identity | 1 |
| `public/.nojekyll`, `src/pages/404.tsx` | new | 1 |
| `src/config/env.ts`, `src/config/presentation.ts` | new | 1 |
| `src/config/tokenConfig.ts` | rebuilt from deployment and presentation | 1 |
| `src/lib/providers/GluonProvider.tsx`, `src/lib/hooks/useGluon.ts` | new | 1 |
| `src/lib/providers/Providers.tsx` | mount `GluonProvider` | 1 |
| `src/lib/utils/amounts.ts`, `erg-converter.ts` | new; decimals from the deployment | 1 |
| `src/lib/utils/node-service.ts` | rewrite without wasm | 1 |
| `src/lib/constants/token.ts` | keep `ACTION_TYPES`; remove `TOKEN_ADDRESS` | 1 |
| `bunfig.toml`, `test/setup.ts`, `test/fakeClient.ts` | new | 1 |
| files that use the SDK 1.x API (PR 1, step 3) | delete in PR 1, then port | 1, 2–5 |
| `src/lib/components/blocks/dashboard/MyStats.tsx` | delete, not ported (UD9) | 1 |
| `components/blockchain/reactor/ReactorHealth.tsx`, `AdjustPegPanel.tsx`, `NetworkGuard.tsx` | new | 3 |
| `README.md`, `docs/github-pages.md` | rewrite; new | 6 |
| forks: `.env.production` | new, one per fork | 7 |

## Appendix B: `.env.example`

```sh
# The deployment this site serves: a v3 deployment bundled in @stabilitynexus/gluon-ergo-sdk.
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
