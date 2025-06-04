package com.example.narytree.db;

import com.example.narytree.model.Descriptor;
import com.example.narytree.model.ApplicabilityRule;
import com.example.narytree.model.DescriptorState;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

// Use OrderAnnotation if tests depend on each other (e.g. create then read)
// For more independent tests, this is not strictly needed but helps structure.
@TestMethodOrder(OrderAnnotation.class)
public class DatabaseManagerTest {

    private static DatabaseManager dbManager;

    // Test data placeholders - will be initialized in tests
    private Descriptor testDesc1, testDesc2, testDescChild;
    private ApplicabilityRule testRule1;
    private DescriptorState testState1, testState2;

    @BeforeAll
    static void setUpAll() {
        // IMPORTANT: This test suite assumes a specific MariaDB setup.
        // It will connect to the DB defined in DatabaseManager (DB_URL, DB_USER, DB_PASSWORD).
        // This database SHOULD BE A TEST DATABASE, not your production or main development DB,
        // as tests will add, modify, and delete data.
        // The initializeSchema() method is called, which will attempt to create/update tables.
        dbManager = new DatabaseManager();
        System.out.println("Initializing schema for tests... (Ensure this is a TEST DATABASE)");
        dbManager.initializeSchema(); // Make sure schema is up-to-date
    }

    @BeforeEach
    void setUpEach() throws SQLException {
        // Clean up potential leftover data from previous failed tests or runs for specific tables
        // More robust cleanup would involve deleting all data from tables in specific order
        // For now, we rely on delete operations in tests or @AfterEach for specific entities.
        // Or, clear tables more broadly here if needed.
        // Example: dbManager.getConnection().createStatement().executeUpdate("DELETE FROM descriptor_states");
        //          dbManager.getConnection().createStatement().executeUpdate("DELETE FROM applicability_rules");
        //          dbManager.getConnection().createStatement().executeUpdate("DELETE FROM descriptors");
        // This is aggressive and requires re-adding common test data each time.
        // A balance is needed. For ordered tests, we can be more targeted.

        // Initialize some basic descriptor objects for use in tests
        testDesc1 = new Descriptor(0, "Test Root 1", null, false, false, false, false);
        testDesc2 = new Descriptor(0, "Test Root 2 (Numeric)", null, true, false, false, false);
    }

    @AfterEach
    void tearDownEach() throws SQLException {
        // Clean up: delete any states, rules, and descriptors created by the test method
        // This makes tests more idempotent.
        // Note: Order of deletion matters due to foreign key constraints.
        // States and Rules depend on Descriptors.
        if (testState1 != null && testState1.getId() != 0) {
            try { dbManager.deleteDescriptorState(testState1.getDescriptorId()); } catch (SQLException e) {/*ignore if already deleted*/}
        }
        if (testState2 != null && testState2.getId() != 0) {
             try { dbManager.deleteDescriptorState(testState2.getDescriptorId()); } catch (SQLException e) {/*ignore*/}
        }
        if (testRule1 != null && testRule1.getId() != 0) {
            try { dbManager.deleteApplicabilityRule(testRule1.getId()); } catch (SQLException e) {/*ignore*/}
        }
        if (testDescChild != null && testDescChild.getId() != 0) { // Delete child first
             try { dbManager.deleteDescriptor(testDescChild.getId()); } catch (SQLException e) {/*ignore*/}
        }
        if (testDesc1 != null && testDesc1.getId() != 0) {
            try { dbManager.deleteDescriptor(testDesc1.getId()); } catch (SQLException e) {/*ignore*/}
        }
        if (testDesc2 != null && testDesc2.getId() != 0) {
            try { dbManager.deleteDescriptor(testDesc2.getId()); } catch (SQLException e) {/*ignore*/}
        }
         testDesc1 = null; testDesc2 = null; testDescChild = null;
         testRule1 = null; testState1 = null; testState2 = null;
    }

    @Test
    @Order(1)
    void testAddAndGetDescriptor() throws SQLException {
        int id = dbManager.addDescriptor(testDesc1);
        assertTrue(id > 0, "Descriptor ID should be positive after add.");
        testDesc1.setId(id); // Keep track for cleanup

        Descriptor fetched = dbManager.getDescriptor(id);
        assertNotNull(fetched, "Fetched descriptor should not be null.");
        assertEquals(testDesc1.getLabel(), fetched.getLabel(), "Fetched label should match original.");
        assertEquals(testDesc1.isNumeric(), fetched.isNumeric(), "Fetched isNumeric should match.");
    }

    @Test
    @Order(2)
    void testUpdateDescriptor() throws SQLException {
        dbManager.addDescriptor(testDesc1); // id will be set on testDesc1
        assertNotEquals(0, testDesc1.getId(), "ID should be set before update");

        testDesc1.setLabel("Updated Test Root 1");
        testDesc1.setNumeric(true);
        boolean updated = dbManager.updateDescriptor(testDesc1);
        assertTrue(updated, "Update operation should return true.");

        Descriptor fetched = dbManager.getDescriptor(testDesc1.getId());
        assertEquals("Updated Test Root 1", fetched.getLabel());
        assertTrue(fetched.isNumeric());
    }

    @Test
    @Order(3)
    void testDeleteDescriptor() throws SQLException {
        int id = dbManager.addDescriptor(testDesc1);
        testDesc1.setId(id); // For cleanup, though it will be deleted by the test

        boolean deleted = dbManager.deleteDescriptor(id);
        assertTrue(deleted, "Delete should return true for existing descriptor.");

        Descriptor fetched = dbManager.getDescriptor(id);
        assertNull(fetched, "Descriptor should be null after deletion.");
        testDesc1.setId(0); // Mark as deleted for AfterEach
    }


    @Test
    @Order(4)
    void testAddAndGetApplicabilityRule() throws SQLException {
        // Need source and target descriptors
        int sourceId = dbManager.addDescriptor(testDesc1);
        testDesc1.setId(sourceId);
        int targetId = dbManager.addDescriptor(testDesc2);
        testDesc2.setId(targetId);

        testRule1 = new ApplicabilityRule(0, sourceId, targetId, true);
        int ruleId = dbManager.addApplicabilityRule(testRule1);
        assertTrue(ruleId > 0);
        testRule1.setId(ruleId);

        ApplicabilityRule fetched = dbManager.getApplicabilityRule(ruleId);
        assertNotNull(fetched);
        assertEquals(sourceId, fetched.getSourceId());
        assertEquals(targetId, fetched.getTargetId());
        assertTrue(fetched.isApplicableIf());
    }

    @Test
    @Order(5)
    void testGetChildrenAndRootDescriptors() throws SQLException {
        int root1Id = dbManager.addDescriptor(testDesc1); // parent_id is null
        testDesc1.setId(root1Id);

        testDescChild = new Descriptor(0, "Child of Root 1", root1Id, false, false, false, false);
        int child1Id = dbManager.addDescriptor(testDescChild);
        testDescChild.setId(child1Id);

        List<Descriptor> roots = dbManager.getRootDescriptors();
        assertTrue(roots.stream().anyMatch(d -> d.getId() == root1Id), "Root1 should be in root descriptors");

        List<Descriptor> children = dbManager.getChildrenOfDescriptor(root1Id);
        assertEquals(1, children.size(), "Root1 should have one child.");
        assertEquals(child1Id, children.get(0).getId(), "Child ID should match.");
    }


    @Test
    @Order(6)
    void testSaveAndGetDescriptorState_Insert() throws SQLException {
        int descId = dbManager.addDescriptor(testDesc1);
        testDesc1.setId(descId);

        testState1 = new DescriptorState(descId, "present_test");
        int stateId = dbManager.saveDescriptorState(testState1); // UPSERT
        assertTrue(stateId > 0, "State ID should be positive after save.");
        testState1.setId(stateId);


        DescriptorState fetched = dbManager.getDescriptorState(descId);
        assertNotNull(fetched, "Fetched state should not be null.");
        assertEquals("present_test", fetched.getStateValue());
        assertEquals(stateId, fetched.getId(), "Fetched state ID should match generated/returned ID.");
    }

    @Test
    @Order(7)
    void testSaveDescriptorState_Update() throws SQLException {
        int descId = dbManager.addDescriptor(testDesc1);
        testDesc1.setId(descId);

        testState1 = new DescriptorState(descId, "initial_value");
        dbManager.saveDescriptorState(testState1); // First save (insert)
        assertNotEquals(0, testState1.getId(), "State ID should be set after first save.");


        testState1.setStateValue("updated_value");
        int updatedStateId = dbManager.saveDescriptorState(testState1); // Second save (update)
        assertEquals(testState1.getId(), updatedStateId, "UPSERT should return the same ID for the updated row.");


        DescriptorState fetched = dbManager.getDescriptorState(descId);
        assertNotNull(fetched);
        assertEquals("updated_value", fetched.getStateValue());
    }

    @Test
    @Order(8)
    void testDeleteDescriptorState() throws SQLException {
        int descId = dbManager.addDescriptor(testDesc1);
        testDesc1.setId(descId);

        testState1 = new DescriptorState(descId, "to_delete");
        dbManager.saveDescriptorState(testState1);
        assertNotEquals(0, testState1.getId(), "State must exist before delete test.");

        boolean deleted = dbManager.deleteDescriptorState(descId);
        assertTrue(deleted, "Delete state should return true.");

        DescriptorState fetched = dbManager.getDescriptorState(descId);
        assertNull(fetched, "State should be null after deletion.");
        testState1.setId(0); // Mark as deleted for AfterEach
    }
}
