package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.UIUtils;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import javax.smartcardio.CardException;

/**
 * Controller for main menu screen
 */
public class MainMenuController {

    @FXML
    private Label statusLabel;

    private CardService cardService = new CardService();

    /**
     * Check card and navigate to appropriate screen
     */
    @FXML
    private void onCheckCard() {
        statusLabel.setText("Đang kiểm tra thẻ...");
        
        // Run card check in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Connect to card
                boolean connected = cardService.connect();
                
                if (!connected) {
                    Platform.runLater(() -> {
                        UIUtils.showError("Lỗi", "Không thể kết nối với thẻ", "Vui lòng đảm bảo thẻ đã được đặt vào đầu đọc và jCIDE simulator đang chạy.");
                        statusLabel.setText("");
                    });
                    return;
                }
                
                // Check if card is initialized
                boolean initialized = cardService.isCardInitialized();
                
                // Disconnect
                cardService.disconnect();
                
                Platform.runLater(() -> {
                    if (initialized) {
                        // Card is initialized, go to card info (PIN will be required)
                        statusLabel.setText("Thẻ đã được khởi tạo. Chuyển đến màn hình xem thông tin...");
                        
                        // Delay to show message before switching screen
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(event -> MainApp.setRoot("card-info.fxml"));
                        delay.play();
                    } else {
                        // Card is not initialized, go to registration
                        statusLabel.setText("Thẻ chưa được khởi tạo. Chuyển đến màn hình đăng ký...");
                        
                        // Delay to show message before switching screen
                        PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                        delay.setOnFinished(event -> MainApp.setRoot("card-registration.fxml"));
                        delay.play();
                    }
                });
                
            } catch (CardException e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi kết nối thẻ", "Không thể kiểm tra thẻ", e.getMessage());
                    statusLabel.setText("");
                });
            }
        }).start();
    }
}
