import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { HttpsError, type CallableRequest } from "firebase-functions/v2/https";

import {
  ravelryAuthStatus,
  ravelryCurrentUser,
  ravelryDisconnect,
  ravelryStartAuth,
} from "./auth";
import { createRavelryClient } from "./client";
import type { OAuthTokenRefresh } from "./oauth2";
import {
  importPatternById,
  importPatternByUrl,
  ravelrySearchPatterns,
  ravelryImportPatternById,
  ravelryImportPatternByUrl,
  searchPatternsForUser,
} from "./patternImport";
import {
  RAVELRY_RATE_LIMIT_RULES,
  RavelryRateLimitError,
  type RavelryRateLimitBucket,
  type RavelryRateLimiter,
} from "./rateLimit";
import type { RavelryTokenStore, StoredRavelryToken } from "./tokenStore";
import { parseRavelryPatternUrl } from "./urlParsing";

class MemoryTokenStore implements RavelryTokenStore {
  readonly collectionPath = "ravelryTokens";
  readonly tokens = new Map<string, StoredRavelryToken>();

  async getToken(uid: string): Promise<StoredRavelryToken | null> {
    return this.tokens.get(uid) ?? null;
  }

  async saveToken(token: StoredRavelryToken): Promise<void> {
    this.tokens.set(token.uid, token);
  }

  async deleteToken(uid: string): Promise<void> {
    this.tokens.delete(uid);
  }
}

class RecordingRateLimiter implements RavelryRateLimiter {
  readonly calls: Array<{ uid: string; bucket: RavelryRateLimitBucket }> = [];

  async consume(uid: string, bucket: RavelryRateLimitBucket): Promise<void> {
    this.calls.push({ uid, bucket });
  }
}

class BlockingRateLimiter implements RavelryRateLimiter {
  async consume(_uid: string, bucket: RavelryRateLimitBucket): Promise<void> {
    const rule = RAVELRY_RATE_LIMIT_RULES[bucket];
    throw new RavelryRateLimitError(bucket, rule.limit, rule.windowMillis);
  }
}

function callableRequest(data: unknown): CallableRequest {
  return {
    data,
    rawRequest: {} as CallableRequest["rawRequest"],
    acceptsStreaming: false,
  };
}

function isUnauthenticatedHttpsError(error: unknown): boolean {
  return error instanceof HttpsError && error.code === "unauthenticated";
}

type RunnableCallable = {
  run(request: CallableRequest): Promise<unknown>;
};

describe("Ravelry backend search and import", () => {
  it("parses only Ravelry pattern-library URLs and normalizes canonical URLs", () => {
    assert.deepEqual(
      parseRavelryPatternUrl("https://www.ravelry.com/patterns/library/cozy-hat?set=&foo=bar#notes"),
      {
        originalUrl: "https://www.ravelry.com/patterns/library/cozy-hat?set=&foo=bar#notes",
        canonicalUrl: "https://www.ravelry.com/patterns/library/cozy-hat",
        patternSlug: "cozy-hat",
      },
    );
    assert.deepEqual(parseRavelryPatternUrl("https://ravelry.com/patterns/library/12345"), {
      originalUrl: "https://ravelry.com/patterns/library/12345",
      canonicalUrl: "https://www.ravelry.com/patterns/library/12345",
      ravelryPatternId: 12345,
      patternSlug: "12345",
    });
    assert.equal(parseRavelryPatternUrl("https://example.com/patterns/library/cozy-hat"), null);
    assert.equal(parseRavelryPatternUrl("not a url"), null);
  });

  it("rejects malformed percent-encoded Ravelry pattern slugs", () => {
    assert.equal(parseRavelryPatternUrl("https://www.ravelry.com/patterns/library/%"), null);
    assert.equal(parseRavelryPatternUrl("https://www.ravelry.com/patterns/library/%ZZ"), null);
  });

  it("searches Ravelry with a bearer token and returns only sanitized fields plus pagination", async () => {
    const calls: Array<{ url: string; authorization?: string }> = [];
    const fetchImpl: typeof fetch = async (input, init) => {
      const url = typeof input === "string" ? input : input.toString();
      calls.push({
        url,
        authorization: init?.headers instanceof Headers ? init.headers.get("Authorization") ?? undefined : undefined,
      });
      return new Response(
        JSON.stringify({
          patterns: [
            {
              id: 42,
              name: "Cozy Hat",
              designer: { name: "Ada Designer" },
              first_photo: {
                small2_url: "https://images.example/small.jpg",
                medium_url: "https://images.example/medium.jpg",
              },
              free: false,
              permalink: "cozy-hat",
              pdf_url: "https://private.example/pattern.pdf",
            },
            {
              id: 43,
              name: "Mystery Mitts",
              designer: null,
              first_photo: null,
              permalink: "mystery-mitts",
            },
          ],
          paginator: {
            page: 2,
            page_count: 5,
            results: 84,
          },
        }),
        { status: 200 },
      );
    };

    const response = await createRavelryClient(fetchImpl).searchPatterns("access-token", {
      query: "hat",
      craft: "crochet",
      availability: "free",
      pc: "hat",
      weight: "dk",
      difficultyFrom: 2,
      difficultyTo: 5,
      page: 2,
      pageSize: 10,
    });

    const requestUrl = new URL(calls[0].url);
    assert.equal(requestUrl.origin + requestUrl.pathname, "https://api.ravelry.com/patterns/search.json");
    assert.equal(requestUrl.searchParams.get("query"), "hat");
    assert.equal(requestUrl.searchParams.get("craft"), "crochet");
    assert.equal(requestUrl.searchParams.get("availability"), "free");
    assert.equal(requestUrl.searchParams.get("pc"), "hat");
    assert.equal(requestUrl.searchParams.get("weight"), "dk");
    assert.equal(requestUrl.searchParams.get("diff_from"), "2");
    assert.equal(requestUrl.searchParams.get("diff_to"), "5");
    assert.equal(requestUrl.searchParams.get("page"), "2");
    assert.equal(requestUrl.searchParams.get("page_size"), "10");
    assert.equal(requestUrl.searchParams.get("sort"), "best");
    assert.equal(calls[0].authorization, "Bearer access-token");
    assert.deepEqual(response, {
      patterns: [
        {
          ravelryPatternId: 42,
          title: "Cozy Hat",
          designerName: "Ada Designer",
          thumbnailUrl: "https://images.example/medium.jpg",
          canonicalUrl: "https://www.ravelry.com/patterns/library/cozy-hat",
          availability: "paid",
        },
        {
          ravelryPatternId: 43,
          title: "Mystery Mitts",
          designerName: "",
          canonicalUrl: "https://www.ravelry.com/patterns/library/mystery-mitts",
          availability: "unknown",
        },
      ],
      pagination: {
        page: 2,
        pageCount: 5,
        resultCount: 84,
      },
    });
    assert.equal(JSON.stringify(response).includes("pdf_url"), false);
  });

  it("preserves Ravelry HTTP status codes for backend error mapping", async () => {
    const client = createRavelryClient(async () => new Response("rate limited", { status: 429 }));

    await assert.rejects(
      client.searchPatterns("access-token", { query: "hat" }),
      /ravelry_http_429/,
    );
  });

  it("imports by ID or URL as metadata without downloading PDFs", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });

    const client = createRavelryClient(async (input) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/patterns/search.json")) {
        return new Response(
          JSON.stringify({
            patterns: [
              {
                id: 42,
                name: "Cozy Hat",
                designer: { name: "Ada Designer" },
                permalink: "cozy-hat",
                free: true,
              },
            ],
            paginator: { page: 1, page_count: 1, results: 1 },
          }),
          { status: 200 },
        );
      }
      return new Response(
        JSON.stringify({
          pattern: {
            id: 42,
            name: "Cozy Hat",
            designer: { name: "Ada Designer" },
            photos: [{ medium_url: "https://images.example/detail.jpg" }],
            permalink: "cozy-hat",
            free: true,
            pdf_url: "https://private.example/pattern.pdf",
          },
        }),
        { status: 200 },
      );
    });

    assert.deepEqual(
      await importPatternById({
        uid: "uid",
        tokenStore,
        client,
        ravelryPatternId: 42,
      }),
      {
        ravelryPatternId: 42,
        title: "Cozy Hat",
        designerName: "Ada Designer",
        thumbnailUrl: "https://images.example/detail.jpg",
        canonicalUrl: "https://www.ravelry.com/patterns/library/cozy-hat",
        availability: "free",
      },
    );

    const fromUrl = await importPatternByUrl({
      uid: "uid",
      tokenStore,
      client,
      url: "https://www.ravelry.com/patterns/library/cozy-hat?utm_source=share",
    });
    assert.equal(fromUrl.ravelryPatternId, 42);
    assert.equal(fromUrl.originalUrl, "https://www.ravelry.com/patterns/library/cozy-hat?utm_source=share");
    assert.equal(JSON.stringify(fromUrl).includes("pdf_url"), false);
  });

  it("consumes search and import rate-limit buckets for slug URL imports", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });
    const rateLimiter = new RecordingRateLimiter();

    const client = createRavelryClient(async (input) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/patterns/search.json")) {
        return new Response(
          JSON.stringify({
            patterns: [
              {
                id: 42,
                name: "Cozy Hat",
                designer: { name: "Ada Designer" },
                permalink: "cozy-hat",
              },
            ],
            paginator: { page: 1, page_count: 1, results: 1 },
          }),
          { status: 200 },
        );
      }
      return new Response(
        JSON.stringify({
          pattern: {
            id: 42,
            name: "Cozy Hat",
            designer: { name: "Ada Designer" },
            permalink: "cozy-hat",
          },
        }),
        { status: 200 },
      );
    });

    await importPatternByUrl({
      uid: "uid",
      tokenStore,
      client,
      rateLimiter,
      url: "https://www.ravelry.com/patterns/library/cozy-hat",
    });

    assert.deepEqual(rateLimiter.calls, [
      { uid: "uid", bucket: "search" },
      { uid: "uid", bucket: "import" },
    ]);
  });

  it("does not call Ravelry when the authenticated search bucket is exhausted", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });
    let searchCount = 0;
    const client = {
      async getCurrentUser() {
        throw new Error("not used");
      },
      async searchPatterns() {
        searchCount += 1;
        throw new Error("not used");
      },
      async getPatternById() {
        throw new Error("not used");
      },
    };

    await assert.rejects(
      searchPatternsForUser({
        uid: "uid",
        tokenStore,
        client,
        rateLimiter: new BlockingRateLimiter(),
        query: { query: "hat" },
      }),
      /ravelry_rate_limited/,
    );
    assert.equal(searchCount, 0);
  });

  it("does not call Ravelry when the Firebase user has no stored Ravelry token", async () => {
    const tokenStore = new MemoryTokenStore();
    let searchCount = 0;
    const client = {
      async getCurrentUser() {
        throw new Error("not used");
      },
      async searchPatterns() {
        searchCount += 1;
        throw new Error("not used");
      },
      async getPatternById() {
        throw new Error("not used");
      },
    };

    await assert.rejects(
      searchPatternsForUser({
        uid: "uid",
        tokenStore,
        client,
        query: { query: "hat" },
      }),
      /ravelry_not_connected/,
    );
    assert.equal(searchCount, 0);
  });

  it("rejects unauthenticated Ravelry callables before backend work or request data validation", async () => {
    const callables: Array<{ name: string; callable: RunnableCallable; data: unknown }> = [
      { name: "ravelryStartAuth", callable: ravelryStartAuth, data: {} },
      { name: "ravelryAuthStatus", callable: ravelryAuthStatus, data: {} },
      { name: "ravelryDisconnect", callable: ravelryDisconnect, data: {} },
      { name: "ravelryCurrentUser", callable: ravelryCurrentUser, data: {} },
      { name: "ravelrySearchPatterns", callable: ravelrySearchPatterns, data: {} },
      { name: "ravelryImportPatternById", callable: ravelryImportPatternById, data: {} },
      { name: "ravelryImportPatternByUrl", callable: ravelryImportPatternByUrl, data: {} },
    ];

    for (const { name, callable, data } of callables) {
      await assert.rejects(
        callable.run(callableRequest(data)),
        isUnauthenticatedHttpsError,
        `${name} should reject unauthenticated calls first`,
      );
    }
  });

  it("refreshes an expired token before searching patterns", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });
    const refresh: OAuthTokenRefresh = async ({ refreshToken }) => {
      assert.equal(refreshToken, "old-refresh-token");
      return {
        accessToken: "fresh-access-token",
        refreshToken: "rotated-refresh-token",
        expiresAtMillis: 10_000,
      };
    };
    const client = {
      async getCurrentUser() {
        throw new Error("not used");
      },
      async searchPatterns(accessToken: string) {
        assert.equal(accessToken, "fresh-access-token");
        return {
          patterns: [],
          pagination: { page: 1, pageCount: 1, resultCount: 0 },
        };
      },
      async getPatternById() {
        throw new Error("not used");
      },
    };

    await searchPatternsForUser({
      uid: "uid",
      tokenStore,
      client,
      refresh,
      nowMillis: () => 1_000,
      query: { query: "hat" },
    });

    assert.deepEqual(await tokenStore.getToken("uid"), {
      uid: "uid",
      authType: "oauth2",
      accessToken: "fresh-access-token",
      refreshToken: "rotated-refresh-token",
      expiresAtMillis: 10_000,
      createdAtMillis: 100,
      updatedAtMillis: 1_000,
    });
  });

  it("keeps refresh handling when importing a slug URL", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "old-refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });
    const refresh: OAuthTokenRefresh = async () => ({
      accessToken: "fresh-access-token",
      refreshToken: "rotated-refresh-token",
      expiresAtMillis: 10_000,
    });
    const client = createRavelryClient(async (input, init) => {
      assert.equal(
        init?.headers instanceof Headers ? init.headers.get("Authorization") : undefined,
        "Bearer fresh-access-token",
      );
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/patterns/search.json")) {
        return new Response(
          JSON.stringify({
            patterns: [
              {
                id: 42,
                name: "Cozy Hat",
                designer: { name: "Ada Designer" },
                permalink: "cozy-hat",
              },
            ],
            paginator: { page: 1, page_count: 1, results: 1 },
          }),
          { status: 200 },
        );
      }
      return new Response(
        JSON.stringify({
          pattern: {
            id: 42,
            name: "Cozy Hat",
            designer: { name: "Ada Designer" },
            permalink: "cozy-hat",
          },
        }),
        { status: 200 },
      );
    });

    const pattern = await importPatternByUrl({
      uid: "uid",
      tokenStore,
      client,
      refresh,
      nowMillis: () => 1_000,
      url: "https://www.ravelry.com/patterns/library/cozy-hat",
    });

    assert.equal(pattern.ravelryPatternId, 42);
  });
});
