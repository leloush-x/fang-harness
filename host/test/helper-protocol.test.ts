import { describe, expect, it } from "vitest";
import { decryptPairingBody, pairingKey } from "../src/helper/pairing.js";
import {
  clientMac,
  FrameError,
  FrameOpener,
  FrameSealer,
  serverMac,
  sessionKeys,
} from "../src/helper/protocol.js";

// The spec's test vectors; the app's HelperProtocol asserts the same ones.
const K = Buffer.from("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", "hex");
const nonceC = Buffer.alloc(32, 0xaa);
const nonceS = Buffer.alloc(32, 0xbb);
const { c2s, s2c } = sessionKeys(K, nonceC, nonceS);

describe("fang-helper/1 vectors", () => {
  it("derives the handshake MACs and session keys", () => {
    expect(serverMac(K, nonceC, nonceS).toString("hex")).toBe(
      "f1c6b6cf5f75f497d3fffdd4bfc70890166e49fbe2b9b046ed4f6ae2c8e83c5f",
    );
    expect(clientMac(K, nonceC, nonceS).toString("hex")).toBe(
      "17066979ccce93fca0d1f993543eb1aa423f6d842255f0435a01a415c70869e7",
    );
    expect(c2s.toString("hex")).toBe("35ab6ce27d291c5642acc963f0c3d78fd4f38f2bd2d004df345a7426c86f0e71");
    expect(s2c.toString("hex")).toBe("a33408c6299154bdae49759e131d428c279c3b0d4831e9163b445c9881144aaf");
  });

  it("seals frames with a per-direction counter", () => {
    const client = new FrameSealer(c2s);
    expect(client.sealBody({ type: "hello" }).toString("hex")).toBe(
      "fcfb5ab65cc49cad195bbf471ee6b9639bba1af4ed826e138ba16697d69a527c",
    );
    expect(client.sealBody({ type: "hello" }).toString("hex")).toBe(
      "5696811a9cb57adb907792b93963ec3976a36d936b9d09578f2a50240889eef8",
    );
    expect(new FrameSealer(s2c).sealBody({ type: "hello", version: "x" }).toString("hex")).toBe(
      "44728e25fc3bd94ab9cab296051bd0c4cba4d3da3e5faaa19ee8ef8ee2641750c6ed4f6817dd05a9c6b7a4a39f09",
    );
  });

  it("opens length-prefixed frames split across chunks", () => {
    const sealer = new FrameSealer(c2s);
    const bytes = Buffer.concat([sealer.seal({ type: "hello" }), sealer.seal({ type: "launch", n: 1 })]);
    const opener = new FrameOpener(c2s);
    expect(opener.push(bytes.subarray(0, 3))).toEqual([]);
    expect(opener.push(bytes.subarray(3, 40))).toEqual([{ type: "hello" }]);
    expect(opener.push(bytes.subarray(40))).toEqual([{ type: "launch", n: 1 }]);
  });

  it("rejects replayed, reordered, tampered and oversized frames", () => {
    const sealer = new FrameSealer(c2s);
    const first = sealer.seal({ type: "hello" });
    const second = sealer.seal({ type: "hello" });

    const replay = new FrameOpener(c2s);
    replay.push(first);
    expect(() => replay.push(first)).toThrow(FrameError);

    expect(() => new FrameOpener(c2s).push(second)).toThrow(FrameError);

    const tampered = Buffer.from(first);
    tampered[6] = (tampered[6] ?? 0) ^ 1;
    expect(() => new FrameOpener(c2s).push(tampered)).toThrow(FrameError);

    expect(() => new FrameOpener(c2s).push(Buffer.from([0x00, 0x41, 0x00, 0x11]))).toThrow(FrameError);
    expect(() => new FrameOpener(c2s).push(Buffer.alloc(4))).toThrow(FrameError);
  });
});

describe("pairing vector", () => {
  const token = Buffer.alloc(32, 0x11);
  const sid = "00112233445566778899aabbccddeeff";
  const body =
    "22222222222222222222222220732b98962a8c00ec1da107e4dd015de1ef4ac4195fdd716281e640626ad134d00ecce68873851e6749628d3d071825";

  it("derives pk and decrypts the key body", () => {
    expect(pairingKey(token).toString("hex")).toBe("c30194d6f11f4ca9fcb6e555a793ef35e5b136c22207869f9f6eb0c04143af6a");
    expect(decryptPairingBody(body, token, sid)).toEqual(K);
  });

  it("rejects a body for another session", () => {
    expect(() => decryptPairingBody(body, token, "ffeeddccbbaa99887766554433221100")).toThrow(
      "The key from Fang Harness didn't match this setup command.",
    );
  });
});
