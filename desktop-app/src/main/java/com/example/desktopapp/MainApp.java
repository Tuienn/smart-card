package com.example.desktopapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main Application for Entertainment Card Registration
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
            MainApp.class.getResource("card-registration.fxml")
        );
        
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        
        // Load CSS
        String css = MainApp.class.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(css);
        
        // Configure stage
        stage.setTitle("🎮 Khu Vui Chơi - Đăng Ký Thẻ");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(700);
        
        // Center on screen
        stage.centerOnScreen();
        
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
