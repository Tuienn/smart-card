package com.example.desktopapp.controller;

import com.example.desktopapp.ClientApp;
import com.example.desktopapp.util.AppConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controller for Payment Success Screen
 */
public class PaymentSuccessController {

    @FXML
    private Label gameNameLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button playAgainButton;

    @FXML
    private Button exitButton;

    @FXML
    public void initialize() {
        String gameName = AppConfig.getProperty("selectedGameName", "Unknown Game");
        gameNameLabel.setText(gameName);
        messageLabel.setText("Chúc bạn chơi game vui vẻ!");
    }

    /**
     * Return to game selection to play another game
     */
    @FXML
    private void handlePlayAgain() {
        ClientApp.setRoot("game-selection.fxml");
    }

    /**
     * Exit the application
     */
    @FXML
    private void handleExit() {
        System.exit(0);
    }
}
