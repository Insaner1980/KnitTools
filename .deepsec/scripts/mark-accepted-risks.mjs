import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const deepsecRoot = path.resolve(here, "..");
const projectRoot = path.resolve(deepsecRoot, "..");
const securityDecisionPath = path.join(projectRoot, "config", "security-decisions.md");
const fileRecordsRoot = path.join(deepsecRoot, "data", "knittools", "files");

const RAVELRY_ACCEPTED_RISK_FILES = new Set([
  "app/build.gradle.kts",
  "app/src/main/java/com/finnvek/knittools/auth/RavelryAuthManager.kt",
  "app/src/main/java/com/finnvek/knittools/data/remote/RavelryApiService.kt",
]);

const decision = fs.readFileSync(securityDecisionPath, "utf8");
if (!decision.includes("Ravelry embedded credentials") || !decision.includes("Status: Accepted risk")) {
  throw new Error("Ravelry accepted-risk decision is missing or no longer accepted.");
}

let updatedFindings = 0;

for (const recordPath of walkJsonFiles(fileRecordsRoot)) {
  const record = JSON.parse(fs.readFileSync(recordPath, "utf8"));
  if (!RAVELRY_ACCEPTED_RISK_FILES.has(record.filePath)) continue;

  let changed = false;
  for (const finding of record.findings ?? []) {
    if (!isRavelryCredentialFinding(finding)) continue;

    finding.revalidation = {
      verdict: "accepted-risk",
      reasoning:
        "KnitTools keeps Ravelry backendittomana tietoisena tuotepaatoksena. " +
        "Credentialien APK-paljastuminen on dokumentoitu config/security-decisions.md:ssa " +
        "hyvaksyttyna riskina; automatisoitu export piilottaa taman vain Ravelryyn rajatun riskin.",
      revalidatedAt: new Date().toISOString(),
      runId: "manual-ravelry-accepted-risk",
      model: "manual",
    };
    changed = true;
    updatedFindings += 1;
  }

  if (changed) {
    fs.writeFileSync(recordPath, `${JSON.stringify(record, null, 2)}\n`);
  }
}

console.log(`Marked ${updatedFindings} documented Ravelry credential finding(s) as accepted-risk.`);

function isRavelryCredentialFinding(finding) {
  const text = `${finding.title ?? ""}\n${finding.description ?? ""}\n${finding.recommendation ?? ""}`;
  return finding.vulnSlug === "secrets-exposure" && /Ravelry/i.test(text);
}

function* walkJsonFiles(root) {
  if (!fs.existsSync(root)) return;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      yield* walkJsonFiles(fullPath);
    } else if (entry.isFile() && entry.name.endsWith(".json")) {
      yield fullPath;
    }
  }
}
