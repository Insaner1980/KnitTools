export function finiteNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

export function connectionGenerationFromData(
  value: Record<string, unknown> | undefined,
): number {
  const generation = finiteNumber(value?.connectionGeneration);
  return generation !== undefined && generation >= 0 ? Math.trunc(generation) : 0;
}
