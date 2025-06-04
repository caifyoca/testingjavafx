package com.example.narytree.db;

import com.example.narytree.model.Descriptor;
import com.example.narytree.model.ApplicabilityRule;
import com.example.narytree.model.DescriptorState; // Added import

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp; // Added import
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseManager {

    // ... (DB_URL, DB_USER, DB_PASSWORD, constructor, getConnection, closeConnection, initializeSchema as before)
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/narytree_db";
    private static final String DB_USER = "your_user";
    private static final String DB_PASSWORD = "your_password";
    private Connection connection;

    public DatabaseManager() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MariaDB JDBC Driver not found!");
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void initializeSchema() {
        // Re-run with potentially updated schema.sql (UNIQUE constraint)
        try (InputStream schemaStream = DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql");
             InputStreamReader reader = new InputStreamReader(schemaStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader);
             Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            if (schemaStream == null) {
                System.err.println("Could not find schema.sql in resources.");
                return;
            }

            String schemaSql = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator())); // Use system line separator
            // Split by semicolon followed by a line break, or just semicolon if it's the last statement.
            // This is a bit more robust than just splitting by semicolon.
            String[] statements = schemaSql.split(";" + System.lineSeparator() + "?|;" + "$");


            for (String statement : statements) {
                String trimmedStatement = statement.trim();
                if (trimmedStatement.isEmpty()) {
                    continue;
                }
                // Ensure statement ends with a semicolon if it's not a comment
                if (!trimmedStatement.endsWith(";") && !trimmedStatement.startsWith("--") && !trimmedStatement.startsWith("/*")) {
                    trimmedStatement += ";";
                }
                System.out.println("Executing schema statement: " + trimmedStatement.substring(0, Math.min(trimmedStatement.length(), 120)) + "...");
                stmt.executeUpdate(trimmedStatement);
            }
            System.out.println("Database schema initialized successfully or already exists (with potential updates).");

        } catch (SQLException e) {
            System.err.println("Error initializing schema: " + e.getMessage());
            // e.printStackTrace(); // Can be very verbose
        } catch (NullPointerException e) {
            System.err.println("Error: schema.sql not found in resources. Make sure it's in src/main/resources.");
            e.printStackTrace();
        }
        catch (Exception e) {
            System.err.println("Error reading or executing schema.sql: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // --- Descriptor CRUD methods (as before) ---
    public int addDescriptor(Descriptor descriptor) throws SQLException {
        String sql = "INSERT INTO descriptors (label, parent_id, is_numeric, is_multistate, is_type_of_presence, is_hidden_defaultly) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, descriptor.getLabel());
            if (descriptor.getParentId() != null) { pstmt.setInt(2, descriptor.getParentId()); } else { pstmt.setNull(2, java.sql.Types.INTEGER); }
            pstmt.setBoolean(3, descriptor.isNumeric());
            pstmt.setBoolean(4, descriptor.isMultistate());
            pstmt.setBoolean(5, descriptor.isTypeOfPresence());
            pstmt.setBoolean(6, descriptor.isHiddenDefaultly());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) throw new SQLException("Creating descriptor failed, no rows affected.");
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) { descriptor.setId(generatedKeys.getInt(1)); return descriptor.getId(); }
                else throw new SQLException("Creating descriptor failed, no ID obtained.");
            }
        }
    }
    public Descriptor getDescriptor(int descriptorId) throws SQLException {
        String sql = "SELECT * FROM descriptors WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, descriptorId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return mapRowToDescriptor(rs); }
        } return null;
    }
    public List<Descriptor> getAllDescriptors() throws SQLException {
        List<Descriptor> descriptors = new ArrayList<>(); String sql = "SELECT * FROM descriptors";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) descriptors.add(mapRowToDescriptor(rs));
        } return descriptors;
    }
    public List<Descriptor> getChildrenOfDescriptor(int parentId) throws SQLException {
        List<Descriptor> descriptors = new ArrayList<>(); String sql = "SELECT * FROM descriptors WHERE parent_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, parentId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) descriptors.add(mapRowToDescriptor(rs));}
        } return descriptors;
    }
    public List<Descriptor> getRootDescriptors() throws SQLException {
        List<Descriptor> descriptors = new ArrayList<>(); String sql = "SELECT * FROM descriptors WHERE parent_id IS NULL";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) descriptors.add(mapRowToDescriptor(rs));
        } return descriptors;
    }
    public boolean updateDescriptor(Descriptor descriptor) throws SQLException {
        String sql = "UPDATE descriptors SET label = ?, parent_id = ?, is_numeric = ?, is_multistate = ?, is_type_of_presence = ?, is_hidden_defaultly = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, descriptor.getLabel());
            if (descriptor.getParentId() != null) { pstmt.setInt(2, descriptor.getParentId()); } else { pstmt.setNull(2, java.sql.Types.INTEGER); }
            pstmt.setBoolean(3, descriptor.isNumeric());
            pstmt.setBoolean(4, descriptor.isMultistate());
            pstmt.setBoolean(5, descriptor.isTypeOfPresence());
            pstmt.setBoolean(6, descriptor.isHiddenDefaultly());
            pstmt.setInt(7, descriptor.getId());
            return pstmt.executeUpdate() > 0;
        }
    }
    public boolean deleteDescriptor(int descriptorId) throws SQLException {
        String sql = "DELETE FROM descriptors WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, descriptorId); return pstmt.executeUpdate() > 0;
        }
    }
    private Descriptor mapRowToDescriptor(ResultSet rs) throws SQLException {
        Descriptor d = new Descriptor(); d.setId(rs.getInt("id")); d.setLabel(rs.getString("label"));
        int parentId = rs.getInt("parent_id"); if (rs.wasNull()) d.setParentId(null); else d.setParentId(parentId);
        d.setNumeric(rs.getBoolean("is_numeric")); d.setMultistate(rs.getBoolean("is_multistate"));
        d.setTypeOfPresence(rs.getBoolean("is_type_of_presence")); d.setHiddenDefaultly(rs.getBoolean("is_hidden_defaultly"));
        return d;
    }

    // --- ApplicabilityRule CRUD methods (as before) ---
    public int addApplicabilityRule(ApplicabilityRule rule) throws SQLException {
        String sql = "INSERT INTO applicability_rules (source_id, target_id, is_applicable_if) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, rule.getSourceId()); pstmt.setInt(2, rule.getTargetId()); pstmt.setBoolean(3, rule.isApplicableIf());
            int affectedRows = pstmt.executeUpdate(); if (affectedRows == 0) throw new SQLException("Creating rule failed.");
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) { rule.setId(generatedKeys.getInt(1)); return rule.getId(); }
                else throw new SQLException("Creating rule failed, no ID.");
            }
        }
    }
    public ApplicabilityRule getApplicabilityRule(int ruleId) throws SQLException {
        String sql = "SELECT * FROM applicability_rules WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return mapRowToApplicabilityRule(rs); }
        } return null;
    }
    public List<ApplicabilityRule> getApplicabilityRulesBySourceId(int sourceDescriptorId) throws SQLException {
        List<ApplicabilityRule> rules = new ArrayList<>(); String sql = "SELECT * FROM applicability_rules WHERE source_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, sourceDescriptorId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) rules.add(mapRowToApplicabilityRule(rs)); }
        } return rules;
    }
    public List<ApplicabilityRule> getApplicabilityRulesByTargetId(int targetDescriptorId) throws SQLException {
         List<ApplicabilityRule> rules = new ArrayList<>(); String sql = "SELECT * FROM applicability_rules WHERE target_id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, targetDescriptorId);
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) rules.add(mapRowToApplicabilityRule(rs)); }
        } return rules;
    }
    public List<ApplicabilityRule> getAllApplicabilityRules() throws SQLException {
        List<ApplicabilityRule> rules = new ArrayList<>(); String sql = "SELECT * FROM applicability_rules";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) rules.add(mapRowToApplicabilityRule(rs));
        } return rules;
    }
    public boolean updateApplicabilityRule(ApplicabilityRule rule) throws SQLException {
        String sql = "UPDATE applicability_rules SET source_id = ?, target_id = ?, is_applicable_if = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rule.getSourceId()); pstmt.setInt(2, rule.getTargetId()); pstmt.setBoolean(3, rule.isApplicableIf()); pstmt.setInt(4, rule.getId());
            return pstmt.executeUpdate() > 0;
        }
    }
    public boolean deleteApplicabilityRule(int ruleId) throws SQLException {
        String sql = "DELETE FROM applicability_rules WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, ruleId); return pstmt.executeUpdate() > 0;
        }
    }
    private ApplicabilityRule mapRowToApplicabilityRule(ResultSet rs) throws SQLException {
        ApplicabilityRule rule = new ApplicabilityRule(); rule.setId(rs.getInt("id")); rule.setSourceId(rs.getInt("source_id"));
        rule.setTargetId(rs.getInt("target_id")); rule.setApplicableIf(rs.getBoolean("is_applicable_if")); return rule;
    }

   // --- DescriptorState CRUD methods (NEW) ---

   /**
    * Retrieves the stored state for a given descriptor.
    * @param descriptorId The ID of the descriptor.
    * @return DescriptorState object if found, null otherwise.
    * @throws SQLException if a database access error occurs.
    */
   public DescriptorState getDescriptorState(int descriptorId) throws SQLException {
       String sql = "SELECT id, descriptor_id, state_value, last_modified FROM descriptor_states WHERE descriptor_id = ?";
       try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
           pstmt.setInt(1, descriptorId);
           try (ResultSet rs = pstmt.executeQuery()) {
               if (rs.next()) {
                   return mapRowToDescriptorState(rs);
               }
           }
       }
       return null; // Return null if no state is found for this descriptor
   }

   /**
    * Saves (inserts or updates) the state for a given descriptor.
    * Uses INSERT ... ON DUPLICATE KEY UPDATE (UPSERT) based on the UNIQUE constraint on descriptor_id.
    * @param state The DescriptorState object to save. The id field of state is ignored for insert, used for feedback on update.
    * @return The ID of the inserted or updated record.
    * @throws SQLException if a database access error occurs.
    */
   public int saveDescriptorState(DescriptorState state) throws SQLException {
        // MariaDB UPSERT: INSERT ... ON DUPLICATE KEY UPDATE
        // The 'id' field in DescriptorState is auto-increment.
        // 'descriptor_id' has a UNIQUE constraint.
        String sql = "INSERT INTO descriptor_states (descriptor_id, state_value) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE state_value = VALUES(state_value), last_modified = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, state.getDescriptorId());
            pstmt.setString(2, state.getStateValue());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                // This case should ideally not happen with ON DUPLICATE KEY UPDATE unless the values are identical
                // and no update occurs. If it means "no insert and no update", something is odd.
                // Try to fetch the existing ID if no new key was generated.
                DescriptorState existingState = getDescriptorState(state.getDescriptorId());
                if(existingState != null) return existingState.getId();
                throw new SQLException("Saving descriptor state failed, no rows affected and no existing state found.");
            }

            // Check if a new row was inserted or an existing one updated.
            // getGeneratedKeys returns the ID if a new row was inserted.
            // If an update occurred, it might return nothing or the number of affected rows (driver dependent).
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    state.setId(generatedKeys.getInt(1)); // Set ID on object if it was an insert
                    return state.getId();
                } else {
                    // If no generated key, it was likely an update or no change.
                    // We need to retrieve the ID of the (potentially) updated row.
                    // This is guaranteed by the UNIQUE constraint on descriptor_id.
                    DescriptorState updatedState = getDescriptorState(state.getDescriptorId());
                    if (updatedState != null) {
                       state.setId(updatedState.getId()); // Update the ID on the passed object
                       return updatedState.getId();
                    } else {
                       // This should not happen if affectedRows > 0
                       throw new SQLException("Saving descriptor state indicated success, but could not retrieve ID.");
                    }
                }
            }
        }
   }

   /**
    * Deletes the stored state for a given descriptor.
    * @param descriptorId The ID of the descriptor whose state is to be deleted.
    * @return true if a state was deleted, false otherwise.
    * @throws SQLException if a database access error occurs.
    */
   public boolean deleteDescriptorState(int descriptorId) throws SQLException {
       String sql = "DELETE FROM descriptor_states WHERE descriptor_id = ?";
       try (Connection conn = getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
           pstmt.setInt(1, descriptorId);
           int affectedRows = pstmt.executeUpdate();
           return affectedRows > 0;
       }
   }


   private DescriptorState mapRowToDescriptorState(ResultSet rs) throws SQLException {
       return new DescriptorState(
               rs.getInt("id"),
               rs.getInt("descriptor_id"),
               rs.getString("state_value"),
               rs.getTimestamp("last_modified")
       );
   }

   // --- Main method for testing (updated) ---
   public static void main(String[] args) {
       DatabaseManager dbManager = new DatabaseManager();
       System.out.println("--- MariaDB Setup Instructions (ensure these are done) ---");
       System.out.println("1. Ensure MariaDB server is running.");
       System.out.println("2. Connect to MariaDB as a privileged user (e.g., root).");
       System.out.println("3. Execute the following SQL commands if not already done:");
       System.out.println("   CREATE DATABASE IF NOT EXISTS narytree_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;");
       System.out.println("   CREATE USER IF NOT EXISTS 'your_user'@'localhost' IDENTIFIED BY 'your_password';");
       System.out.println("   GRANT ALL PRIVILEGES ON narytree_db.* TO 'your_user'@'localhost';");
       System.out.println("   FLUSH PRIVILEGES;");
       System.out.println("4. Update DB_USER and DB_PASSWORD in DatabaseManager.java if you used different credentials.");
       System.out.println("------------------------------------");

       try {
           System.out.println("\nAttempting to initialize/update schema (with UNIQUE constraint on descriptor_states.descriptor_id)...");
           dbManager.initializeSchema();

           // --- Descriptor CRUD Examples (Uncomment to test, ensure IDs match for state tests) ---
           int desc1Id = -1, desc2Id = -1;

           System.out.println("\n--- Testing Descriptor CRUD Operations ---");
           // Clean up existing descriptors to ensure fresh test data for state tests
            // List<Descriptor> existingDescriptors = dbManager.getAllDescriptors();
            // for (Descriptor d : existingDescriptors) {
            //     dbManager.deleteDescriptorState(d.getId()); // Delete state first due to FK if any
            //     dbManager.deleteDescriptor(d.getId());
            // }
            // System.out.println("Cleaned up existing descriptors for fresh testing.");


           Descriptor d1 = new Descriptor(0, "Leaf Color", null, false, false, false, false);
           desc1Id = dbManager.addDescriptor(d1);
           System.out.println("Added Descriptor: " + d1.getLabel() + " with ID: " + desc1Id);

           Descriptor d2 = new Descriptor(0, "Petal Count", null, true, false, false, false); // Numeric
           desc2Id = dbManager.addDescriptor(d2);
           System.out.println("Added Descriptor: " + d2.getLabel() + " with ID: " + desc2Id);


           // --- DescriptorState CRUD Examples ---

           if (desc1Id != -1 && desc2Id != -1) {
               System.out.println("\n--- Testing DescriptorState CRUD Operations ---");

               // Add/Save state for Leaf Color (desc1Id)
               DescriptorState state1 = new DescriptorState(desc1Id, "present");
               int state1Id = dbManager.saveDescriptorState(state1);
               System.out.println("Saved state for descriptor " + desc1Id + ": '" + state1.getStateValue() + "', State ID: " + state1.getId() + " (returned: " + state1Id +")");

               // Get the state
               DescriptorState fetchedState1 = dbManager.getDescriptorState(desc1Id);
               if (fetchedState1 != null) {
                   System.out.println("Fetched state for " + desc1Id + ": '" + fetchedState1.getStateValue() + "', LastModified: " + fetchedState1.getLastModified());
               } else {
                   System.out.println("Could not fetch state for " + desc1Id);
               }

               // Update state for Leaf Color (desc1Id)
               state1.setStateValue("unknown");
               dbManager.saveDescriptorState(state1);
               System.out.println("Updated state for descriptor " + desc1Id + " to: '" + state1.getStateValue() + "'");
               fetchedState1 = dbManager.getDescriptorState(desc1Id);
               if (fetchedState1 != null) {
                   System.out.println("Fetched updated state for " + desc1Id + ": '" + fetchedState1.getStateValue() + "'");
               }

               // Add state for Petal Count (desc2Id) - numeric value stored as string
               DescriptorState state2 = new DescriptorState(desc2Id, "5");
               dbManager.saveDescriptorState(state2);
               System.out.println("Saved state for descriptor " + desc2Id + ": '" + state2.getStateValue() + "', State ID: " + state2.getId());

               // Test UPSERT by re-saving state2 with a new value
               state2.setStateValue("7");
               dbManager.saveDescriptorState(state2);
                System.out.println("UPSERTED state for descriptor " + desc2Id + " to: '" + state2.getStateValue() + "', State ID should be same: " + state2.getId());
                DescriptorState fetchedState2 = dbManager.getDescriptorState(desc2Id);
                 if (fetchedState2 != null) {
                   System.out.println("Fetched UPSERTED state for " + desc2Id + ": '" + fetchedState2.getStateValue() + "', ID: " + fetchedState2.getId());
               }


               // Delete state
               System.out.println("\nDeleting state for descriptor " + desc1Id);
               boolean deleted = dbManager.deleteDescriptorState(desc1Id);
               System.out.println("State deletion successful: " + deleted);
               fetchedState1 = dbManager.getDescriptorState(desc1Id);
               System.out.println("Attempt to fetch deleted state for " + desc1Id + ": " + (fetchedState1 == null ? "Not found (Correct)" : "Found (Error)"));
           } else {
               System.out.println("\nSkipping DescriptorState CRUD tests as descriptor IDs are not set (Descriptor tests might be commented out).");
           }


       } catch (SQLException e) {
           System.err.println("\n--- DATABASE OPERATION FAILED ---");
           e.printStackTrace();
       } finally {
           dbManager.closeConnection();
           System.out.println("\nDatabase connection closed.");
       }
   }
}
