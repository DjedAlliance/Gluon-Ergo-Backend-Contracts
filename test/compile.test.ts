import { SigmaCompiler$, Value$ } from "sigmastate-js-next/main";
import { describe, expect, it } from "vitest";
import { CONTRACT_SOURCE, compileGluon, namedConstants } from "../lib/compile.ts";
import { PROFILES, constantsFor } from "./helpers.ts";

const hex = (bytes: Uint8Array) => Buffer.from(bytes).toString("hex");

describe.each(PROFILES)("Compilation ($name)", (profile) => {
  const tree = compileGluon(constantsFor(profile));

  it("produces an ErgoTree of version 0 with segregated constants, like the deployed reactors", () => {
    expect(tree.version).toBe(0);
    expect(tree.hasSegregatedConstants).toBe(true);
    expect(tree.toHex().slice(0, 2)).toBe("10");
  });

  it("compiles to the same bytes with the Sigma 6.0.7 compiler (sigmastate-js 0.6.3)", () => {
    // Fleet SDK compiles with sigmastate-js 0.4.6. Newer compilers check types more strictly; this test
    // fails if the contract stops compiling with them or if they compile it differently.
    const named = Object.fromEntries(
      Object.entries(namedConstants(constantsFor(profile))).map(([name, value]) => [name, Value$.fromHex(value.toHex())]),
    );
    const next = SigmaCompiler$.forMainnet().compile(named, true, 0, CONTRACT_SOURCE);
    expect(next.toHex()).toBe(tree.toHex());
  });
});

it("gives every deployment the same template, so one template hash identifies all v3 reactors", () => {
  const [gold, dollar] = PROFILES.map((p) => compileGluon(constantsFor(p)));
  expect(hex(gold.template)).toBe(hex(dollar.template));
  expect(gold.toHex()).not.toBe(dollar.toHex());
});
