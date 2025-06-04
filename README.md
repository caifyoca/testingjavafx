# N-ary Tree Descriptor Management Application

## Project Overview
This project is a JavaFX application designed to manage n-ary tree structures of descriptors. Descriptors can be quantitative (numeric) or qualitative (nominal, type of presence). The application allows users to define these descriptors, establish parent-child relationships, and set up applicability rules that govern how descriptors affect each other's visibility and enabled state based on their current state. The data is persisted in a MariaDB database.

## Features
- **Descriptor Management**: Create, view, update, and delete descriptors.
  - Hierarchical (n-ary tree) structure with parent-child relationships.
  - Support for different descriptor types:
    - **Numeric/Quantitative**: Represents measurable values.
      - Parent numeric descriptors (or root numeric descriptors) display with "Present", "Unknown", "Inapplicable" state checkboxes.
      - Child numeric descriptors (that are not parents themselves) display with a label and a text input field for the numeric value.
    - **Nominal/Qualitative (Standard)**: Represents named categories. Can be a parent (e.g., "Leaf Shape") or a child (e.g., "Ovate", "Lanceolate"). Supports `is_multistate` for parents to allow selection of multiple children.
    - **Nominal/Qualitative (Type of Presence)**: A special nominal type that has predefined states: "Oui (Present)", "Inconnu (Unknown)", "Jamais (Inapplicable)". "Non (Absent)" is implied if "Oui" is not active.
  - `isHiddenDefaultly` property: Descriptors can be initially hidden in the UI.
- **Applicability Rules**: Define rules where the state of one descriptor (source) affects another (target).
  - `is_applicable_if = TRUE`: Target becomes applicable/visible if the source is "present". If target was `isHiddenDefaultly`, it's revealed.
  - `is_applicable_if = FALSE`: Target becomes inapplicable/grised out if the source is "present".
- **State Persistence**: User-defined states for descriptors are saved to a MariaDB database.
- **Dynamic UI**:
  - TreeView visualization of the descriptor hierarchy.
  - Details pane dynamically updates based on the selected descriptor's type and properties, showing relevant controls (checkboxes, text fields).
  - UI elements (tree cells, detail controls) are styled or disabled/enabled based on `isHiddenDefaultly` properties and active applicability rules.
- **Database Initialization**: The application can initialize its own database schema if tables do not exist.
- **Unit Tests**: JUnit 5 tests for database interaction logic.

## Database Schema
The application uses three main tables: `descriptors`, `applicability_rules`, and `descriptor_states`.

-   **`descriptors`**: Stores the definition of each descriptor.
    *   `id`: Primary Key, Auto-increment.
    *   `label`: Text, display name.
    *   `parent_id`: Integer, Foreign Key to `descriptors(id)` (for tree structure).
    *   `is_numeric`: Boolean. TRUE signifies a quantitative descriptor. If it's a parent, it displays with 'Present', 'Unknown', 'Inapplicable' checkboxes. Its direct non-parent children will typically show an input field for a numeric value.
    *   `is_multistate`: Boolean (for nominal parents). Only for nominal (non-numeric) parent descriptors. If TRUE, multiple direct or indirect children can be selected simultaneously. If FALSE, selecting one child/descendant may disable selection of its siblings/cousins under this parent.
    *   `is_type_of_presence`: Boolean (for special nominal type). Only for nominal parent descriptors. If TRUE, the descriptor displays with 'Oui (Present)', 'Inconnu (Unknown)', and 'Jamais (Inapplicable)' checkboxes. Often, such descriptors might not have children.
    *   `is_hidden_defaultly`: Boolean. If TRUE, the descriptor (and its associated UI controls) are not visible by default and typically require an applicability rule to reveal them.
-   **`applicability_rules`**: Defines how descriptors affect each other.
    *   `id`: Primary Key, Auto-increment.
    *   `source_id`: Integer, Foreign Key to `descriptors(id)`.
    *   `target_id`: Integer, Foreign Key to `descriptors(id)`.
    *   `is_applicable_if`: Boolean (determines effect on target). This rule is triggered when the `source_id` descriptor is marked as 'present'.
      - If `is_applicable_if` is TRUE: The `target_id` descriptor becomes applicable. If the `target_id` descriptor had `is_hidden_defaultly = TRUE`, it becomes visible.
      - If `is_applicable_if` is FALSE: The `target_id` descriptor becomes inapplicable (e.g., its UI controls are disabled or 'grised out').
-   **`descriptor_states`**: Stores the user-set state for each descriptor.
    *   `id`: Primary Key, Auto-increment.
    *   `descriptor_id`: Integer, Foreign Key to `descriptors(id)`, **UNIQUE** (one state per descriptor).
    *   `state_value`: Text. Stores the observed state (like 'present', 'oui', 'unknown', 'inapplicable') or an entered value (like a number for numeric descriptors, or 'true'/'false' for a simple selection).
    *   `last_modified`: Timestamp.

Refer to `src/main/resources/schema.sql` for the exact table definitions.

## Core UI and Rule Logic
-   **UI (`com.example.narytree.ui` package)**:
    -   `MainApp.java`: JavaFX Application entry point, initializes DB, loads FXML.
    -   `MainController.java`: Handles UI logic, user interactions, populates the TreeView, displays descriptor details, and manages state changes. It also orchestrates the application of applicability rules. It uses a `TreeView` to display the descriptor hierarchy and dynamically populates a `detailsPane` with controls relevant to the selected descriptor's type via its `showDescriptorDetails()` method.
    -   `MainView.fxml`: Defines the layout of the main application window.
    -   `DescriptorTreeCell.java`: Custom `TreeCell` for rendering descriptors in the `TreeView`, including styling based on rules and properties. This is crucial for visually indicating states like 'disabled' or 'hidden by default'.
-   **Rule Engine (in `MainController.java`)**:
    -   User interaction with specific state checkboxes (e.g., 'Present' for numeric parents, 'Oui (Present)' for Type of Presence) in the `detailsPane` triggers `evaluateApplicabilityRules` for the current descriptor (acting as a source).
    -   This method fetches all rules where the changed descriptor is a `source_id`.
    -   For each rule, `applyRuleEffect` updates an internal map (`descriptorRuleEffects`) that tracks whether target descriptors should be styled as 'disabled' or 'hidden_revealed'.
    -   The `TreeView` is then refreshed, and `DescriptorTreeCell` uses the `descriptorRuleEffects` map and the descriptor's `isHiddenDefaultly` property to apply appropriate visual styles (e.g., graying out text).
    -   Similarly, the `showDescriptorDetails()` method consults the `descriptorRuleEffects` map to enable or disable controls in the details pane for the selected descriptor.
    -   On application startup, `applyInitialRuleEffectsFromLoadedStates` processes any persisted "present" states to set the initial UI according to rules.
-   **Data State Interaction**:
    -   When a descriptor is selected, `showDescriptorDetails()` loads its saved state (if any) from the `descriptor_states` table and sets the values of the corresponding UI controls (checkboxes, text fields).
    -   The "Save Current State" button in the `detailsPane` triggers `handleUpdateState()` in `MainController`, which collects the current UI values and persists them to the `descriptor_states` table.
    -   After saving a state, the rule engine is re-evaluated for the affected descriptor to update the UI accordingly.

## Project Setup

### Prerequisites
-   Java Development Kit (JDK) 11 or higher.
-   Apache Maven 3.6.x or higher.
-   MariaDB server.

### Database Configuration (MariaDB)
1.  Ensure your MariaDB server is running.
2.  Connect to MariaDB as a privileged user (e.g., `root` or an administrator).
3.  Create the database and user for the application. Execute the following SQL commands:
    ```sql
    CREATE DATABASE IF NOT EXISTS narytree_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    CREATE USER IF NOT EXISTS 'your_user'@'localhost' IDENTIFIED BY 'your_password';
    GRANT ALL PRIVILEGES ON narytree_db.* TO 'your_user'@'localhost';
    FLUSH PRIVILEGES;
    ```
    *   Replace `'your_user'` and `'your_password'` with your desired username and password.
    *   You may need to adjust 'localhost' in `CREATE USER ...` and in `DB_URL` (in `DatabaseManager.java`) if your MariaDB server is on a different host.

### Application Configuration
-   The database connection details (URL, username, password) are currently hardcoded in `src/main/java/com/example/narytree/db/DatabaseManager.java`:
    ```java
    private static final String DB_URL = "jdbc:mariadb://localhost:3306/narytree_db";
    private static final String DB_USER = "your_user";
    private static final String DB_PASSWORD = "your_password";
    ```
-   Modify these constants to match the database user and password you configured in MariaDB.
    *   **Note**: For a production application, these should be externalized into a configuration file.
The application also attempts to create the schema if it doesn't exist by executing `src/main/resources/schema.sql` via the `DatabaseManager.initializeSchema()` method during its first startup with a new database.

## Building and Running the Application

1.  **Clone the repository or ensure you have the project files.**
2.  **Update Database Configuration**: Modify `DatabaseManager.java` with your MariaDB credentials as described above.
3.  **Build the project using Maven**:
    Open a terminal or command prompt, navigate to the project's root directory (where `pom.xml` is located), and run:
    ```bash
    mvn clean package
    ```
    This command will compile the code, run tests, and package the application into an executable uber-JAR (e.g., `my-javafx-app-1.0-SNAPSHOT.jar`) in the `target/` directory. This JAR includes all necessary dependencies.

4.  **Run the Application**:
    After a successful build, run the application using the following command. The `maven-shade-plugin` ensures this JAR is self-contained and directly runnable:
    ```bash
    java -jar target/my-javafx-app-1.0-SNAPSHOT.jar
    ```
    (Replace `my-javafx-app-1.0-SNAPSHOT.jar` with the actual name of the generated JAR file if it differs, though the artifactId is `my-javafx-app`).

    The application should launch, and the `DatabaseManager` will attempt to initialize the schema in your configured `narytree_db` database if the tables don't already exist.

## Testing

### Test Database Setup
The JUnit tests in `DatabaseManagerTest.java` run against the same database configured in `DatabaseManager.java`. **It is critical that this is a dedicated test database instance or a database that can be freely modified and cleaned up by the tests.** Do NOT run tests against your production or main development database unless you are prepared for data to be added and deleted.

The `@BeforeAll` setup in `DatabaseManagerTest.java` calls `dbManager.initializeSchema()` to ensure the tables are present. The `@AfterEach` method attempts to clean up specific entities created during tests.

### Running Tests
You can run the tests using Maven:
```bash
mvn test
```
This command will compile the test classes and execute them using the Surefire plugin. Test results will be displayed in the console and usually also in `target/surefire-reports/`.