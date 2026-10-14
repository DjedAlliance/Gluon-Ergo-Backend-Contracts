# Comparison: an SDK repository, a library in the UI, or the SDK in the contracts repository

*Written for the Gluon maintainers on 2026-10-12, and revised on 2026-10-13 to add the third route and the maintainers'
decisions of that day. It compares three
ways of getting the same v3 UI, each described in full by its plan: `SDK_PLAN.md` + `UI_PLAN.md` (route A),
`UI_PLAN_ALTERNATIVE_WITHOUT_SDK.md` (route B) and `UI_PLAN_THIRD_WAY.md` (route C). Figures marked as estimates are my
judgment from reading the plans and the code, not measurements.*

## 1. What is compared

**Route A: an SDK repository, then the UI.** The SDK 2.0.0 is built in its own repository,
`StabilityNexus/Gluon-Ergo-SDK`, by porting the contracts repository's tested `lib/` and its contract tests, and is
published to npm as a new package that the maintainers own, `@stabilitynexus/gluon-ergo-sdk`. The UI depends on it.
(`SDK_PLAN.md` has 8 pull requests, `UI_PLAN.md` 7.)

**Route B: a library inside the UI.** The same code is ported into the UI repository, in `src/lib/gluon/`, behind an
ESLint boundary rule and an enforced `CODEOWNERS`. Nothing is published, and the SDK repository and package are left as
they are. (`UI_PLAN_ALTERNATIVE_WITHOUT_SDK.md` has 10 pull requests.)

**Route C: the SDK in the contracts repository.** The contracts repository's `lib/math.ts`, `reactor.ts`,
`transactions.ts` and `node.ts` move into a package folder, `sdk/`, of that repository, which adds what a front end
needs and publishes the same new package, `@stabilitynexus/gluon-ergo-sdk`, from CI. The UI depends on it, as in route
A. The SDK repository is left as it is. (`UI_PLAN_THIRD_WAY.md` has 7 steps in the contracts repository and 7 in the
UI.)

**Identical in all three routes:**
- the UI: v3 only, mainnet only, no testnet, no CoinGecko, no UI fee, a static export on GitHub Pages, two forks
  (`Gluon-Ergo-UI-Gold`, `Gluon-Ergo-UI-Dollar`) that differ only in `.env.production`, and the legacy UI left alone on
  the `mainnet` branch, on Vercel;
- the library's API, rules and formulas, and the way it is tested: the real contract, executed on Fleet's mock chain;
- what visitors download: Fleet core and serializer, plus the library's own code;
- the first live test, which cannot happen before the first mainnet bootstrap, because there is no testnet.

**What really differs:** where the code that must match the contract lives, how many copies of it exist, who can
change it, and how the UI gets it.

## 2. At a glance

| | A: SDK repository | B: library in the UI | C: SDK in the contracts repository | Best |
|---|---|---|---|---|
| Repositories changed | SDK and UI | UI | contracts and UI | B |
| Steps | 15 (8 + 7) | 10 | 14 (7 + 7) | B |
| Copies of the contract-sensitive code | two: the contracts repository's `lib/` and the SDK's port | two: `lib/` and the UI's port | one: `lib/`'s code becomes the SDK | C |
| Contract tests | ported to the SDK | ported to the UI | the existing 375, where they are | C |
| New engineering (estimate) | the most | a quarter to a third less than A | as much as B or less: nothing is ported | C |
| Release process | npm from a tag, by CI, with no stored token; version bumps in the UI | none | the same as A | B |
| From a recorded deployment to the sites | sync, release, bump, merge into both forks | sync, merge into both forks | tag, bump, merge into both forks (the bootstrap writes the data) | B |
| From a library fix to the sites | release, bump, merge into both forks | merge into both forks | tag, bump, merge into both forks | B |
| Who can change the contract-sensitive code | the SDK repository's contributors | any UI pull request, behind `CODEOWNERS` | the contract's own maintainers | C |
| Weight added to the UI repository | none | `sigmastate-js` (about 13 MB), the compiler, the mock chain and Vitest, for development only | none | A, C |
| Safeguards the UI repository needs | none | the boundary rule and an enforced `CODEOWNERS` | none | A, C |
| Public API under semver | yes | no | yes | B |
| Reuse by bots, other front ends, third parties | from npm, under GPL-3.0 | after an extraction | from npm, under GPL-3.0 | A, C |
| Changes to the contracts repository | none | none | a workspace, a build, a package check, a release workflow; the scripts use the SDK | A, B |
| The SDK repository | gets the v3 line | unchanged | unchanged | A |
| Switching to another route later | to B: cheap; to C: two copies to reconcile | to A: cheap; to C: two copies to reconcile | to A or B: cheap | C |

## 3. Discussion

### 3.1 Work and schedule

The new engineering is the same in all three routes: bundled deployment records, node access that works in browsers,
token-aware input selection, error codes, quotes, the dashboard's snapshot and the client; and, in the UI, the same
work on the swap card, the health panel, Adjust Peg, the dashboard, the charts and GitHub Pages.

What differs is what comes on top of it:
- **A** ports the contracts repository's four modules (about 700 lines) and its contract tests (about 870 lines with
  their helpers) into a new package, and adds the package's tooling, its releases and a sync of the records for every
  deployment.
- **B** ports the same code and tests into the UI, and adds Vitest, the mock chain and the boundary rule to the UI
  repository.
- **C** ports nothing: the code moves with `git mv`, and the 375 tests keep running where they are. It adds a workspace,
  a build configuration, a package check and a release workflow, and it switches the deployment scripts to the SDK's
  functions, so that they share one implementation with the UI.

**Estimate:** B is a quarter to a third less work than A, nearly all of it overhead. C is about as much work as B, or
less: it saves the port of the code and of its tests, and spends part of the saving on package and release machinery
that B does not need. C's build was tried during planning: the four modules compile, unchanged, into a package whose
declarations TypeScript 7.0.2 and 5.9.3 consumers accept.

**Critical path.** All three routes wait for the first mainnet bootstrap. After it:
- **A:** sync the record into the SDK, release, bump the UI, merge into both forks;
- **B:** sync the record into the UI, merge into both forks;
- **C:** the bootstrap itself writes the SDK's data; push a release tag (CI publishes), bump the UI, merge into both
  forks.

**The first live period** has real funds in the reactor, and a defect found then has to reach the sites quickly. B's
path is the shortest: a fix in the UI, merged into both forks. A and C add a release and a version bump; in both, the
release is a tag that CI publishes in minutes.

**Parallel work.** A and C let two people work at once, one per repository, as soon as the API is fixed; the UI's
first steps run against a fake client. B can be organized the same way inside one repository.

**Timing with the bootstraps.** A and B leave the contracts repository alone. C changes it while the first bootstraps
are being prepared: it moves four files and switches three scripts to the SDK's functions. Its plan keeps the scripts'
behavior, runs the 375 tests at every step, and keeps the bootstrap's dry run as the check before any real deployment.
A team that wants no change before the bootstraps can run them first: C works either way, because the bootstrap
records the deployment in `deployments/mainnet.json`, from which the SDK's data is generated later.

### 3.2 Copies of the contract-sensitive code

This is where C differs most.
- **A and B** each make a second copy of the transaction-building code. The contracts repository keeps `lib/` for its
  deployment scripts (bootstrap, Adjust Peg, Update Treasury), and the SDK or the UI gets a port, with a port of the
  contract tests. The v3 contract is immutable, so the copies need not follow a changing contract; but a bug found in
  one of them, in the buyback top-up or the fee outputs for instance, has to be looked for in the other too, and the
  two test suites have to stay equivalent.
- **C** keeps one copy. The deployment scripts, the contract tests and the UI use the same functions, and the contract
  tests exercise exactly the code the UI ships.

The previous version of this comparison called the risk of drift "equal" in A and B. C removes it.

### 3.3 Who can change it

Since October 2024, merge commits excluded:
- **A:** the SDK repository has 24 commits by 7 authors, ten of them from one author.
- **B:** the UI repository has 201 commits by 24 authors, many of them occasional contributors. The plan answers with
  `CODEOWNERS`, which the maintainers have decided to enforce, branch protection that requires a code owner's review,
  and an ESLint rule that stops the library importing the UI. These work, but they are repository settings that
  someone must keep on, and they rely on the owners' reviews.
- **C:** the contracts repository has 107 commits by 4 author addresses, 98 of them from one. The code sits next to the
  contract it must match, in the repository its authors already maintain. The UI holds no contract-sensitive code, so
  neither a casual UI change nor a coding agent working on a UI task can reach it, whatever the repository settings.

### 3.4 Testing

- **A:** the contract tests are ported to the SDK repository; the UI keeps a light setup, `bun test` with a fake client.
- **B:** the contract tests are ported to the UI repository, under Vitest, with `sigmastate-js` and the mock chain as
  development dependencies. Each fork's deploy job repeats them unless it skips them.
- **C:** the contract tests stay with the contract and test the SDK's code from the first step. The UI keeps route A's
  light setup. A package check verifies the published files, the absence of Node-only code, and that a consumer can
  import the package and compile against its types.

In no route can Adjust Peg be tested live before a reactor leaves its healthy range; the mock-chain tests cover it.

### 3.5 Releases and versions

- **A and C** publish the SDK as a new package that the maintainers own, `@stabilitynexus/gluon-ergo-sdk`. Its
  versions start at 2.0.0, following `gluon-ergo-sdk` 1.x, which stays as it is; pre-releases go under the `next`
  dist-tag, and the UI uses exact versions.
- Both publish the first version by hand, because npm can add a trusted publisher only to a package that exists, and
  every later one from a tag, through CI, with npm trusted publishing: no npm token is stored anywhere, npm attaches a
  provenance attestation that links each version to its commit, and a GitHub environment, or npm's staged publishing,
  can require a maintainer's approval before each publication. `NPM_GUIDE.md` sets this up, for either route.
- **B** publishes nothing.

### 3.6 How changes flow

| Event | A | B | C |
|---|---|---|---|
| A new deployment is recorded | contracts → sync in SDK → release → bump in UI → both forks | contracts → sync in UI → both forks | contracts (the bootstrap writes the SDK's data) → tag → bump in UI → both forks |
| A bug in the library | SDK → release → bump in UI → both forks | UI → both forks | contracts → tag → bump in UI → both forks |
| A UI change | UI → both forks | UI → both forks | UI → both forks |
| A bug in a deployment script's transaction code | contracts, and the SDK's copy may need the same fix | contracts, and the UI's copy may need the same fix | contracts: one fix serves the scripts and the UI |

### 3.7 API stability

A and C publish an API under semver: about twenty exported names and fifteen methods on the client, the same in both.
A mistake costs a major version or a workaround. B can change its API at will. In C, only the API of the plan is
public: the scripts and tests use internal functions from the source, which can change freely.

### 3.8 Reuse and licensing

The existing `gluon-ergo-sdk` had 28 downloads in the last month and 3 in the last week (npm's API, 2026-10-12), so the
demand for reuse is unproven. A and C keep the door open at no extra cost; B needs an extraction first.

The package is GPL-3.0 in A and C, as decided. On the usual reading of the GPL (this is not legal advice), software
that includes it and is distributed must be GPL-compatible. In C, the contracts repository then holds a GPL-3.0 folder,
`sdk/`, while the rest of the repository has no license file (decision D4 of its cleanup); the maintainers have
accepted this.

### 3.9 The UI repository and the forks

A and C add only a dependency to the UI repository. B adds development dependencies: `sigmastate-js` (about 13 MB
unpacked), the compiler and the mock chain, and Vitest (about 3 MB). None is in the site's bundle, but installs and CI
are slower, in the forks too. The bundle is the same in all three routes: Fleet core (about 440 KB unpacked) and
serializer (about 310 KB), plus the library's code.

Every change reaches the forks by a merge from upstream, in all three routes. A and C add merges that only bump the
SDK, and a fork left on an older SDK is a risk that B does not have.

### 3.10 Security and supply chain

The contract validates every transaction, so a library bug mostly produces a transaction the chain rejects, not a
loss. The exceptions are what the contract does not constrain: where the released tokens and the change go. All three
plans test them.
- **A and C:** a package published by OIDC from a tag, behind an approval and with provenance, and no stored token.
  In C, the release workflow runs in the repository that deploys reactors, but it can only publish the package:
  deployments are signed with a mnemonic that stays on a maintainer's machine.
- **B:** no publishing surface. The exposure is the write access of the UI's many contributors, which `CODEOWNERS`
  addresses.

### 3.11 What could go wrong

| | A | B | C |
|---|---|---|---|
| 1 | The API is wrong on release, and the UI works around it or forces a major version | A casual change reaches fee or transaction code despite the safeguards | The refactoring of the contracts repository changes something its tests miss, just before a bootstrap |
| 2 | A forgotten bump leaves a fork on an old SDK | The UI repository's CI and installs get heavier and slower | A forgotten bump leaves a fork on an old SDK |
| 3 | The SDK's copy of the code and the scripts' copy diverge | The UI's copy and the scripts' copy diverge | SDK releases get mixed up with deployment work in one repository |
| 4 | Maintainer time is split across two repositories | The library gets entangled with the UI, and extraction becomes expensive | The contracts repository's maintainers become a bottleneck for SDK changes that the UI needs |

## 4. The cost of changing your mind

- **From C to A or B, later:** mechanical. Move `sdk/` into the SDK repository, with its history (`git filter-repo`),
  or copy it into the UI. The deployment scripts then depend on the published package or keep a copy, which brings back
  the duplicate.
- **From A or B to C, later:** harder than it looks. By then the port and the contracts repository's `lib/` have
  diverged, on purpose (the port adapts the code) and through fixes, and they must be reconciled before one replaces
  the other.
- **Between A and B:** cheap either way, as before. B to A means extracting `src/lib/gluon/` and adding package tooling;
  A to B means copying the SDK's source into the UI.

So C is the cheapest route to leave later, and the most expensive one to reach later.

## 5. Recommendation

**What changed.** The previous version of this comparison recommended B, and set C aside in a short paragraph. Planning
C in full showed that its cost, a build and releases in the contracts repository, is small and was partly tried during
planning, and that it removes the one weakness A and B share: a second copy of the code that must match the contract.
Since then, the maintainers have decided that the SDK will be a new npm package that they own, and have accepted a
GPL-3.0 `sdk/` folder in the contracts repository: both settle questions that C depended on.

**How to decide.**
1. Will the contracts repository's maintainers own the SDK and its releases? If yes, choose C.
2. If not, is there a separate owner for an SDK repository, or a named outside user of the SDK? If yes, choose A.
3. Otherwise, choose B, with `CODEOWNERS` enforced, as decided.

**My recommendation is C.** The reasons:
- there is one copy of the contract-sensitive code, tested next to the contract by the 375 tests that already exist,
  and used alike by the deployment scripts and the UI;
- it needs the least new engineering, because nothing is ported;
- the UI repository stays light and holds no contract-sensitive code, so its many contributors cannot change fee or
  transaction code, whatever the repository settings;
- a v3 SDK exists for anyone who wants it, without a third repository to maintain;
- leaving C later is mechanical, while reaching it later means reconciling two copies.

Its costs, and how its plan handles them:
- from a library fix to the sites there is one more hop than in B: a tag, which CI turns into a publication, and a
  version bump in the UI;
- the contracts repository becomes the home of a package too: a workspace, a GPL-3.0 `sdk/` folder in an otherwise
  unlicensed repository (accepted), and a release workflow;
- the refactoring touches the deployment scripts: the plan keeps their behavior, tests every step, and can wait until
  after the first bootstraps.

**What would change my mind:** the maintainers want the contracts repository to stay a deployment tool and nothing more
(then B); or a separate person will own an SDK repository (then A).

**Caveats.** The effort figures are estimates. I wrote much of the contracts repository's tooling in this project,
which could bias me toward reusing it; the arguments about copies and contributors above do not depend on who wrote the
code.
