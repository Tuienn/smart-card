package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import com.example.desktopapp.util.AppConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Controller for Admin Login Screen
 */
public class AdminLoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    public void initialize() {
        // Clear error label when user types
        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setVisible(false);
        });
        
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            errorLabel.setVisible(false);
        });

        // Press Enter to login
        passwordField.setOnAction(event -> onLogin());
    }

    @FXML
    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu");
            return;
        }

        // Call API to login
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(true);
                    errorLabel.setVisible(false);
                });

                String apiUrl = AppConfig.API_BASE_URL + "/api/admin/login";
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Create JSON body
                JSONObject requestBody = new JSONObject();
                requestBody.put("username", username);
                requestBody.put("password", password);

                // Send request
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Read response
                int responseCode = conn.getResponseCode();
                BufferedReader in;
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                } else {
                    in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());

                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);

                    if (jsonResponse.getBoolean("success")) {
                        // Login successful - navigate to menu
                        AdminApp.changeScene("admin-menu.fxml", "Admin Menu");
                    } else {
                        // Login failed
                        String message = jsonResponse.optString("message", "Đăng nhập thất bại");
                        showError(message);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    showError("Lỗi kết nối server: " + e.getMessage());
                });
                System.err.println("Login error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
