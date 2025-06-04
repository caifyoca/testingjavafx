-- Defines the structure for descriptors (nodes in the n-ary tree)
CREATE TABLE IF NOT EXISTS descriptors (
    id INTEGER PRIMARY KEY AUTO_INCREMENT, -- Unique identifier for the descriptor
    label TEXT NOT NULL,                   -- Display name of the descriptor
    parent_id INTEGER,                     -- ID of the parent descriptor; NULL for root nodes
    is_numeric BOOLEAN DEFAULT FALSE,      -- True if the descriptor is quantitative
    is_multistate BOOLEAN DEFAULT FALSE,   -- True if a nominal parent allows multiple child selections
    is_type_of_presence BOOLEAN DEFAULT FALSE, -- True if a nominal parent has specific presence states (oui/inconnu/jamais)
    is_hidden_defaultly BOOLEAN DEFAULT FALSE, -- True if the descriptor is hidden by default
    FOREIGN KEY (parent_id) REFERENCES descriptors(id) ON DELETE CASCADE -- Ensures data integrity
);

-- Defines rules for descriptor applicability and visibility
CREATE TABLE IF NOT EXISTS applicability_rules (
    id INTEGER PRIMARY KEY AUTO_INCREMENT, -- Unique identifier for the rule
    source_id INTEGER NOT NULL,            -- ID of the controlling descriptor
    target_id INTEGER NOT NULL,            -- ID of the controlled descriptor
    is_applicable_if BOOLEAN NOT NULL,     -- True if target becomes applicable/visible; False if it becomes inapplicable/grised out
    FOREIGN KEY (source_id) REFERENCES descriptors(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES descriptors(id) ON DELETE CASCADE
);

-- Stores the actual observed state or entered value for each descriptor
CREATE TABLE IF NOT EXISTS descriptor_states (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,         -- Unique identifier for the state entry
    descriptor_id INTEGER NOT NULL UNIQUE,         -- Foreign key to the descriptors table, ensuring one state entry per descriptor
    state_value TEXT,                              -- The observed state (e.g., 'present', 'unknown') or entered value
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- When the state was last updated
    FOREIGN KEY (descriptor_id) REFERENCES descriptors(id) ON DELETE CASCADE
);
