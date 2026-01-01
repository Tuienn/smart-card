package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import com.example.desktopapp.service.APDUConstants;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import javax.smartcardio.CardException;

/**
 * Controller for Admin Change PIN function
 */
public class AdminChangePinController {

    @FXML private StackPane step1Circle, step2Circle, step3Circle;
    @FXML private VBox step1Content, step2Content, step3Content;
    @FXML private Label cardStatusLabel, adminPinErrorLabel, changeStatusLabel;
    @FXML private PasswordField adminPinField, newPinField, confirmPinField;
    @FXML private ProgressIndicator changeProgress;

    private CardService cardService;
    private int currentStep = 1;

    @FXML
    public void initialize() {
        cardService = new CardService();
    }

    @FXML
    private void onCheckCard() {
        cardStatusLabel.setText("Đang kiểm tra thẻ...");

        new Thread(() -> {
            try {
                boolean connected = cardService.connect();
                
                if (!connected) {
                    Platform.runLater(() -> {
                        UIUtils.showError("Lỗi", "Không thể kết nối với thẻ", 
                            "Vui lòng đảm bảo thẻ đã được đặt vào đầu đọc.");
                        cardStatusLabel.setText("Không tìm thấy thẻ");
                    });
                    return;
                }

                boolean initialized = cardService.isCardInitialized();
                
                if (!initialized) {
                    cardService.disconnect();
                    Platform.runLater(() -> {
                        UIUtils.showWarning("Cảnh báo", "Thẻ chưa được khởi tạo", 
                            "Thẻ này chưa được đăng ký.");
                        cardStatusLabel.setText("Thẻ chưa khởi tạo");
                    });
                    return;
                }

                Platform.runLater(() -> {
                    cardStatusLabel.setText("✓ Thẻ đã sẵn sàng");
                    cardStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    moveToStep(2);
                });

            } catch (CardException e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể kiểm tra thẻ", e.getMessage());
                    cardStatusLabel.setText("Lỗi: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onVerifyAdminPin() {
        String adminPin = adminPinField.getText();
        
        if (adminPin.isEmpty()) {
            showAdminPinError("Vui lòng nhập Admin PIN");
            return;
        }

        adminPinErrorLabel.setVisible(false);
        adminPinField.setDisable(true);

        new Thread(() -> {
            try {
                cardService.verifyAdminPin(adminPin);
                
                Platform.runLater(() -> {
                    moveToStep(3);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAdminPinError("Admin PIN sai: " + e.getMessage());
                    adminPinField.setDisable(false);
                    adminPinField.clear();
                });
            }
        }).start();
    }

    @FXML
    private void onChangePin() {
        String newPin = newPinField.getText();
        String confirmPin = confirmPinField.getText();
        
        if (newPin.isEmpty() || confirmPin.isEmpty()) {
            UIUtils.showError("Lỗi", "Vui lòng nhập đầy đủ thông tin", "");
            return;
        }
        
        if (!newPin.equals(confirmPin)) {
            UIUtils.showError("Lỗi", "PIN không khớp", "PIN mới và xác nhận PIN không giống nhau.");
            return;
        }
        
        if (newPin.length() < APDUConstants.MIN_PIN_LENGTH || newPin.length() > APDUConstants.MAX_PIN_LENGTH) {
            UIUtils.showError("Lỗi", "PIN không hợp lệ", 
                "PIN phải có độ dài từ " + APDUConstants.MIN_PIN_LENGTH + " đến " + APDUConstants.MAX_PIN_LENGTH + " ký tự");
            return;
        }

        changeProgress.setVisible(true);
        changeStatusLabel.setVisible(true);
        changeStatusLabel.setText("Đang đổi mật khẩu...");

        new Thread(() -> {
            try {
                // Unlock with new PIN (change PIN function)
                cardService.unlockByAdmin(newPin);
                
                Platform.runLater(() -> {
                    changeProgress.setVisible(false);
                    changeStatusLabel.setText("✓ Đổi mật khẩu thành công!");
                    changeStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    
                    UIUtils.showSuccess("Thành công", "Đổi mật khẩu thành công", 
                        "PIN của thẻ đã được thay đổi.");
                    
                    // Disconnect and return to menu
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            cardService.disconnect();
                            Platform.runLater(() -> AdminApp.setRoot("admin-menu.fxml"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });

            } catch (CardException e) {
                Platform.runLater(() -> {
                    changeProgress.setVisible(false);
                    changeStatusLabel.setText("Lỗi: " + e.getMessage());
                    changeStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                    UIUtils.showError("Lỗi", "Không thể đổi mật khẩu", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onBack() {
        if (cardService != null && cardService.isConnected()) {
            cardService.disconnect();
        }
        AdminApp.setRoot("admin-menu.fxml");
    }

    @FXML
    private void onBackToHome() {
        onBack();
    }

    private void moveToStep(int step) {
        currentStep = step;
        
        updateStepCircle(step1Circle, step >= 1);
        updateStepCircle(step2Circle, step >= 2);
        updateStepCircle(step3Circle, step >= 3);
        
        step1Content.setVisible(step == 1);
        step1Content.setManaged(step == 1);
        step2Content.setVisible(step == 2);
        step2Content.setManaged(step == 2);
        step3Content.setVisible(step == 3);
        step3Content.setManaged(step == 3);
    }

    private void updateStepCircle(StackPane circle, boolean active) {
        circle.getStyleClass().removeAll("step-circle", "step-circle-active");
        circle.getStyleClass().add(active ? "step-circle-active" : "step-circle");
    }

    private void showAdminPinError(String message) {
        adminPinErrorLabel.setText(message);
        adminPinErrorLabel.setVisible(true);
    }
}
