package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.service.PinVerificationException;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.smartcardio.CardException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Unlock Card screen
 * Handles Admin PIN verification and card unlocking with new user PIN
 */
public class UnlockCardController implements Initializable {

    private static final int ADMIN_PIN_LENGTH = 16;
    private static final int USER_PIN_LENGTH = 6;

    // States
    @FXML private VBox connectingState;
    @FXML private VBox pinInputState;
    @FXML private VBox newPinInputState;
    @FXML private VBox successState;
    @FXML private VBox errorState;

    // Connecting state
    @FXML private Label connectingLabel;
    @FXML private Label titleLabel;

    // Admin PIN input state - 16 dots
    @FXML private Label pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6, pinDot7, pinDot8;
    @FXML private Label pinDot9, pinDot10, pinDot11, pinDot12, pinDot13, pinDot14, pinDot15, pinDot16;
    @FXML private Label pinInstructionLabel;
    @FXML private Button unlockBtn;
    private Label[] adminPinDots;
    private StringBuilder adminPinBuilder = new StringBuilder();

    // New User PIN - included component
    @FXML private PinKeypadController newPinKeypadController; // Controller from fx:include
    @FXML private Button confirmNewPinBtn;

    // Error state
    @FXML private Label errorLabel;

    // Service
    private CardService cardService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        adminPinDots = new Label[]{
            pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6, pinDot7, pinDot8,
            pinDot9, pinDot10, pinDot11, pinDot12, pinDot13, pinDot14, pinDot15, pinDot16
        };
        cardService = new CardService();
        
        // Listen to new PIN keypad changes to enable/disable confirm button
        // Use Platform.runLater to ensure the nested controller from fx:include is available
        Platform.runLater(() -> {
            if (newPinKeypadController != null) {
                newPinKeypadController.pinProperty().addListener((obs, oldVal, newVal) -> {
                    confirmNewPinBtn.setDisable(!newPinKeypadController.isPinComplete());
                });
            }
        });
        
        // Auto-connect to card on initialize
        connectToCard();
    }

    /**
     * Connect to card in background
     */
    private void connectToCard() {
        showState("connecting");
        connectingLabel.setText("Đang kết nối thẻ...");

        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                cardService.connect();
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    showState("adminPin");
                    adminPinBuilder.setLength(0);
                    updateAdminPinDisplay();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showState("error");
                    Throwable ex = getException();
                    errorLabel.setText(ex != null ? ex.getMessage() : "Không thể kết nối thẻ");
                });
            }
        };

        new Thread(connectTask).start();
    }

    /**
     * Show specific state
     */
    private void showState(String state) {
        connectingState.setVisible("connecting".equals(state));
        pinInputState.setVisible("adminPin".equals(state));
        newPinInputState.setVisible("newPin".equals(state));
        successState.setVisible("success".equals(state));
        errorState.setVisible("error".equals(state));

        switch (state) {
            case "connecting":
                titleLabel.setText("Mở khóa thẻ");
                break;
            case "adminPin":
                titleLabel.setText("Xác thực Admin PIN");
                break;
            case "newPin":
                titleLabel.setText("Đặt mã PIN mới");
                break;
            case "success":
                titleLabel.setText("Thành công");
                break;
            case "error":
                titleLabel.setText("Lỗi");
                break;
        }
    }

    // ============ Admin PIN Keypad Handlers ============

    @FXML
    private void onKeypadPress(ActionEvent event) {
        if (adminPinBuilder.length() >= ADMIN_PIN_LENGTH) {
            return;
        }
        Button btn = (Button) event.getSource();
        String digit = (String) btn.getUserData();
        adminPinBuilder.append(digit);
        updateAdminPinDisplay();
    }

    @FXML
    private void onPinClearAll() {
        adminPinBuilder.setLength(0);
        updateAdminPinDisplay();
    }

    @FXML
    private void onPinBackspace() {
        if (adminPinBuilder.length() > 0) {
            adminPinBuilder.deleteCharAt(adminPinBuilder.length() - 1);
            updateAdminPinDisplay();
        }
    }

    private void updateAdminPinDisplay() {
        int length = adminPinBuilder.length();
        for (int i = 0; i < adminPinDots.length; i++) {
            adminPinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            if (i < length) {
                adminPinDots[i].getStyleClass().add("pin-dot-filled");
            } else {
                adminPinDots[i].getStyleClass().add("pin-dot-empty");
            }
        }

        // Update instruction and button state
        if (length == 0) {
            pinInstructionLabel.setText("Nhập mã Admin PIN 16 số");
            pinInstructionLabel.setGraphic(null);
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            unlockBtn.setDisable(true);
        } else if (length < ADMIN_PIN_LENGTH) {
            pinInstructionLabel.setText("Còn " + (ADMIN_PIN_LENGTH - length) + " số nữa");
            pinInstructionLabel.setGraphic(null);
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            unlockBtn.setDisable(true);
        } else {
            pinInstructionLabel.setText("Hoàn tất");
            pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.CHECK, "#22c55e", 14));
            if (!pinInstructionLabel.getStyleClass().contains("pin-instruction-complete")) {
                pinInstructionLabel.getStyleClass().add("pin-instruction-complete");
            }
            unlockBtn.setDisable(false);
            
            // Auto-unlock when Admin PIN is complete
            Platform.runLater(() -> onUnlock());
        }
    }

    /**
     * Verify Admin PIN and go to new PIN input
     */
    @FXML
    private void onUnlock() {
        if (adminPinBuilder.length() != ADMIN_PIN_LENGTH) {
            UIUtils.showAlert("Lỗi", "Vui lòng nhập đủ 16 số Admin PIN");
            return;
        }

        String adminPin = adminPinBuilder.toString();
        unlockBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang xác thực Admin PIN...");

        Task<Void> verifyTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Verify Admin PIN only
                updateMessage("Đang xác thực Admin PIN...");
                cardService.verifyAdminPin(adminPin);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Unbind before updating UI
                    connectingLabel.textProperty().unbind();
                    
                    // Go to new PIN input state
                    showState("newPin");
                    if (newPinKeypadController != null) {
                        newPinKeypadController.reset();
                    }
                    confirmNewPinBtn.setDisable(true);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // Unbind before updating UI
                    connectingLabel.textProperty().unbind();
                    
                    Throwable ex = getException();
                    
                    // Check if this is a PIN verification error
                    if (ex instanceof PinVerificationException) {
                        PinVerificationException pinEx = (PinVerificationException) ex;
                        
                        if (pinEx.isCardBlocked()) {
                            // Admin is blocked
                            showState("error");
                            errorLabel.setText("Admin bị khóa do nhập sai PIN quá 3 lần.");
                        } else {
                            // Wrong Admin PIN - show remaining attempts
                            showState("adminPin");
                            adminPinBuilder.setLength(0);
                            updateAdminPinDisplay();
                            
                            String message;
                            if (pinEx.isWrongPin()) {
                                int remaining = pinEx.getRemainingAttempts();
                                message = "Admin PIN sai! Còn " + remaining + " lần thử";
                            } else {
                                message = pinEx.getMessage();
                            }
                            pinInstructionLabel.setText(message);
                            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
                            pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#f59e0b", 14));
                        }
                    } else {
                        // Other errors
                        showState("error");
                        String message = ex != null ? ex.getMessage() : "Xác thực thất bại";
                        errorLabel.setText(message);
                    }
                });
            }
        };

        connectingLabel.textProperty().bind(verifyTask.messageProperty());
        new Thread(verifyTask).start();
    }

    /**
     * Confirm new PIN and unlock card
     */
    @FXML
    private void onConfirmNewPin() {
        if (newPinKeypadController == null || !newPinKeypadController.isPinComplete()) {
            UIUtils.showAlert("Lỗi", "Vui lòng nhập đủ 6 số PIN mới");
            return;
        }

        String newPin = newPinKeypadController.getPin();
        confirmNewPinBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang mở khóa thẻ...");

        Task<Void> unlockTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Unlock card with new PIN
                updateMessage("Đang mở khóa thẻ với mã PIN mới...");
                cardService.unlockByAdmin(newPin);
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Unbind before updating UI
                    connectingLabel.textProperty().unbind();
                    showState("success");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // Unbind before updating UI
                    connectingLabel.textProperty().unbind();
                    
                    Throwable ex = getException();
                    showState("error");
                    String message = ex != null ? ex.getMessage() : "Mở khóa thất bại";
                    errorLabel.setText(message);
                });
            }
        };

        connectingLabel.textProperty().bind(unlockTask.messageProperty());
        new Thread(unlockTask).start();
    }

    /**
     * Retry connection
     */
    @FXML
    private void onRetry() {
        adminPinBuilder.setLength(0);
        if (newPinKeypadController != null) {
            newPinKeypadController.reset();
        }
        connectToCard();
    }

    /**
     * Go back to main menu
     */
    @FXML
    private void onGoHome() {
        if (cardService != null) {
            cardService.disconnect();
        }
        MainApp.setRoot("main-menu.fxml");
    }
}
