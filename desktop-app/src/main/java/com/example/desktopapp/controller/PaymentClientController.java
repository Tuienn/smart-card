package com.example.desktopapp.controller;

import com.example.desktopapp.ClientApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.service.APDUConstants;
import com.example.desktopapp.service.PinVerificationException;
import com.example.desktopapp.util.AppConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controller for Payment Screen (Client App)
 * Handles PIN entry and game payment using INS 0x30 (TRY_PLAY_GAME)
 */
public class PaymentClientController {

    @FXML
    private Label gameInfoLabel;

    @FXML
    private ProgressIndicator loadingIndicator;

    @FXML
    private Label statusLabel;

    @FXML
    private PinKeypadController pinKeypadController;

    private CardService cardService;

    @FXML
    public void initialize() {
        cardService = new CardService();
        
        // Display selected game info
        String gameName = AppConfig.getProperty("selectedGameName", "Unknown Game");
        String gamePrice = AppConfig.getProperty("selectedGamePrice", "0");
        gameInfoLabel.setText("Trò chơi: " + gameName + "\nGiá: " + gamePrice + " coins");
        
        loadingIndicator.setVisible(false);
        statusLabel.setText("");
        
        // Auto-submit when PIN is complete (6 digits)
        if (pinKeypadController != null) {
            pinKeypadController.pinProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.length() == 6) {
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(300); // Small delay for visual feedback
                            handlePayment();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
        }
    }

    /**
     * Process payment using INS 0x30 (TRY_PLAY_GAME)
     */
    @FXML
    private void handlePayment() {
        String pin = pinKeypadController.getPin();
        
        if (pin.isEmpty() || pin.length() < 4) {
            showError("Vui lòng nhập PIN (tối thiểu 4 ký tự)");
            return;
        }

        loadingIndicator.setVisible(true);
        statusLabel.setText("Đang xử lý thanh toán...");

        new Thread(() -> {
            try {
                // Connect to card first
                cardService.connect();
                
                // Step 1: Verify PIN using CardService method
                cardService.verifyPin(pin);
                
                Platform.runLater(() -> statusLabel.setText("PIN đã xác thực. Đang thanh toán..."));

                // Step 2: Try to play game using CardService method
                String gameIdStr = AppConfig.getProperty("selectedGameId", "1");
                System.out.println("gameIdStr: " + gameIdStr);
                String gamePriceStr = AppConfig.getProperty("selectedGamePrice", "0");
                
                byte gameId = (byte) Integer.parseInt(gameIdStr);
                int gamePrice = Integer.parseInt(gamePriceStr);
                
                boolean success = cardService.tryPlayGame(gameId, gamePrice);
                
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    
                    if (success) {
                        statusLabel.setText("Thanh toán thành công!");
                        statusLabel.setStyle("-fx-text-fill: green;");
                        
                        // Navigate to success screen
                        ClientApp.setRoot("payment-success.fxml");
                    } else {
                        statusLabel.setText("Lỗi: Phản hồi không hợp lệ");
                        statusLabel.setStyle("-fx-text-fill: red;");
                        showError("Phản hồi từ thẻ không hợp lệ");
                    }
                });
                
            } catch (PinVerificationException e) {
                // Handle PIN verification errors specifically
                String errorMsg = getErrorMessage(e.getStatusWord());
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Lỗi: " + errorMsg);
                    statusLabel.setStyle("-fx-text-fill: red;");
                    showError("Xác thực PIN thất bại: " + errorMsg);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Lỗi: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                    showError("Lỗi thanh toán: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Get error message from status word
     */
    private String getErrorMessage(int sw) {
        switch (sw) {
            case APDUConstants.SW_PIN_VERIFICATION_REQUIRED:
                return "Chưa xác thực PIN";
            case APDUConstants.SW_AUTHENTICATION_BLOCKED:
                return "Thẻ bị khóa";
            case APDUConstants.SW_INSUFFICIENT_FUNDS:
                return "Không đủ coins để chơi game";
            case APDUConstants.SW_WRONG_DATA:
                return "Dữ liệu không hợp lệ";
            case 0x6300:
                return "PIN sai";
            default:
                if ((sw & 0xFF00) == 0x6300) {
                    int remaining = sw & 0x000F;
                    return "PIN sai, còn " + remaining + " lần thử";
                }
                return String.format("Lỗi không xác định (0x%04X)", sw);
        }
    }

    /**
     * Go back to card check
     */
    @FXML
    private void handleBack() {
        ClientApp.setRoot("card-check-client.fxml");
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
