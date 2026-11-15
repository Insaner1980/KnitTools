import assert from "node:assert/strict";
import { afterEach, beforeEach, describe, it, mock } from "node:test";
import timers from "node:timers/promises";

import { ravelryCallbackUrl, ravelryClientId, ravelryClientSecret } from "../config";
import {
  ravelryAuthStatus,
  ravelryCallback,
  ravelryCompleteAuth,
  ravelryCurrentUser,
  ravelryDisconnect,
  ravelryStartAuth,
} from "./auth";
import { BROWSER_AUTH_COOKIE, browserSecretFromCookie, browserSecretHash } from "./browserAuth";
import type { StoredOAuthState } from "./oauthStateStore";
import { ravelryImportPatternById, ravelryImportPatternByUrl, ravelrySearchPatterns } from "./patternImport";
import { disabledRavelryRateLimiter, RavelryRateLimitError } from "./rateLimit";
import * as stores from "./stores";
import type { PendingRavelryToken } from "./tokenStore";

const state = "s".repeat(43);
const now = 10_000;
const rawError = "synthetic-provider-detail";

describe("production Ravelry HTTP callback with local responses", () => {
  let stored: StoredOAuthState | null;
  let pending: PendingRavelryToken | undefined;
  let generation: number;
  let consumeState: () => Promise<boolean>;
  let fetchResponse: () => Promise<Response>;
  let fetchCalls: number;
  let globalCalls: number;
  let uidCalls: number;
  let stateReads: number;
  let browserBinds: number;
  let stateWrites: number;
  let status: number;
  let body: unknown;
  let redirect: string | undefined;
  let failSave: boolean;
  let failRead: boolean;
  let limit: boolean;
  let failLimiter: boolean;
  let logs: unknown[][];
  let responseCookie: string | undefined;

  beforeEach(() => {
    stored = {
      state, uid: "test-uid", authType: "oauth2", createdAtMillis: 0,
      expiresAtMillis: now + 1, usedAtMillis: null,
      redirectUri: "https://callback.example/ravelryCallback",
      codeVerifier: "synthetic-verifier", codeChallenge: "synthetic-challenge",
      codeChallengeMethod: "S256", connectionGeneration: 3,
    };
    pending = undefined;
    generation = 3;
    fetchCalls = 0;
    globalCalls = 0;
    uidCalls = 0;
    stateReads = 0;
    browserBinds = 0;
    stateWrites = 0;
    status = 200;
    body = undefined;
    redirect = undefined;
    failSave = false;
    failRead = false;
    limit = false;
    failLimiter = false;
    logs = [];
    responseCookie = undefined;
    mock.method(Date, "now", () => now);
    for (const method of ["log", "warn", "error", "info", "debug"] as const) {
      mock.method(console, method, (...args: unknown[]) => { logs.push(args); });
    }
    mock.method(ravelryClientId, "value", () => "synthetic-client");
    mock.method(ravelryClientSecret, "value", () => "synthetic-secret");
    mock.method(ravelryCallbackUrl, "value", () => "https://callback.example/ravelryCallback");
    consumeState = async () => {
      assert.ok(stored);
      stored = { ...stored, usedAtMillis: now };
      return true;
    };
    fetchResponse = async () => Response.json({
      access_token: "synthetic-access", refresh_token: "synthetic-refresh", expires_in: 60,
    });
    mock.method(globalThis, "fetch", async (url: string, options: RequestInit) => {
      fetchCalls++;
      assert.equal(url, "https://www.ravelry.com/oauth2/token");
      assert.equal(stored?.usedAtMillis, now);
      assert.equal(options.method, "POST");
      assert.equal(options.redirect, "error");
      assert.ok(options.signal);
      assert.equal(new Headers(options.headers).get("Authorization"),
        `Basic ${Buffer.from("synthetic-client:synthetic-secret").toString("base64")}`);
      assert.deepEqual(Object.fromEntries(new URLSearchParams(String(options.body))), {
        grant_type: "authorization_code", code: "synthetic-code",
        code_verifier: "synthetic-verifier", redirect_uri: stored?.redirectUri,
      });
      return fetchResponse();
    });
    mock.method(stores, "createRavelryBackendStores", () => ({
      stateStore: {
        async saveState(value: StoredOAuthState) {
          stateWrites++;
          stored = value;
        },
        async bindBrowser(key: string, ticketHash: string, bindingHash: string) {
          browserBinds++;
          assert.ok(globalCalls >= 1);
          assert.ok(uidCalls >= 1);
          if (!stored || key !== stored.state || stored.browserStartHash !== ticketHash ||
              stored.browserBindingHash != null || stored.usedAtMillis != null || stored.expiresAtMillis <= now) return null;
          const { browserStartHash: _startHash, ...unbound } = stored;
          stored = { ...unbound, browserBindingHash: bindingHash };
          return stored;
        },
        async getState(key: string) {
          stateReads++;
          if (failRead) throw new Error(rawError);
          return key === stored?.state ? stored : null;
        },
        async markStateUsed() { return consumeState(); },
        async saveBrowserResult(key: string, _bindingHash: string, result: string) {
          if (!stored || key !== stored.state || stored.usedAtMillis == null) return false;
          stored = { ...stored, browserResult: result };
          return true;
        },
      },
      tokenStore: {
        async getConnectionGeneration() { return generation; },
        async savePendingTokenIfGenerationCurrent(value: PendingRavelryToken, expected: number) {
          if (failSave) throw new Error(rawError);
          if (expected !== generation) return false;
          pending = value;
          return true;
        },
      },
      rateLimiter: {
        ...disabledRavelryRateLimiter,
        async consumeGlobal() {
          globalCalls++;
          if (failLimiter) throw new Error(rawError);
        },
        async consumeUid() {
          uidCalls++;
          if (limit) throw new RavelryRateLimitError("callback", "uid", 10, 60_000);
        },
      },
    }));
  });

  afterEach(() => { mock.restoreAll(); });

  async function invoke(query: Record<string, unknown> = { state, code: "synthetic-code" }, method = "GET", cookie?: string) {
    await ravelryCallback(
      { query, body: query, method, ip: "127.0.0.1", headers: { cookie } } as Parameters<typeof ravelryCallback>[0],
      {
        set(name: string, value: string) {
          if (name === "Set-Cookie") responseCookie = value;
          return this;
        },
        redirect(code: number, url: string) { status = code; redirect = url; },
        status(code: number) { status = code; return this; },
        json(value: unknown) { body = value; return this; },
      } as unknown as Parameters<typeof ravelryCallback>[1],
    );
    const output = JSON.stringify({ redirect, body, logs });
    for (const secret of [rawError, "synthetic-access", "synthetic-refresh",
      "synthetic-secret", "synthetic-verifier", "synthetic-code"]) {
      assert.equal(output.includes(secret), false);
    }
    assert.deepEqual(logs, []);
  }

  function expectRedirect(error: string) {
    assert.equal(status, 302);
    assert.equal(redirect, `knittools://ravelry-auth-complete?state=${state}&error=${error}`);
    assert.equal(body, undefined);
    assert.equal(pending, undefined);
  }

  it("rejects malformed callable completion before consuming the UID quota", async () => {
    const proof = "p".repeat(43);
    for (const data of [
      {},
      { state: [state], proof },
      { state, proof: 1 },
      { state, proof: "p".repeat(44) },
      { state: "invalid", proof },
    ]) {
      await assert.rejects(
        ravelryCompleteAuth.run({
          auth: { uid: "test-uid" },
          data,
        } as Parameters<typeof ravelryCompleteAuth.run>[0]),
        { code: "invalid-argument" },
      );
    }
    assert.equal(uidCalls, 0);
  });

  it("rejects unauthenticated callables and authenticated document-path aliases before creating stores", async () => {
    let storeCalls = 0;
    mock.method(stores, "createRavelryBackendStores", () => { storeCalls++; throw new Error("unexpected_store"); });
    const callables = [ravelryAuthStatus, ravelryCompleteAuth, ravelryCurrentUser, ravelryDisconnect,
      ravelryStartAuth, ravelryImportPatternById, ravelryImportPatternByUrl, ravelrySearchPatterns];
    for (const auth of [undefined, { uid: "test-uid/" }, { uid: "/test-uid" },
      { uid: "." }, { uid: ".." }, { uid: "__reserved__" }]) {
      for (const callable of callables) {
        await assert.rejects(callable.run({
          auth, data: { uid: "other-user" },
        } as Parameters<typeof callable.run>[0]), { code: auth ? "permission-denied" : "unauthenticated" });
      }
    }
    assert.equal(storeCalls, 0);
  });

  it("rejects missing client configuration before saving an OAuth attempt", async () => {
    mock.method(ravelryClientId, "value", () => "");

    await assert.rejects(
      ravelryStartAuth.run({
        auth: { uid: "test-uid" }, data: {},
      } as Parameters<typeof ravelryStartAuth.run>[0]),
      { code: "unavailable" },
    );
    assert.equal(stateWrites, 0);
  });

  it("rejects malformed or insecure callback configuration before saving an OAuth attempt", async () => {
    for (const url of [
      "not a URL", "http://callback.example/ravelryCallback",
      "https://synthetic-user@callback.example/ravelryCallback",
    ]) {
      mock.method(ravelryCallbackUrl, "value", () => url);
      await assert.rejects(
        ravelryStartAuth.run({
          auth: { uid: "test-uid" }, data: {},
        } as Parameters<typeof ravelryStartAuth.run>[0]),
        { code: "unavailable" },
      );
      assert.equal(stateWrites, 0);
    }
  });

  it("preserves the browser binding across overlapping login starts", async () => {
    stored = { ...stored!, browserStartHash: browserSecretHash("t".repeat(43)) };
    await invoke({ state, start: "t".repeat(43) });
    assert.equal(status, 302);
    const firstSecret = browserSecretFromCookie(responseCookie);
    const firstBinding = stored?.browserBindingHash;
    assert.equal(firstBinding, browserSecretHash(firstSecret));
    globalCalls = 0;
    const secondState = "u".repeat(43);
    stored = {
      ...stored!, state: secondState, browserBindingHash: undefined,
      browserStartHash: browserSecretHash("v".repeat(43)),
    };
    await invoke({ state: secondState, start: "v".repeat(43) }, "GET", responseCookie);
    assert.equal(status, 302);
    assert.equal(browserSecretFromCookie(responseCookie), firstSecret);
    assert.equal(stored?.browserBindingHash, firstBinding);
    await invoke({ state: secondState, code: "synthetic-code" }, "GET", responseCookie);
    assert.equal(status, 302);
    assert.ok(pending);
  });

  it("replaces malformed or duplicate browser cookies when starting login", async () => {
    for (const cookie of [
      `${BROWSER_AUTH_COOKIE}=invalid`,
      `${BROWSER_AUTH_COOKIE}=${"b".repeat(43)}; ${BROWSER_AUTH_COOKIE}=${"c".repeat(43)}`,
    ]) {
      globalCalls = 0;
      stored = {
        ...stored!, browserBindingHash: undefined,
        browserStartHash: browserSecretHash("t".repeat(43)),
      };
      await invoke({ state, start: "t".repeat(43) }, "GET", cookie);
      assert.equal(status, 302);
      assert.equal(stored?.browserBindingHash, browserSecretHash(browserSecretFromCookie(responseCookie)));
    }
  });

  it("charges one UID admission for an error-only browser callback and one for its duplicate", async () => {
    const secret = "b".repeat(43);
    assert.ok(stored);
    stored = { ...stored, browserBindingHash: browserSecretHash(secret) };
    mock.method(timers, "setTimeout", async () => {});
    const cookie = `${BROWSER_AUTH_COOKIE}=${secret}`;

    await invoke({ state, error: "access_denied" }, "GET", cookie);
    expectRedirect("access_denied");
    assert.equal(uidCalls, 1);
    assert.equal(globalCalls, 1);
    assert.equal(fetchCalls, 0);

    await invoke({ state, error: "access_denied" }, "GET", cookie);
    expectRedirect("access_denied");
    assert.equal(uidCalls, 2);
    assert.equal(globalCalls, 2);
    assert.equal(fetchCalls, 0);
  });

  it("admits a browser code callback and its cached duplicate once each", async () => {
    const secret = "b".repeat(43);
    assert.ok(stored);
    stored = { ...stored, browserBindingHash: browserSecretHash(secret) };
    const cookie = `${BROWSER_AUTH_COOKIE}=${secret}`;

    await invoke({ state, code: "synthetic-code" }, "GET", cookie);
    assert.equal(status, 302);
    assert.ok(redirect && new URL(redirect).searchParams.get("proof"));
    const originalRedirect = redirect;
    assert.equal(uidCalls, 1);
    assert.equal(fetchCalls, 1);

    await invoke({ state, error: "access_denied" }, "GET", cookie);
    assert.equal(redirect, originalRedirect);
    assert.equal(uidCalls, 2);
    assert.equal(globalCalls, 2);
    assert.equal(fetchCalls, 1);
  });

  it("rejects a browser callback at UID admission before consuming its state", async () => {
    const secret = "b".repeat(43);
    assert.ok(stored);
    stored = { ...stored, browserBindingHash: browserSecretHash(secret) };
    limit = true;

    await invoke({ state, code: "synthetic-code" }, "GET", `${BROWSER_AUTH_COOKIE}=${secret}`);

    assert.equal(status, 429);
    assert.equal(uidCalls, 1);
    assert.equal(fetchCalls, 0);
    assert.equal(stored.usedAtMillis, null);
    assert.equal(pending, undefined);
  });

  for (const query of [
    {}, { state: "bad-state", code: "synthetic-code" }, { state: [state], code: "synthetic-code" },
    { state }, { state, code: "bad\ncode" }, { state, error: ["denied"] },
    { state, code: "x".repeat(2_049) }, { state, start: "bad-ticket" },
    { state, code: "synthetic-code", error: "access_denied" },
    { state, start: "t".repeat(43), code: "synthetic-code" },
    { state, start: "t".repeat(43), error: "access_denied" },
  ]) {
    it(`rejects malformed ${JSON.stringify(Object.keys(query))} before quota and state access`, async () => {
      await invoke(query);
      assert.equal(status, 400);
      assert.equal(globalCalls, 0);
      assert.equal(stateReads, 0);
      assert.equal(fetchCalls, 0);
      assert.equal(stored?.usedAtMillis, null);
    });
  }

  it("rejects a browser start POST before quota and state access", async () => {
    await invoke({ state, start: "t".repeat(43) }, "POST");
    assert.equal(status, 400);
    assert.equal(globalCalls, 0);
    assert.equal(stateReads, 0);
  });

  it("limits syntactically valid unknown states before any database lookup", async () => {
    failLimiter = true;
    await invoke({ state: "u".repeat(43), code: "synthetic-code" });
    assert.equal(status, 500);
    assert.equal(globalCalls, 1);
    assert.equal(stateReads, 0);
  });

  it("charges valid browser starts before binding and redirects to Ravelry", async () => {
    stored = { ...stored!, browserStartHash: browserSecretHash("t".repeat(43)) };
    await invoke({ state, start: "t".repeat(43) });
    assert.equal(globalCalls, 1);
    assert.equal(browserBinds, 1);
    assert.equal(stateReads, 1);
    assert.equal(uidCalls, 1);
    assert.equal(status, 302);
    assert.equal(new URL(redirect!).origin, "https://www.ravelry.com");
    assert.equal(fetchCalls, 0);
  });

  it("keeps the browser ticket retryable when the UID quota rejects the start", async () => {
    const ticket = "t".repeat(43);
    stored = { ...stored!, browserStartHash: browserSecretHash(ticket) };
    limit = true;
    await invoke({ state, start: ticket });
    assert.equal(status, 429);
    assert.equal(browserBinds, 0);
    assert.equal(responseCookie, undefined);
    assert.equal(stored?.browserStartHash, browserSecretHash(ticket));
    limit = false;
    await invoke({ state, start: ticket });
    assert.equal(status, 302);
    assert.equal(browserBinds, 1);
    assert.equal(stored?.browserStartHash, undefined);
    assert.ok(responseCookie);
  });

  it("rejects an invalid browser ticket before UID charging", async () => {
    stored = { ...stored!, browserStartHash: browserSecretHash("t".repeat(43)) };
    await invoke({ state, start: "w".repeat(43) });
    assert.equal(status, 400);
    assert.equal(uidCalls, 0);
    assert.equal(browserBinds, 0);
  });

  it("rejects an overloaded browser start before binding", async () => {
    failLimiter = true;
    await invoke({ state, start: "t".repeat(43) });
    assert.equal(status, 500);
    assert.equal(globalCalls, 1);
    assert.equal(browserBinds, 0);
    assert.equal(stateReads, 0);
  });

  for (const failure of ["http", "network", "timeout", "invalid-json", "missing-token"] as const) {
    it(`redirects ${failure} exchange failure only after consuming accepted state`, async () => {
      fetchResponse = async () => {
        if (failure === "http") return new Response(rawError, { status: 503 });
        if (failure === "network") throw new Error(rawError);
        if (failure === "timeout") throw new DOMException(rawError, "TimeoutError");
        if (failure === "invalid-json") return new Response(rawError);
        return Response.json({ error: rawError });
      };
      await invoke();
      expectRedirect("oauth_error");
      assert.equal(fetchCalls, 1);
      assert.equal(stored?.usedAtMillis, now);
      redirect = undefined;
      await invoke();
      assert.equal(status, 400);
      assert.deepEqual(body, { code: "used_state" });
      assert.equal(redirect, undefined);
      assert.equal(fetchCalls, 1);
    });
  }

  it("returns a proof after a successful PKCE exchange and stores only a pending token", async () => {
    await invoke();
    assert.equal(status, 302);
    assert.equal(fetchCalls, 1);
    const url = new URL(redirect!);
    assert.equal(url.origin, "null");
    assert.equal(url.protocol, "knittools:");
    assert.equal(url.host, "ravelry-auth-complete");
    assert.deepEqual([...url.searchParams.keys()], ["state", "proof"]);
    assert.equal(url.searchParams.get("state"), state);
    assert.match(url.searchParams.get("proof") ?? "", /^[A-Za-z0-9_-]{43}$/);
    assert.equal(pending?.token.accessToken, "synthetic-access");
    assert.equal(pending?.token.connectionGeneration, 3);
    assert.equal(pending?.state, state);
    assert.equal(body, undefined);
  });

  for (const scenario of ["missing", "malformed", "unknown", "used", "missing-code", "invalid-code"] as const) {
    it(`keeps ${scenario} callback as JSON without exchanging tokens`, async () => {
      const query: Record<string, unknown> = { state, code: "synthetic-code" };
      let code = "invalid_state";
      if (scenario === "missing") { delete query.state; code = "missing_state"; }
      if (scenario === "malformed") query.state = "bad-state";
      if (scenario === "unknown") stored = null;
      if (scenario === "used") { stored = { ...stored!, usedAtMillis: now - 1 }; code = "used_state"; }
      if (scenario === "missing-code") { delete query.code; code = "missing_code"; }
      if (scenario === "invalid-code") { query.code = "bad\ncode"; code = "invalid_code"; }
      await invoke(query);
      assert.equal(status, 400);
      assert.deepEqual(body, { code });
      assert.equal(redirect, undefined);
      assert.equal(fetchCalls, 0);
      assert.equal(pending, undefined);
    });
  }

  for (const expiry of [now - 1, now]) {
    it(`preserves state_expired rejection at expiry ${expiry}`, async () => {
      stored = { ...stored!, expiresAtMillis: expiry };
      await invoke();
      expectRedirect("state_expired");
      assert.equal(fetchCalls, 0);
      assert.equal(stored.usedAtMillis, null);
    });
  }

  it("rejects a lost state-consumption race without an exchange", async () => {
    consumeState = async () => { stored = { ...stored!, usedAtMillis: now }; return false; };
    await invoke();
    assert.equal(status, 400);
    assert.deepEqual(body, { code: "used_state" });
    assert.equal(redirect, undefined);
    assert.equal(fetchCalls, 0);
  });

  for (const timing of ["before-exchange", "during-exchange"] as const) {
    it(`rejects a disconnect ${timing} using connectionGeneration`, async () => {
      if (timing === "before-exchange") generation++;
      else fetchResponse = async () => { generation++; return Response.json({ access_token: "synthetic-access" }); };
      await invoke();
      expectRedirect("state_expired");
      assert.equal(fetchCalls, timing === "before-exchange" ? 0 : 1);
    });
  }

  for (const failure of ["rate-limit", "limiter-store", "state-read", "pending-write"] as const) {
    it(`keeps ${failure} errors outside the exchange redirect boundary`, async () => {
      limit = failure === "rate-limit";
      failLimiter = failure === "limiter-store";
      failRead = failure === "state-read";
      failSave = failure === "pending-write";
      await invoke();
      assert.equal(status, limit ? 429 : 500);
      assert.deepEqual(body, { code: limit ? "ravelry_rate_limited" : "ravelry_callback_failed" });
      assert.equal(redirect, undefined);
      assert.equal(pending, undefined);
      assert.equal(fetchCalls, failSave ? 1 : 0);
    });
  }
});
