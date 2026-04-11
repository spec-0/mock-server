package io.spec0.mockserver.domain;

/** Response selection strategy for a mock server or a specific operation. */
public enum MockResponseStrategy {
  /** Pick a random variant from the available list on every request. */
  RANDOM,
  /** Always return variants in ascending displayOrder sequence; wraps around. */
  SEQUENTIAL,
  /** Rotate evenly across all variants in displayOrder; state tracked per-operation. */
  ROUND_ROBIN,
  /** Always return only the variant marked isDefault=true. */
  DEFAULT_ONLY
}
