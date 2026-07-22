import assert from "node:assert/strict";
import { describe, it } from "node:test";

import { connectionGenerationFromData, finiteNumber } from "./connectionGeneration";

describe("Ravelry connection generation normalization", () => {
  it("accepts only finite numbers", () => {
    assert.equal(finiteNumber(3), 3);
    assert.equal(finiteNumber(Number.NaN), undefined);
    assert.equal(finiteNumber(Number.POSITIVE_INFINITY), undefined);
    assert.equal(finiteNumber("3"), undefined);
  });

  it("truncates non-negative generations and defaults invalid values to zero", () => {
    assert.equal(connectionGenerationFromData({ connectionGeneration: 4.9 }), 4);
    assert.equal(connectionGenerationFromData({ connectionGeneration: -1 }), 0);
    assert.equal(connectionGenerationFromData({ connectionGeneration: Number.NaN }), 0);
    assert.equal(connectionGenerationFromData(undefined), 0);
  });
});
