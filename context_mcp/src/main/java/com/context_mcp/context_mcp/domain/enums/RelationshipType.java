package com.context_mcp.context_mcp.domain.enums;

/**
 * Types of edges in the context relationship graph.
 */
public enum RelationshipType {
    BELONGS_TO_PROJECT,
    BELONGS_TO_TOPIC,
    DERIVED_FROM,
    REPLACES,
    SUPERSEDES,
    SUPPORTS,
    CONTRADICTS,
    REFERENCES,
    ATTACHED_TO,
    GENERATED_FOR,
    DECISION_FOR,
    BLOCKS,
    FOLLOWS_UP,
    RELATED_TO
}
