package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import javax.smartcardio.CardException;

/**
 * Controller for Admin Reset Card function
 */
public class AdminResetCardController {

    @FXML private StackPane step1Circle, step2Circle, step3Circle;
    @FXML private VBox step1Content, step2Content, step3Content;
    @FXML private Label cardStatusLabel, adminPinErrorLabel, resetStatusLabel;
    @FXML private PasswordField adminPinField;
    @FXML private CheckBox confirmCheckbox;
    @FXML private Button resetBtn;
    @FXML private ProgressIndicator resetProgress;

    private CardService cardService;
    private int currentStep = 1;

    @FXML
    public void initialize() {
        cardService = new CardService();
        
        // Enable reset button only when checkbox is checked
        confirmCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            resetBtn.setDisable(!newVal);
        });
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
                            "Thẻ này chưa có dữ liệu để reset.");
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
    private void onResetCard() {
        if (!confirmCheckbox.isSelected()) {
            UIUtils.showError("Lỗi", "Vui lòng xác nhận", "Bạn cần check vào ô xác nhận để tiếp tục.");
            return;
        }

        // Final confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Xác nhận reset thẻ");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn reset thẻ này?");
        confirmAlert.setContentText("Hành động này sẽ XÓA TOÀN BỘ dữ liệu và KHÔNG THỂ KHÔI PHỤC!");
        
        ButtonType btnYes = new ButtonType("Có, reset thẻ", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmAlert.getButtonTypes().setAll(btnYes, btnNo);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                performReset();
            }
        });
    }

    private void performReset() {
        resetProgress.setVisible(true);
        resetStatusLabel.setVisible(true);
        resetStatusLabel.setText("Đang reset thẻ...");
        resetBtn.setDisable(true);

        new Thread(() -> {
            try {
                cardService.resetCard();
                
                Platform.runLater(() -> {
                    resetProgress.setVisible(false);
                    resetStatusLabel.setText("✓ Reset thẻ thành công!");
                    resetStatusLabel.setStyle("-fx-text-fill: #22c55e;");
                    
                    UIUtils.showSuccess("Thành công", "Reset thẻ thành công", 
                        "Thẻ đã được reset về trạng thái ban đầu.");
                    
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
                    resetProgress.setVisible(false);
                    resetStatusLabel.setText("Lỗi: " + e.getMessage());
                    resetStatusLabel.setStyle("-fx-text-fill: #ef4444;");
                    UIUtils.showError("Lỗi", "Không thể reset thẻ", e.getMessage());
                    resetBtn.setDisable(false);
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
