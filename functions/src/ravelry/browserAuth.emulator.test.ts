import assert from "node:assert/strict";
import { randomBytes } from "node:crypto";
import { after, before, describe, it, mock } from "node:test";

import { Firestore } from "firebase-admin/firestore";

import { RAVELRY_OAUTH_STATES_COLLECTION, RAVELRY_TOKENS_COLLECTION, ravelryClientId, ravelryClientSecret } from "../config";
import { ravelryCallback } from "./auth";
import { completeRavelryOAuth, disconnectRavelry } from "./authCore";
import { BROWSER_AUTH_COOKIE, startBrowserOAuth } from "./browserAuth";
import { createOAuthStateStore } from "./oauthStateStore";
import { disabledRavelryRateLimiter, RavelryRateLimitError } from "./rateLimit";
import * as stores from "./stores";
import { createTokenStore } from "./tokenStore";

describe("browser-bound callback delivery in Firestore", {
  skip: process.env.FIRESTORE_EMULATOR_HOST ? false : "Requires a local Firestore emulator",
}, () => {
  let db: Firestore;
  let stateStore: ReturnType<typeof createOAuthStateStore>;
  let tokenStore: ReturnType<typeof createTokenStore>;
  const created: Array<{ state: string; uid: string }> = [];
  let exchangeCalls = 0;
  let exchange: () => Promise<Response>;
  let limited = false;

  before(() => {
    assert.match(process.env.FIRESTORE_EMULATOR_HOST ?? "", /^(127\.0\.0\.1|localhost):\d+$/);
    db = new Firestore({ projectId: "demo-knittools-browser-auth" });
    stateStore = createOAuthStateStore(db);
    tokenStore = createTokenStore(db);
    exchange = async () => Response.json({ access_token: "synthetic-access" });
    mock.method(ravelryClientId, "value", () => "synthetic-client");
    mock.method(ravelryClientSecret, "value", () => "synthetic-secret");
    mock.method(stores, "createRavelryBackendStores", () => ({
      stateStore, tokenStore,
      rateLimiter: {
        ...disabledRavelryRateLimiter,
        async consumeGlobal() { if (limited) throw new RavelryRateLimitError("callback", "global", 60, 60_000); },
      },
    }));
    mock.method(globalThis, "fetch", async () => { exchangeCalls++; return exchange(); });
  });

  after(async () => {
    mock.restoreAll();
    await Promise.all(created.flatMap(({ state, uid }) => [
      db.collection(RAVELRY_OAUTH_STATES_COLLECTION).doc(state).delete(),
      db.collection(RAVELRY_TOKENS_COLLECTION).doc(uid).delete(),
    ]));
    await db.terminate();
  });

  async function request(query: Record<string, unknown>, cookie?: string, method = "GET") {
    const result = { status: 200, headers: {} as Record<string, string>, redirect: "", body: undefined as unknown };
    await ravelryCallback({ query, method, headers: { cookie } } as Parameters<typeof ravelryCallback>[0], {
      set(name: string, value: string) { result.headers[name] = value; return this; },
      status(code: number) { result.status = code; return this; },
      json(value: unknown) { result.body = value; return this; },
      redirect(code: number, url: string) { result.status = code; result.redirect = url; },
    } as unknown as Parameters<typeof ravelryCallback>[1]);
    return result;
  }

  async function start() {
    const uid = randomBytes(12).toString("hex");
    const started = await startBrowserOAuth({ uid, stateStore, tokenStore,
      clientId: "synthetic-client", backendCallbackUrl: "https://callback.example/ravelryCallback" });
    created.push({ uid, state: started.state });
    const startQuery = Object.fromEntries(new URL(started.authorizeUrl).searchParams);
    assert.equal(new URL(started.authorizeUrl).host, "callback.example");
    const opened = await request(startQuery);
    assert.equal(opened.status, 302);
    const cookie = opened.headers["Set-Cookie"].split(";")[0];
    return { uid, state: started.state, startQuery, opened, cookie };
  }

  it("binds the start ticket once, with a server-only cookie and unchanged PKCE", async () => {
    const flow = await start();
    assert.match(flow.opened.headers["Set-Cookie"], new RegExp(`^${BROWSER_AUTH_COOKIE}=[A-Za-z0-9_-]{43}; Max-Age=600; Path=/; Secure; HttpOnly; SameSite=Lax$`));
    assert.equal(flow.opened.headers["Cache-Control"], "no-store");
    assert.equal(flow.opened.headers["Referrer-Policy"], "no-referrer");
    const provider = new URL(flow.opened.redirect);
    const stored = await stateStore.getState(flow.state);
    assert.equal(provider.origin, "https://www.ravelry.com");
    assert.equal(provider.searchParams.get("code_challenge"), stored?.codeChallenge);
    assert.equal(provider.searchParams.get("code_challenge_method"), "S256");
    assert.equal(stored?.browserStartHash, undefined);
    assert.ok(stored?.browserBindingHash);
    assert.equal((await request(flow.startQuery)).status, 400);
    assert.equal((await request(flow.startQuery, flow.cookie)).status, 400);
    assert.equal(JSON.stringify(stored).includes(flow.cookie.split("=")[1]), false);
  });

  it("delivers the original proof after a duplicate provider error while exchanging exactly once", async () => {
    const flow = await start();
    const beforeCalls = exchangeCalls;
    let release!: () => void;
    let started!: () => void;
    const exchangeStarted = new Promise<void>(resolve => { started = resolve; });
    const gate = new Promise<void>(resolve => { release = resolve; });
    exchange = async () => { started(); await gate; return Response.json({ access_token: "synthetic-access" }); };
    const first = request({ state: flow.state, code: "synthetic-code" }, flow.cookie);
    await exchangeStarted;
    const duplicate = request({ state: flow.state, error: "synthetic-provider-duplicate" }, flow.cookie);
    release();
    const results = await Promise.all([first, duplicate]);
    assert.deepEqual(results.map(result => result.status), [302, 302]);
    assert.equal(results[0].redirect, results[1].redirect);
    assert.equal(exchangeCalls, beforeCalls + 1);
    const proof = new URL(results[1].redirect).searchParams.get("proof");
    assert.ok(proof);
    const stored = await stateStore.getState(flow.state);
    assert.equal(JSON.stringify(stored).includes(proof), false);
    assert.equal(await tokenStore.getToken(flow.uid), null);
    const completion = { uid: flow.uid, state: flow.state, completionProof: proof, tokenStore };
    await assert.rejects(completeRavelryOAuth({ ...completion, completionProof: "x".repeat(43) }), /invalid_completion/);
    await assert.rejects(completeRavelryOAuth({ ...completion, uid: "another-owner" }), /invalid_completion/);
    assert.deepEqual(await completeRavelryOAuth(completion), { connected: true });
    await assert.rejects(completeRavelryOAuth(completion), /invalid_completion/);
    exchange = async () => Response.json({ access_token: "synthetic-access" });
  });

  it("rejects missing, wrong and ambiguous cookies before consumption or result delivery", async () => {
    const flow = await start();
    const query = { state: flow.state, code: "synthetic-code" };
    const beforeCalls = exchangeCalls;
    for (const cookie of [undefined, `${BROWSER_AUTH_COOKIE}=${"w".repeat(43)}`, `${flow.cookie}; ${flow.cookie}`]) {
      assert.equal((await request(query, cookie)).status, 400);
    }
    assert.equal((await request({ state: [flow.state], code: "synthetic-code" }, flow.cookie)).status, 400);
    assert.equal((await stateStore.getState(flow.state))?.usedAtMillis, null);
    assert.equal(exchangeCalls, beforeCalls);
    assert.equal((await request(query, flow.cookie)).status, 302);
    assert.equal((await request(query)).status, 400);
    assert.equal((await request(query, `${BROWSER_AUTH_COOKIE}=${"w".repeat(43)}`)).status, 400);
    assert.equal(exchangeCalls, beforeCalls + 1);
  });

  it("handles an error arriving before two concurrent code callbacks without a second exchange", async () => {
    const flow = await start();
    const beforeCalls = exchangeCalls;
    const error = request({ state: flow.state, error: "synthetic-provider-duplicate" }, flow.cookie);
    const first = request({ state: flow.state, code: "synthetic-code" }, flow.cookie);
    const second = request({ state: flow.state, code: "synthetic-code" }, flow.cookie);
    const results = await Promise.all([error, first, second]);
    assert.deepEqual(results.map(result => result.status), [302, 302, 302]);
    assert.equal(new Set(results.map(result => result.redirect)).size, 1);
    assert.ok(new URL(results[0].redirect).searchParams.get("proof"));
    assert.equal(exchangeCalls, beforeCalls + 1);
  });

  it("preserves a failed token exchange as an error on duplicate delivery", async () => {
    const flow = await start();
    const beforeCalls = exchangeCalls;
    exchange = async () => new Response("synthetic-private-error", { status: 400 });
    try {
      const query = { state: flow.state, code: "synthetic-code" };
      const first = await request(query, flow.cookie);
      const duplicate = await request(query, flow.cookie);
      assert.equal(first.redirect, duplicate.redirect);
      assert.equal(new URL(first.redirect).searchParams.get("error"), "oauth_error");
      assert.equal(new URL(first.redirect).searchParams.has("proof"), false);
      assert.equal(exchangeCalls, beforeCalls + 1);
      assert.equal(await tokenStore.getToken(flow.uid), null);
    } finally { exchange = async () => Response.json({ access_token: "synthetic-access" }); }
  });

  it("keeps global admission on cached results and rejects unsupported methods", async () => {
    const flow = await start();
    const query = { state: flow.state, code: "synthetic-code" };
    assert.equal((await request(query, flow.cookie)).status, 302);
    limited = true;
    assert.equal((await request(query, flow.cookie)).status, 429);
    limited = false;
    assert.equal((await request(query, flow.cookie, "PUT")).status, 405);
  });

  it("blocks result delivery after disconnect, expiry, or an invalid stored outcome", async () => {
    for (const change of ["disconnect", "expiry", "outcome"] as const) {
      const flow = await start();
      const query = { state: flow.state, code: "synthetic-code" };
      const first = await request(query, flow.cookie);
      assert.equal(first.status, 302);
      const beforeCalls = exchangeCalls;
      const ref = db.collection(RAVELRY_OAUTH_STATES_COLLECTION).doc(flow.state);
      if (change === "disconnect") await disconnectRavelry({ uid: flow.uid, tokenStore, stateStore });
      if (change === "expiry") await ref.update({ expiresAtMillis: Date.now() - 1 });
      if (change === "outcome") await ref.update({ browserResult: "untrusted-result" });
      const replay = await request(query, flow.cookie);
      assert.equal(replay.status, 400);
      assert.equal(replay.redirect, "");
      assert.equal(exchangeCalls, beforeCalls);
    }
  });
});
