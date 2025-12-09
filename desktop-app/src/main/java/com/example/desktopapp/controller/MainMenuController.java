package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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
     * Navigate to View Card Info screen (placeholder for now)
     */
    @FXML
    private void onViewCardInfo() {
        // TODO: Replace with actual navigation when card-info.fxml is created
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText("Chức năng đang phát triển");
        alert.setContentText("Tính năng xem thông tin thẻ sẽ được cập nhật trong phiên bản tiếp theo.");
        alert.showAndWait();
    }
}
