// Compiles the contract with the constants of a deployment config and prints the contract it yields.
// Usage: node scripts/compile.ts <config file>
import { parseArgs } from "node:util";
import { run } from "../lib/cli.ts";
import { compileGluon } from "../lib/compile.ts";
import { contractConstants, contractSource, readDeployment } from "../lib/config.ts";
import { templateHashes } from "../lib/records.ts";

run(async () => {
  const { positionals } = parseArgs({ allowPositionals: true });
  if (positionals.length !== 1) throw new Error("usage: node scripts/compile.ts <config file>");
  const config = readDeployment(positionals[0]);
  const constants = contractConstants(config);
  const tree = compileGluon(constants, config.network, contractSource(config));
  const hashes = templateHashes(tree);
  console.log(`Deployment         ${config.deployment} (${config.network})
ErgoTree           version ${tree.version}, ${tree.toHex().length / 2} bytes
Template SHA-256   ${hashes.sha256}
Template BLAKE2b   ${hashes.blake2b256}
_MinFee            ${constants.minFee}
_OraclePoolNFT     ${constants.oraclePoolNft}
_OracleBuybackNFT  ${constants.oracleBuybackNft}
_OracleFeePk       ErgoTree of ${config.oracle.buybackContractAddress.slice(0, 16)}… (${constants.oracleFeeTree.length / 2} bytes)

Contract address
${tree.toAddress().encode()}

ErgoTree
${tree.toHex()}`);
});
