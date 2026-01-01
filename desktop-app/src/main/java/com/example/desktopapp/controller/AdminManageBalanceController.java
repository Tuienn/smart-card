package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import javafx.fxml.FXML;

/**
 * Controller for Admin Manage Balance (placeholder)
 */
public class AdminManageBalanceController {

    @FXML
    private void onBack() {
        AdminApp.setRoot("admin-menu.fxml");
    }

    @FXML
    private void onBackToHome() {
        onBack();
    }
}
