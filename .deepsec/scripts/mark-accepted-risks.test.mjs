import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const source = process.env.DEEPSEC_MARKER_SOURCE ?? fileURLToPath(new URL("./mark-accepted-risks.mjs", import.meta.url));
const currentDecision = fs.readFileSync(new URL("../../config/security-decisions.md", import.meta.url), "utf8");
const heading = "## Ravelry embedded credentials\n\n";

function runDecision(decision, expectedVerdict, credentialPath = null) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "knittools-risk-test-"));
  try {
    const script = path.join(root, ".deepsec/scripts/mark-accepted-risks.mjs");
    const record = path.join(root, ".deepsec/data/knittools/files/fixture.json");
    fs.mkdirSync(path.dirname(script), { recursive: true });
    fs.mkdirSync(path.dirname(record), { recursive: true });
    fs.mkdirSync(path.join(root, "config"));
    fs.copyFileSync(source, script);
    fs.writeFileSync(path.join(root, "config/security-decisions.md"), decision);
    fs.mkdirSync(path.join(root, "app"));
    fs.writeFileSync(path.join(root, "app/build.gradle.kts"), "");
    if (credentialPath) {
      const credentialFile = path.join(root, credentialPath);
      fs.mkdirSync(path.dirname(credentialFile), { recursive: true });
      fs.writeFileSync(credentialFile, "val synthetic = BuildConfig.RAVELRY_OAUTH2_SYNTHETIC");
    }
    const initial = JSON.stringify({
      filePath: "app/build.gradle.kts",
      findings: [
        {
          findingId: "finding_6e03ceb481162a46",
          producedByRunId: "20260514063758-293653dff198bae7",
          vulnSlug: "secrets-exposure",
          title: "Synthetic Ravelry credential fixture",
        },
        {
          findingId: "finding_6e03ceb481162a46",
          producedByRunId: "new-scan",
          vulnSlug: "secrets-exposure",
          title: "Synthetic Ravelry credential fixture",
        },
        {
          findingId: "finding_new_ravelry",
          producedByRunId: "new-scan",
          vulnSlug: "secrets-exposure",
          title: "New Ravelry credential in the same Gradle file",
        },
        { vulnSlug: "secrets-exposure", title: "Unrelated synthetic finding" },
      ],
    });
    fs.writeFileSync(record, initial);
    const result = spawnSync(process.execPath, [script], { encoding: "utf8", timeout: 10000 });
    assert.ifError(result.error);
    if (expectedVerdict === null) {
      assert.notEqual(result.status, 0, "Invalid decision must fail before updating records");
      assert.equal(fs.readFileSync(record, "utf8"), initial);
    } else {
      assert.equal(result.status, 0, result.stderr);
      const findings = JSON.parse(fs.readFileSync(record, "utf8")).findings;
      assert.equal(findings[0].revalidation.verdict, expectedVerdict);
      assert.deepEqual(findings.slice(1), JSON.parse(initial).findings.slice(1));
    }
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

for (const [name, decision] of [
  ["missing status", heading + "No decision yet"],
  ["unknown status", heading + "Status: Pending"],
  ["status only in next section", heading + "## Other\nStatus: Accepted risk"],
  ["unknown plus other accepted", heading + "Status: Pending\n## Other\nStatus: Accepted risk"],
  ["other section before target", "## Other\nStatus: Accepted risk\n" + heading],
  ["conflicting statuses", heading + "Status: Accepted risk\nStatus: Removed from Android"],
  ["known plus unknown", heading + "Status: Accepted risk\nStatus: Pending"],
  ["duplicate status", heading + "Status: Accepted risk\nStatus: Accepted risk"],
  ["status prefix", heading + "Status: Accepted risky"],
  ["same-line conflict", heading + "Status: Accepted risk; Status: Removed from Android"],
  ["heading only in prose", "See Ravelry embedded credentials\nStatus: Accepted risk"],
  ["duplicate section", heading + "Status: Accepted risk\n" + heading + "Status: Removed from Android"],
]) {
  test(`rejects Ravelry decision: ${name}`, () => runDecision(decision, null));
}

test("interprets the current documented decision as fixed", () => runDecision(currentDecision, "fixed"));
for (const sourcePath of [
  "app/build.gradle.kts",
  "app/src/debug/java/Synthetic.kt",
  "app/src/debugShared/kotlin/Synthetic.kt",
]) {
  test(`keeps the removed-source guard in ${sourcePath}`, () => runDecision(currentDecision, null, sourcePath));
}
test("accepts exactly one accepted status in a test copy only", () =>
  runDecision(heading + "Status: Accepted risk\n## Other\nStatus: Removed from Android", "accepted-risk"));
test("ignores another section's accepted status when Ravelry is removed", () =>
  runDecision(heading + "Status: Removed from Android\n## Other\nStatus: Accepted risk", "fixed"));
