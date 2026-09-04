import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { HttpsError, type CallableRequest } from "firebase-functions/v2/https";

import {
  ravelryAuthStatus,
  ravelryCurrentUser,
  ravelryDisconnect,
  ravelryStartAuth,
} from "./auth";
import { httpsErrorFor } from "./callable";
import { createRavelryClient, RavelryClientHttpError, type RavelryClient } from "./client";
import type { OAuthTokenRefresh } from "./oauth2";
import {
  importPatternById,
  importPatternByUrl,
  RavelryPatternImportError,
  RAVELRY_SEARCH_MAX_PAGE_SIZE,
  ravelrySearchPatterns,
  ravelryImportPatternById,
  ravelryImportPatternByUrl,
  ravelrySearchQueryFromData,
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
  private readonly generations = new Map<string, number>();

  async getToken(uid: string): Promise<StoredRavelryToken | null> {
    return this.tokens.get(uid) ?? null;
  }

  async getConnectionGeneration(uid: string): Promise<number> {
    return this.generations.get(uid) ?? this.tokens.get(uid)?.connectionGeneration ?? 0;
  }

  async saveToken(token: StoredRavelryToken): Promise<void> {
    const connectionGeneration = token.connectionGeneration ?? await this.getConnectionGeneration(token.uid);
    this.generations.set(token.uid, connectionGeneration);
    this.tokens.set(token.uid, { ...token, connectionGeneration });
  }

  async saveTokenIfGenerationCurrent(
    token: StoredRavelryToken,
    expectedGeneration: number,
  ): Promise<boolean> {
    if (await this.getConnectionGeneration(token.uid) !== expectedGeneration) {
      return false;
    }
    this.generations.set(token.uid, expectedGeneration);
    this.tokens.set(token.uid, { ...token, connectionGeneration: expectedGeneration });
    return true;
  }

  async saveRefreshedTokenIfCurrent(
    token: StoredRavelryToken,
    expectedToken: StoredRavelryToken,
  ): Promise<StoredRavelryToken | null> {
    const current = this.tokens.get(token.uid);
    if (!current ||
      current.accessToken !== expectedToken.accessToken ||
      current.refreshToken !== expectedToken.refreshToken ||
      current.expiresAtMillis !== expectedToken.expiresAtMillis ||
      (current.connectionGeneration ?? 0) !== (expectedToken.connectionGeneration ?? 0)) {
      return null;
    }
    const persisted = { ...token, connectionGeneration: current.connectionGeneration ?? 0 };
    this.tokens.set(token.uid, persisted);
    return persisted;
  }

  async updateUserMetadataIfGenerationCurrent(
    uid: string,
    update: {
      readonly ravelryUserId?: string;
      readonly ravelryUsername?: string;
      readonly verifiedAtMillis: number;
    },
    expectedGeneration: number,
  ): Promise<boolean> {
    const current = this.tokens.get(uid);
    if (!current || (current.connectionGeneration ?? 0) !== expectedGeneration) {
      return false;
    }
    this.tokens.set(uid, {
      ...current,
      ravelryUserId: update.ravelryUserId,
      ravelryUsername: update.ravelryUsername,
      updatedAtMillis: update.verifiedAtMillis,
      lastVerifiedAtMillis: update.verifiedAtMillis,
    });
    return true;
  }

  async deleteToken(uid: string, _nowMillis?: number): Promise<void> {
    this.generations.set(uid, await this.getConnectionGeneration(uid) + 1);
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
    throw new RavelryRateLimitError(bucket, "uid", rule.limit, rule.windowMillis);
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
    assert.equal(parseRavelryPatternUrl("https://ravelry.com.example/patterns/library/cozy-hat"), null);
    assert.equal(parseRavelryPatternUrl("https://user@ravelry.com/patterns/library/cozy-hat"), null);
    assert.equal(parseRavelryPatternUrl("http://ravelry.com/patterns/library/cozy-hat"), null);
    assert.equal(parseRavelryPatternUrl("https://ravelry.com/patterns/library/cozy-hat/extra"), null);
    assert.equal(parseRavelryPatternUrl("https://ravelry.com/patterns/library/cozy%2Fhat"), null);
    assert.equal(parseRavelryPatternUrl("https://ravelry.com/patterns/library/0"), null);
    assert.equal(
      parseRavelryPatternUrl("https://ravelry.com/patterns/library/9007199254740993"),
      null,
    );
    assert.equal(parseRavelryPatternUrl("not a url"), null);
  });

  it("rejects malformed percent-encoded Ravelry pattern slugs", () => {
    assert.equal(parseRavelryPatternUrl("https://www.ravelry.com/patterns/library/%"), null);
    assert.equal(parseRavelryPatternUrl("https://www.ravelry.com/patterns/library/%ZZ"), null);
  });

  it("rejects oversized Ravelry search page sizes before backend work", () => {
    assert.throws(
      () => ravelrySearchQueryFromData({ query: "hat", pageSize: RAVELRY_SEARCH_MAX_PAGE_SIZE + 1 }),
      /invalid_page_size/,
    );
    assert.deepEqual(
      ravelrySearchQueryFromData({ query: "hat", pageSize: RAVELRY_SEARCH_MAX_PAGE_SIZE }),
      { query: "hat", pageSize: RAVELRY_SEARCH_MAX_PAGE_SIZE },
    );
  });

  it("rejects malformed or unbounded search inputs", () => {
    for (const data of [
      { query: "hat\nscarf" },
      { query: "A".repeat(201) },
      { query: "hat", craft: 42 },
      { query: "hat", page: 0 },
      { query: "hat", page: 1.5 },
      { query: "hat", page: 1_001 },
      { query: "hat", difficultyFrom: 0 },
      { query: "hat", difficultyTo: 11 },
      { query: "hat", difficultyFrom: 8, difficultyTo: 2 },
    ]) {
      assert.throws(() => ravelrySearchQueryFromData(data));
    }
  });

  it("rejects unsafe direct pattern IDs before backend work", async () => {
    for (const ravelryPatternId of [0, -1, 1.5, 2_147_483_648, Number.MAX_SAFE_INTEGER + 1]) {
      await assert.rejects(
        importPatternById({
          uid: "uid",
          tokenStore: new MemoryTokenStore(),
          client: {} as RavelryClient,
          ravelryPatternId,
        }),
        /invalid_pattern_id/,
      );
    }
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

  it("rejects upstream patterns without a positive integer Ravelry pattern ID", async () => {
    const invalidIds = [0, -1, 1.5];
    const client = createRavelryClient(async (input) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/patterns/search.json")) {
        return new Response(
          JSON.stringify({
            patterns: invalidIds.map((id) => ({
              id,
              name: "Invalid pattern",
              permalink: `invalid-${id}`,
            })),
            paginator: { page: 1, page_count: 1, results: invalidIds.length },
          }),
          { status: 200 },
        );
      }
      return new Response(
        JSON.stringify({
          pattern: {
            id: 0,
            name: "Invalid pattern",
            permalink: "invalid-pattern",
          },
        }),
        { status: 200 },
      );
    });

    const searchResponse = await client.searchPatterns("access-token", { query: "invalid" });

    assert.deepEqual(searchResponse.patterns, []);
    assert.equal(await client.getPatternById("access-token", 42), null);
  });

  it("omits missing or malformed thumbnail data from sanitized patterns", async () => {
    const client = createRavelryClient(async (input) => {
      const url = typeof input === "string" ? input : input.toString();
      if (url.includes("/patterns/search.json")) {
        return new Response(
          JSON.stringify({
            patterns: [
              {
                id: 42,
                name: "No thumbnail",
                permalink: "no-thumbnail",
                first_photo: { medium_url: "not a URL", small2_url: 123 },
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
            name: "No thumbnail",
            permalink: "no-thumbnail",
            photos: [{ medium_url: "http://images.example/cleartext.jpg", small2_url: ["invalid"] }],
          },
        }),
        { status: 200 },
      );
    });

    const searchPattern = (await client.searchPatterns("access-token", { query: "hat" })).patterns[0];
    const detailPattern = await client.getPatternById("access-token", 42);

    assert.equal(Object.hasOwn(searchPattern, "thumbnailUrl"), false);
    assert.ok(detailPattern);
    assert.equal(Object.hasOwn(detailPattern, "thumbnailUrl"), false);
  });

  it("bounds upstream display fields and requires credential-free HTTPS thumbnail URLs", async () => {
    const client = createRavelryClient(async () =>
      new Response(
        JSON.stringify({
          patterns: [{
            id: 42,
            name: `  Cozy\u0000${"H".repeat(600)}  `,
            designer: { name: "Ada\nDesigner" },
            permalink: "cozy-hat",
            first_photo: { medium_url: "https://user@example.com/private.jpg" },
          }],
          paginator: { page: -1, page_count: 1.5, results: Number.MAX_SAFE_INTEGER + 1 },
        }),
        { status: 200 },
      ),
    );

    const response = await client.searchPatterns("access-token", { query: "hat" });
    assert.equal(response.patterns[0]?.title.length, 500);
    assert.equal(response.patterns[0]?.title.includes("\u0000"), false);
    assert.equal(response.patterns[0]?.designerName, "Ada Designer");
    assert.equal(Object.hasOwn(response.patterns[0] ?? {}, "thumbnailUrl"), false);
    assert.deepEqual(response.pagination, { page: 1, pageCount: 1, resultCount: 0 });
  });

  it("sanitizes and bounds current-user metadata", async () => {
    const client = createRavelryClient(async () =>
      new Response(
        JSON.stringify({
          user: {
            id: 42,
            username: `  Ada\n${"D".repeat(300)}  `,
          },
        }),
        { status: 200 },
      ),
    );

    const user = await client.getCurrentUser("access-token");
    assert.equal(user.ravelryUserId, "42");
    assert.equal(user.ravelryUsername?.length, 200);
    assert.equal(user.ravelryUsername?.includes("\n"), false);
  });

  it("bounds Ravelry requests and maps transport failures to a sanitized unavailable error", async () => {
    let capturedSignal: AbortSignal | undefined;
    let capturedRedirect: RequestRedirect | undefined;
    const signalClient = createRavelryClient(async (_input, init) => {
      capturedSignal = init?.signal ?? undefined;
      capturedRedirect = init?.redirect;
      return new Response(JSON.stringify({ patterns: [], paginator: {} }), { status: 200 });
    });
    await signalClient.searchPatterns("access-token", { query: "hat" });
    assert.ok(capturedSignal instanceof AbortSignal);
    assert.equal(capturedRedirect, "error");

    const failingClient = createRavelryClient(async () => {
      throw new Error("access-token upstream body");
    });
    await assert.rejects(
      failingClient.searchPatterns("access-token", { query: "hat" }),
      (error: unknown) => {
        assert.ok(error instanceof RavelryClientHttpError);
        assert.equal(error.httpStatus, 503);
        assert.equal(error.message, "ravelry_http_503");
        assert.equal(error.message.includes("access-token upstream body"), false);
        return true;
      },
    );

    const malformedResponseClient = createRavelryClient(async () =>
      new Response("not-json", { status: 200 }),
    );
    await assert.rejects(
      malformedResponseClient.searchPatterns("access-token", { query: "hat" }),
      (error: unknown) => {
        assert.ok(error instanceof RavelryClientHttpError);
        assert.equal(error.httpStatus, 503);
        return true;
      },
    );
  });

  it("rejects an oversized Ravelry response before parsing it", async () => {
    const client = createRavelryClient(async () =>
      new Response(
        JSON.stringify({
          patterns: [],
          paginator: {},
          padding: "x".repeat(1_048_576),
        }),
        { status: 200 },
      ),
    );

    await assert.rejects(
      client.searchPatterns("access-token", { query: "hat" }),
      (error: unknown) => {
        assert.ok(error instanceof RavelryClientHttpError);
        assert.equal(error.httpStatus, 503);
        return true;
      },
    );
  });

  it("preserves Ravelry HTTP status codes for backend error mapping", async () => {
    const client = createRavelryClient(async () => new Response("rate limited", { status: 429 }));

    await assert.rejects(
      client.searchPatterns("access-token", { query: "hat" }),
      /ravelry_http_429/,
    );
    assert.equal(httpsErrorFor(new RavelryClientHttpError(403)).code, "unauthenticated");
    assert.equal(httpsErrorFor(new RavelryClientHttpError(429)).code, "resource-exhausted");
    assert.equal(httpsErrorFor(new RavelryClientHttpError(503)).code, "unavailable");
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

  it("rejects a slug URL when search returns no exact canonical match", async () => {
    const tokenStore = new MemoryTokenStore();
    const rateLimiter = new RecordingRateLimiter();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "access-token",
      createdAtMillis: 1_000,
      updatedAtMillis: 1_000,
    });
    let detailCalls = 0;
    const client: RavelryClient = {
      async getCurrentUser() {
        throw new Error("not used");
      },
      async searchPatterns() {
        return {
          patterns: [{
            ravelryPatternId: 99,
            title: "Different Pattern",
            designerName: "Different Designer",
            canonicalUrl: "https://www.ravelry.com/patterns/library/different-pattern",
            availability: "unknown",
          }],
          pagination: { page: 1, pageCount: 1, resultCount: 1 },
        };
      },
      async getPatternById() {
        detailCalls += 1;
        return {
          ravelryPatternId: 99,
          title: "Different Pattern",
          designerName: "Different Designer",
          canonicalUrl: "https://www.ravelry.com/patterns/library/different-pattern",
          availability: "unknown",
        };
      },
    };

    await assert.rejects(
      importPatternByUrl({
        uid: "uid",
        tokenStore,
        client,
        rateLimiter,
        url: "https://www.ravelry.com/patterns/library/requested-pattern",
      }),
      (error: unknown) =>
        error instanceof RavelryPatternImportError && error.code === "pattern_not_found",
    );
    assert.equal(detailCalls, 0);
    assert.deepEqual(rateLimiter.calls, [{ uid: "uid", bucket: "search" }]);
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

  it("does not refresh expired Ravelry tokens before search or import buckets are consumed", async () => {
    const tokenStore = new MemoryTokenStore();
    await tokenStore.saveToken({
      uid: "uid",
      authType: "oauth2",
      accessToken: "expired-access-token",
      refreshToken: "refresh-token",
      expiresAtMillis: 999,
      createdAtMillis: 100,
      updatedAtMillis: 100,
    });
    let refreshCount = 0;
    const refresh: OAuthTokenRefresh = async () => {
      refreshCount += 1;
      return {
        accessToken: "fresh-access-token",
        refreshToken: "fresh-refresh-token",
        expiresAtMillis: 10_000,
      };
    };
    const client = {
      async getCurrentUser() {
        throw new Error("not used");
      },
      async searchPatterns() {
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
        refresh,
        nowMillis: () => 1_000,
        query: { query: "hat" },
      }),
      /ravelry_rate_limited/,
    );
    await assert.rejects(
      importPatternById({
        uid: "uid",
        tokenStore,
        client,
        rateLimiter: new BlockingRateLimiter(),
        refresh,
        nowMillis: () => 1_000,
        ravelryPatternId: 42,
      }),
      /ravelry_rate_limited/,
    );
    assert.equal(refreshCount, 0);
    assert.equal((await tokenStore.getToken("uid"))?.accessToken, "expired-access-token");
  });

  it("does not call Ravelry when the Firebase user has no stored Ravelry token", async () => {
    const tokenStore = new MemoryTokenStore();
    const rateLimiter = new RecordingRateLimiter();
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
        rateLimiter,
        query: { query: "hat" },
      }),
      /ravelry_not_connected/,
    );
    assert.equal(searchCount, 0);
    assert.deepEqual(rateLimiter.calls, []);
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
      connectionGeneration: 0,
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
