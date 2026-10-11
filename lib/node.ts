import type { Box, SignedTransaction } from "@fleet-sdk/common";

// Minimal client for an Ergo node's REST API. Amounts can exceed 2^53, so JSON is parsed and written
// without going through JavaScript numbers.

/** JSON.parse that keeps integers beyond 2^53 exact, as bigint. */
export function parseJson<T>(text: string): T {
  return JSON.parse(text, (_key, value, context?: { source?: string }) =>
    typeof value === "number" && !Number.isSafeInteger(value) && context?.source && /^-?\d+$/.test(context.source)
      ? BigInt(context.source)
      : value,
  );
}

const rawJSON = (JSON as unknown as { rawJSON(text: string): unknown }).rawJSON;

/** JSON.stringify that writes bigint values as plain JSON numbers. */
export function stringifyJson(value: unknown, space?: number): string {
  return JSON.stringify(value, (_key, v) => (typeof v === "bigint" ? rawJSON(v.toString()) : v), space);
}

export interface NodeHeader {
  id: string;
  parentId: string;
  version: number;
  height: number;
  timestamp: number;
  nBits: number;
  stateRoot: string;
  adProofsRoot: string;
  transactionsRoot: string;
  extensionHash: string;
  powSolutions: { pk: string; w: string; n: string; d: number | bigint | string };
  votes: string;
}

/** Blockchain parameters as the node reports them in /info. */
export interface NodeParameters {
  storageFeeFactor: number;
  minValuePerByte: number;
  maxBlockSize: number;
  tokenAccessCost: number;
  inputCost: number;
  dataInputCost: number;
  outputCost: number;
  maxBlockCost: number;
  blockVersion: number;
}

interface NodeBox {
  boxId: string;
  transactionId: string;
  index: number;
  value: number | bigint;
  ergoTree: string;
  creationHeight: number;
  assets: { tokenId: string; amount: number | bigint }[];
  additionalRegisters: Record<string, string>;
}

export class ErgoNode {
  readonly url: string;

  constructor(url: string) {
    this.url = url;
  }

  async info(): Promise<{ fullHeight: number; parameters: NodeParameters }> {
    return this.get("/info");
  }

  async height(): Promise<number> {
    return (await this.info()).fullHeight;
  }

  /** The last `count` block headers, newest first. */
  async lastHeaders(count = 10): Promise<NodeHeader[]> {
    const headers = await this.get<NodeHeader[]>(`/blocks/lastHeaders/${count}`);
    return headers.sort((a, b) => b.height - a.height);
  }

  /** Unspent boxes holding a token, including unconfirmed ones and excluding boxes spent in the mempool. */
  async unspentByTokenId(tokenId: string): Promise<Box<bigint>[]> {
    const boxes = await this.live(
      await this.get<NodeBox[]>(`/blockchain/box/unspent/byTokenId/${tokenId}?${MEMPOOL_AWARE}&offset=0&limit=100`),
    );
    const spent = await Promise.all(boxes.map((b) => this.spentInMempool(b.boxId)));
    return boxes.filter((_, i) => !spent[i]);
  }

  /** Unspent boxes of an address, including unconfirmed ones and excluding boxes spent in the mempool. */
  async unspentByAddress(address: string): Promise<Box<bigint>[]> {
    const boxes = await this.post<NodeBox[]>(
      `/blockchain/box/unspent/byAddress?${MEMPOOL_AWARE}&offset=0&limit=500`,
      JSON.stringify(address),
    );
    return this.live(boxes);
  }

  /**
   * The boxes that are in the UTXO set or created in the mempool. The node's blockchain index can list
   * boxes as unspent after they have been spent, so its answers are checked against the UTXO set.
   */
  private async live(boxes: NodeBox[]): Promise<Box<bigint>[]> {
    if (boxes.length === 0) return [];
    const live = await this.post<NodeBox[]>("/utxo/withPool/byIds", JSON.stringify(boxes.map((b) => b.boxId)));
    const ids = new Set(live.map((b) => b.boxId));
    return boxes.filter((b) => ids.has(b.boxId)).map(toBox);
  }

  /** Whether an unconfirmed transaction spends the box. */
  private async spentInMempool(boxId: string): Promise<boolean> {
    const path = `/transactions/unconfirmed/inputs/byBoxId/${boxId}`;
    const response = await fetch(new URL(path, this.url));
    if (response.status === 404) return false;
    await this.read(response, `GET ${path}`);
    return true;
  }

  /** Has the node validate a signed transaction without broadcasting it. Returns the transaction id. */
  async checkTransaction(tx: SignedTransaction): Promise<string> {
    return this.post("/transactions/check", stringifyJson(tx));
  }

  /** Submits a signed transaction to the node's mempool. Returns the transaction id. */
  async submitTransaction(tx: SignedTransaction): Promise<string> {
    return this.post("/transactions", stringifyJson(tx));
  }

  private async get<T>(path: string): Promise<T> {
    const response = await fetch(new URL(path, this.url));
    return this.read<T>(response, `GET ${path}`);
  }

  private async post<T>(path: string, body: string): Promise<T> {
    const response = await fetch(new URL(path, this.url), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body,
    });
    return this.read<T>(response, `POST ${path.split("?")[0]}`);
  }

  private async read<T>(response: Response, request: string): Promise<T> {
    const text = await response.text();
    if (!response.ok) throw new Error(`${request} failed with ${response.status}: ${text}`);
    return parseJson<T>(text);
  }
}

const MEMPOOL_AWARE = "includeUnconfirmed=true&excludeMempoolSpent=true";

function toBox(b: NodeBox): Box<bigint> {
  return {
    boxId: b.boxId,
    transactionId: b.transactionId,
    index: b.index,
    value: BigInt(b.value),
    ergoTree: b.ergoTree,
    creationHeight: b.creationHeight,
    assets: b.assets.map((a) => ({ tokenId: a.tokenId, amount: BigInt(a.amount) })),
    additionalRegisters: b.additionalRegisters,
  };
}
