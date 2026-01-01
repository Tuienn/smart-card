package com.example.desktopapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Admin Application for Card Management
 * Allows admin to manage cards, reset cards, change PINs, and view transaction history
 */
public class AdminApp extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("admin-menu.fxml"), 1200, 800);
        
        // Load CSS
        String css = AdminApp.class.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Configure stage
        stage.setTitle("Hệ thống quản trị thẻ - Admin Panel");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        
        // Center on screen
        stage.centerOnScreen();
        
        stage.show();
    }

    /**
     * Set the root of the scene to a new FXML
     * @param fxml the FXML file name (e.g., "admin-menu.fxml")
     */
    public static void setRoot(String fxml) {
        try {
            scene.setRoot(loadFXML(fxml));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Set the root of the scene to a new FXML with controller callback
     * @param fxml the FXML file name
     * @param controllerCallback callback to configure controller after loading
     */
    public static <T> void setRoot(String fxml, java.util.function.Consumer<T> controllerCallback) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(AdminApp.class.getResource(fxml));
            Parent root = fxmlLoader.load();
            T controller = fxmlLoader.getController();
            if (controllerCallback != null && controller != null) {
                controllerCallback.accept(controller);
            }
            scene.setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an FXML file and return the root Parent
     * @param fxml the FXML file name
     * @return the loaded Parent node
     * @throws IOException if loading fails
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(AdminApp.class.getResource(fxml));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
