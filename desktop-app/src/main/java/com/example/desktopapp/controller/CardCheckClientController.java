package com.example.desktopapp.controller;

import com.example.desktopapp.ClientApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.AppConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for Card Check Screen (Client App)
 */
public class CardCheckClientController {

    @FXML
    private Label instructionLabel;

    @FXML
    private Label gameInfoLabel;

    @FXML
    private Button checkCardButton;

    @FXML
    private Label statusLabel;

    private CardService cardService;

    @FXML
    public void initialize() {
        cardService = new CardService();
        
        // Display selected game info
        String gameName = AppConfig.getProperty("selectedGameName", "Unknown Game");
        String gamePrice = AppConfig.getProperty("selectedGamePrice", "0");
        gameInfoLabel.setText("Trò chơi: " + gameName + " - Giá: " + gamePrice + " coins");
    
        statusLabel.setText("");
    }

    /**
     * Check if card is present and connected
     */
    @FXML
    private void handleCheckCard() {
        checkCardButton.setDisable(true);
        statusLabel.setManaged(true);
        statusLabel.setText("Đang kiểm tra thẻ...");
        statusLabel.setStyle("-fx-text-fill: #94a3b8;");

        new Thread(() -> {
            try {
                cardService.connect();
                boolean cardFound = true;
                
                Platform.runLater(() -> {
                    checkCardButton.setDisable(false);
                    
                    if (cardFound) {
                        statusLabel.setText("Thẻ đã được phát hiện!");
                        statusLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-size: 14px; -fx-font-weight: bold;");
                        
                        // Delay để người dùng nhìn thấy thông báo
                        new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                                Platform.runLater(() -> ClientApp.setRoot("payment-client.fxml"));
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    } else {
                        statusLabel.setText("Không tìm thấy thẻ!");
                        statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    checkCardButton.setDisable(false);
                    statusLabel.setManaged(true);
                    statusLabel.setText("Lỗi: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
                });
            }
        }).start();
    }

    /**
     * Go back to game selection
     */
    @FXML
    private void handleBack() {
        ClientApp.setRoot("game-selection.fxml");
    }

    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
