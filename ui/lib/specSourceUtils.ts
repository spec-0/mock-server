/**
 * Spec record source types. Matches SpecRecordEntity.SourceType in backend.
 */
export type SpecSourceType =
  | "MANUAL_UPLOAD"
  | "GITHUB"
  | "RUNTIME_DETECTED"
  | "API_SPEC_CRD";

/**
 * Human-readable labels for spec source types.
 */
export const SPEC_SOURCE_LABELS: Record<SpecSourceType, string> = {
  MANUAL_UPLOAD: "Manual Upload",
  GITHUB: "GitHub",
  RUNTIME_DETECTED: "Runtime Discovery",
  API_SPEC_CRD: "ApiSpec CRD",
};

/**
 * Get display label for a spec source type.
 */
export function getSpecSourceLabel(sourceType: string | null | undefined): string {
  if (!sourceType) return "Unknown";
  return SPEC_SOURCE_LABELS[sourceType as SpecSourceType] ?? sourceType;
}

/**
 * Build GitHub blob URL from spec record metadata.
 * Format: https://github.com/{owner}/{repo}/blob/{sha}/{specFilePath}
 */
export function buildGithubBlobUrl(
  githubRepo: string | null | undefined,
  gitSha: string | null | undefined,
  specFilePath: string | null | undefined
): string | null {
  if (!githubRepo || !gitSha || !specFilePath) return null;
  // Normalize repo URL (remove .git, ensure https)
  const repo = githubRepo.replace(/\.git$/, "").replace("git@github.com:", "https://github.com/");
  const path = specFilePath.startsWith("/") ? specFilePath.slice(1) : specFilePath;
  return `${repo}/blob/${gitSha}/${path}`;
}
