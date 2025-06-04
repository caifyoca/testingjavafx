-- This table stores descriptors, which are used to describe various entities.
CREATE TABLE descriptors (
    id INTEGER PRIMARY KEY, -- Unique identifier for the descriptor
    label TEXT, -- Human-readable label for the descriptor
    parent_id INTEGER, -- ID of the parent descriptor, forming a hierarchy
    is_numeric BOOLEAN, -- True if the descriptor represents a numeric value
    is_multistate BOOLEAN, -- True if the descriptor can have multiple states
    is_type_of_presence BOOLEAN, -- True if the descriptor indicates presence/absence
    is_hidden_defaultly BOOLEAN, -- True if the descriptor is hidden by default
    FOREIGN KEY (parent_id) REFERENCES descriptors(id) -- Enforces referential integrity
);

-- This table stores applicability rules, which define when and how descriptors apply.
CREATE TABLE applicability_rules (
    id INTEGER PRIMARY KEY, -- Unique identifier for the applicability rule
    source_id INTEGER, -- ID of the source descriptor
    target_id INTEGER, -- ID of the target descriptor
    is_applicable_if BOOLEAN, -- Condition for applicability (e.g., True if applicable when source is present)
    FOREIGN KEY (source_id) REFERENCES descriptors(id), -- Enforces referential integrity
    FOREIGN KEY (target_id) REFERENCES descriptors(id) -- Enforces referential integrity
);
