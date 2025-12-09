package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import javafx.fxml.FXML;

/**
 * Controller for main menu screen
 */
public class MainMenuController {

    /**
     * Navigate to Card Registration screen
     */
    @FXML
    private void onRegisterCard() {
        MainApp.setRoot("card-registration.fxml");
    }

    /**
     * Navigate to View Card Info screen
     */
    @FXML
    private void onViewCardInfo() {
        MainApp.setRoot("card-info.fxml");
    }
}
