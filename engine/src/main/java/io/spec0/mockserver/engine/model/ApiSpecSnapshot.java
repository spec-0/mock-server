package io.spec0.mockserver.engine.model;

import java.util.UUID;

/** Immutable API spec as seen by the engine (no persistence annotations). */
public record ApiSpecSnapshot(
    UUID specId, String specName, String specContent, String specHash, String specVersion) {}
