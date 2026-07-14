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

const RAVELRY_CREDENTIAL_PATTERNS = [
  {
    regex: /BuildConfig\.RAVELRY_(?:BASIC_AUTH|OAUTH2)_[A-Z_]+/,
    label: "Ravelry credential BuildConfig read",
  },
  {
    regex: /KNITTOOLS_(?:ALLOW_EMBEDDED_RAVELRY_SECRETS|RAVELRY_[A-Z0-9_]+)/,
    label: "Ravelry credential environment gate",
  },
  {
    regex: /Authorization["']?\s*,\s*["'](?:Basic|Bearer)\b/,
    label: "Ravelry Authorization header construction",
  },
];

const decision = fs.readFileSync(securityDecisionPath, "utf8");
const hasRavelryDecision = decision.includes("Ravelry embedded credentials");
const isAcceptedRisk = hasRavelryDecision && decision.includes("Status: Accepted risk");
const isRemovedFromAndroid =
  hasRavelryDecision && decision.includes("Status: Removed from Android");

if (!isAcceptedRisk && !isRemovedFromAndroid) {
  throw new Error("Ravelry accepted-risk decision is missing or has an unknown status.");
}

if (isRemovedFromAndroid) {
  const currentMatches = currentRavelryCredentialMatches();
  if (currentMatches.length > 0) {
    throw new Error(
      "Ravelry accepted-risk decision is removed, but Android still has credential surfaces:\n" +
        currentMatches.join("\n"),
    );
  }

  const updatedFindings = markRavelryCredentialFindings({
    verdict: "fixed",
    reasoning:
      "Ravelryn vanha backenditon credential-riski on poistettu Androidista. " +
      "Nykyinen config/security-decisions.md merkkaa riskin superseded-tilaan, " +
      "eika Android-lahteista loydy Ravelry BuildConfig credential -lukuja, " +
      "Ravelry credential -ymparistomuuttujia tai Ravelry Authorization -headerin muodostusta. " +
      "Phase 9 release-surface -vahti estaa vanhan Android-secret-pinnan paluun.",
    runId: "manual-ravelry-removed-from-android",
  });

  console.log(`Marked ${updatedFindings} historical Ravelry credential finding(s) as fixed.`);
} else {
  const updatedFindings = markRavelryCredentialFindings({
    verdict: "accepted-risk",
    reasoning:
      "KnitTools keeps Ravelry backendittomana tietoisena tuotepaatoksena. " +
      "Credentialien APK-paljastuminen on dokumentoitu config/security-decisions.md:ssa " +
      "hyvaksyttyna riskina; automatisoitu export piilottaa taman vain Ravelryyn rajatun riskin.",
    runId: "manual-ravelry-accepted-risk",
  });

  console.log(`Marked ${updatedFindings} documented Ravelry credential finding(s) as accepted-risk.`);
}

function markRavelryCredentialFindings({ verdict, reasoning, runId }) {
  let updatedFindings = 0;

  for (const recordPath of walkJsonFiles(fileRecordsRoot)) {
    const record = JSON.parse(fs.readFileSync(recordPath, "utf8"));
    if (!RAVELRY_ACCEPTED_RISK_FILES.has(record.filePath)) continue;

    let changed = false;
    for (const finding of record.findings ?? []) {
      if (!isRavelryCredentialFinding(finding)) continue;

      finding.revalidation = {
        verdict,
        reasoning,
        revalidatedAt: new Date().toISOString(),
        runId,
        model: "manual",
      };
      changed = true;
      updatedFindings += 1;
    }

    if (changed) {
      fs.writeFileSync(recordPath, `${JSON.stringify(record, null, 2)}\n`);
    }
  }

  return updatedFindings;
}

function isRavelryCredentialFinding(finding) {
  const text = `${finding.title ?? ""}\n${finding.description ?? ""}\n${finding.recommendation ?? ""}`;
  return finding.vulnSlug === "secrets-exposure" && /Ravelry/i.test(text);
}

function currentRavelryCredentialMatches() {
  const matches = [];
  const sourcePaths = [
    path.join(projectRoot, "app", "build.gradle.kts"),
    ...walkKotlinFiles(path.join(projectRoot, "app", "src", "main", "java")),
  ];

  for (const sourcePath of sourcePaths) {
    if (!fs.existsSync(sourcePath)) continue;
    const content = fs.readFileSync(sourcePath, "utf8");
    const relativePath = path.relative(projectRoot, sourcePath).replaceAll(path.sep, "/");
    const patterns = /Ravelry/i.test(relativePath)
      ? RAVELRY_CREDENTIAL_PATTERNS
      : RAVELRY_CREDENTIAL_PATTERNS.slice(0, 2);

    for (const { regex, label } of patterns) {
      const flags = regex.flags.includes("g") ? regex.flags : `${regex.flags}g`;
      const globalRegex = new RegExp(regex.source, flags);
      for (const match of content.matchAll(globalRegex)) {
        matches.push(`${relativePath}:${lineNumberAt(content, match.index ?? 0)}: ${label}`);
      }
    }
  }

  return matches;
}

function* walkKotlinFiles(root) {
  if (!fs.existsSync(root)) return;
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const fullPath = path.join(root, entry.name);
    if (entry.isDirectory()) {
      yield* walkKotlinFiles(fullPath);
    } else if (entry.isFile() && entry.name.endsWith(".kt")) {
      yield fullPath;
    }
  }
}

function lineNumberAt(content, index) {
  return content.slice(0, index).split(/\r\n|\r|\n/).length;
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
