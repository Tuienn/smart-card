package com.example.desktopapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main Application for Entertainment Card Registration
 */
public class MainApp extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("main-menu.fxml"), 1200, 800);
        
        // Load CSS
        String css = MainApp.class.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Configure stage
        stage.setTitle("Khu vui chơi giải trí");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        
        // Center on screen
        stage.centerOnScreen();
        
        stage.show();
    }

    /**
     * Set the root of the scene to a new FXML
     * @param fxml the FXML file name (e.g., "main-menu.fxml")
     */
    public static void setRoot(String fxml) {
        try {
            scene.setRoot(loadFXML(fxml));
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
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource(fxml));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
