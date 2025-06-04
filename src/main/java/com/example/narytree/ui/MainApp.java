package com.example.narytree.ui;

import com.example.narytree.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private DatabaseManager dbManager;

    @Override
    public void init() throws Exception {
        super.init();
        dbManager = new DatabaseManager();
        // Initialize schema and potentially add some default data if needed
        // Ensure DB_USER and DB_PASSWORD in DatabaseManager are correctly set up
        // and MariaDB server is running with the 'narytree_db' database created.
        System.out.println("Attempting to initialize DB schema from MainApp...");
        dbManager.initializeSchema();
        // Consider adding some default/test data here for the UI to display initially
        // dbManager.addSampleDataForUI(); // You would create this method in DatabaseManager
        System.out.println("DB schema initialized.");
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/narytree/ui/MainView.fxml"));
            Parent root = loader.load();

            // Get the controller and pass the DatabaseManager
            MainController controller = loader.getController();
            controller.setDatabaseManager(dbManager);
            controller.loadTreeData(); // Initial data load

            Scene scene = new Scene(root, 800, 600);
            primaryStage.setTitle("N-ary Tree Descriptor Editor");
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Failed to load MainView.fxml: " + e.getMessage());
            e.printStackTrace();
            // Show a basic error dialog to the user
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Application Error");
            alert.setHeaderText("Failed to load application UI.");
            alert.setContentText("Could not load MainView.fxml. Please check application resources.");
            alert.showAndWait();
        } catch (Exception e) {
             System.err.println("An unexpected error occurred during application start: " + e.getMessage());
             e.printStackTrace();
             javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
             alert.setTitle("Application Error");
             alert.setHeaderText("An unexpected error occurred.");
             alert.setContentText(e.getMessage());
             alert.showAndWait();
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if (dbManager != null) {
            dbManager.closeConnection();
            System.out.println("Database connection closed.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
