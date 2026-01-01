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
 * Controller for Admin Unlock Card function
 */
public class AdminUnlockCardController {

    @FXML private StackPane step1Circle, step2Circle, step3Circle;
    @FXML private VBox step1Content, step2Content, step3Content;
    @FXML private Label cardStatusLabel, adminPinErrorLabel, unlockStatusLabel;
    @FXML private Button checkCardBtn;
    @FXML private PasswordField adminPinField, newPinField;
    @FXML private CheckBox changePinCheckbox;
    @FXML private ProgressIndicator unlockProgress;

    private CardService cardService;
    private int currentStep = 1;

    @FXML
    public void initialize() {
        cardService = new CardService();
        
        // Setup change PIN checkbox listener
        changePinCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            newPinField.setVisible(newVal);
            newPinField.setManaged(newVal);
        });
    }

    @FXML
    private void onCheckCard() {
        cardStatusLabel.setText("Đang kiểm tra thẻ...");
        checkCardBtn.setDisable(true);

        new Thread(() -> {
            try {
                boolean connected = cardService.connect();
                
                if (!connected) {
                    Platform.runLater(() -> {
                        UIUtils.showError("Lỗi", "Không thể kết nối với thẻ", 
                            "Vui lòng đảm bảo thẻ đã được đặt vào đầu đọc và jCIDE simulator đang chạy.");
                        cardStatusLabel.setText("Không tìm thấy thẻ");
                        checkCardBtn.setDisable(false);
                    });
                    return;
                }

                // Check if card is initialized
                boolean initialized = cardService.isCardInitialized();
                
                if (!initialized) {
                    cardService.disconnect();
                    Platform.runLater(() -> {
                        UIUtils.showWarning("Cảnh báo", "Thẻ chưa được khởi tạo", 
                            "Thẻ này chưa được đăng ký. Vui lòng sử dụng MainApp để đăng ký thẻ mới.");
                        cardStatusLabel.setText("Thẻ chưa khởi tạo");
                        checkCardBtn.setDisable(false);
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
                    UIUtils.showError("Lỗi kết nối thẻ", "Không thể kiểm tra thẻ", e.getMessage());
                    cardStatusLabel.setText("Lỗi: " + e.getMessage());
                    checkCardBtn.setDisable(false);
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

        if (adminPin.length() < APDUConstants.MIN_PIN_LENGTH) {
            showAdminPinError("Admin PIN phải có ít nhất " + APDUConstants.MIN_PIN_LENGTH + " ký tự");
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

            } catch (CardException e) {
                Platform.runLater(() -> {
                    String errorMsg = APDUConstants.getErrorMessage(e.getMessage().contains("0x") ? 
                        Integer.parseInt(e.getMessage().substring(e.getMessage().indexOf("0x") + 2), 16) : 0);
                    showAdminPinError("Admin PIN sai: " + errorMsg);
                    adminPinField.setDisable(false);
                    adminPinField.clear();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAdminPinError("Lỗi: " + e.getMessage());
                    adminPinField.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void onUnlockCard() {
        String newPin = changePinCheckbox.isSelected() ? newPinField.getText() : null;
        
        if (changePinCheckbox.isSelected()) {
            if (newPin == null || newPin.isEmpty()) {
                UIUtils.showError("Lỗi", "Vui lòng nhập PIN mới", "");
                return;
            }
            if (newPin.length() < APDUConstants.MIN_PIN_LENGTH || newPin.length() > APDUConstants.MAX_PIN_LENGTH) {
                UIUtils.showError("Lỗi", "PIN không hợp lệ", 
                    "PIN phải có độ dài từ " + APDUConstants.MIN_PIN_LENGTH + " đến " + APDUConstants.MAX_PIN_LENGTH + " ký tự");
                return;
            }
        }

        unlockProgress.setVisible(true);
        unlockStatusLabel.setVisible(true);
        unlockStatusLabel.setText("Đang mở khóa thẻ...");

        new Thread(() -> {
            try {
                cardService.unlockByAdmin(newPin);
                
                Platform.runLater(() -> {
                    unlockProgress.setVisible(false);
                    unlockStatusLabel.setText("✓ Mở khóa thẻ thành công!");
                    unlockStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    
                    UIUtils.showSuccess("Thành công", "Mở khóa thẻ thành công", 
                        changePinCheckbox.isSelected() ? "Thẻ đã được mở khóa và PIN đã được thay đổi." : "Thẻ đã được mở khóa.");
                    
                    // Disconnect and return to menu after 2 seconds
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
                    unlockProgress.setVisible(false);
                    unlockStatusLabel.setText("Lỗi: " + e.getMessage());
                    unlockStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                    UIUtils.showError("Lỗi", "Không thể mở khóa thẻ", e.getMessage());
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
        
        // Update step circles
        updateStepCircle(step1Circle, step >= 1);
        updateStepCircle(step2Circle, step >= 2);
        updateStepCircle(step3Circle, step >= 3);
        
        // Update content visibility
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
