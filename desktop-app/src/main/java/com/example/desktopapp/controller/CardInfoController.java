package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.smartcardio.CardException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Card Info screen
 * Handles card connection, PIN verification, and displaying card data
 */
public class CardInfoController implements Initializable {

    private static final int MAX_PIN_LENGTH = 6;

    // States
    @FXML private VBox connectingState;
    @FXML private VBox pinInputState;
    @FXML private VBox cardInfoState;
    @FXML private VBox errorState;

    // Connecting state
    @FXML private Label connectingLabel;

    // PIN input state
    @FXML private Label pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6;
    @FXML private Label pinInstructionLabel;
    @FXML private Button verifyBtn;
    private Label[] pinDots;
    private StringBuilder pinBuilder = new StringBuilder();

    // Card info state
    @FXML private ImageView avatarImage;
    @FXML private FontIcon avatarPlaceholder;
    @FXML private Label nameLabel;
    @FXML private Label ageLabel;
    @FXML private Label genderLabel;
    @FXML private Label coinsLabel;

    // Error state
    @FXML private Label errorLabel;
    @FXML private Label titleLabel;

    // Service
    private CardService cardService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pinDots = new Label[]{pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6};
        cardService = new CardService();
        
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
                    showState("pin");
                    pinBuilder.setLength(0);
                    updatePinDisplay();
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
        pinInputState.setVisible("pin".equals(state));
        cardInfoState.setVisible("info".equals(state));
        errorState.setVisible("error".equals(state));

        switch (state) {
            case "connecting":
                titleLabel.setText("Xem thông tin thẻ");
                break;
            case "pin":
                titleLabel.setText("Xác thực PIN");
                break;
            case "info":
                titleLabel.setText("Thông tin thẻ");
                break;
            case "error":
                titleLabel.setText("Lỗi");
                break;
        }
    }

    // ============ PIN Keypad Handlers ============

    @FXML
    private void onKeypadPress(ActionEvent event) {
        if (pinBuilder.length() >= MAX_PIN_LENGTH) {
            return;
        }
        Button btn = (Button) event.getSource();
        String digit = (String) btn.getUserData();
        pinBuilder.append(digit);
        updatePinDisplay();
    }

    @FXML
    private void onPinClearAll() {
        pinBuilder.setLength(0);
        updatePinDisplay();
    }

    @FXML
    private void onPinBackspace() {
        if (pinBuilder.length() > 0) {
            pinBuilder.deleteCharAt(pinBuilder.length() - 1);
            updatePinDisplay();
        }
    }

    private void updatePinDisplay() {
        int length = pinBuilder.length();
        for (int i = 0; i < pinDots.length; i++) {
            pinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            if (i < length) {
                pinDots[i].getStyleClass().add("pin-dot-filled");
            } else {
                pinDots[i].getStyleClass().add("pin-dot-empty");
            }
        }

        // Update instruction and button state
        if (length == 0) {
            pinInstructionLabel.setText("Nhập mã PIN 6 số");
            pinInstructionLabel.setGraphic(null);
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            verifyBtn.setDisable(true);
        } else if (length < MAX_PIN_LENGTH) {
            pinInstructionLabel.setText("Còn " + (MAX_PIN_LENGTH - length) + " số nữa");
            pinInstructionLabel.setGraphic(null);
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            verifyBtn.setDisable(true);
        } else {
            pinInstructionLabel.setText("Hoàn tất");
            pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.CHECK, "#22c55e", 14));
            if (!pinInstructionLabel.getStyleClass().contains("pin-instruction-complete")) {
                pinInstructionLabel.getStyleClass().add("pin-instruction-complete");
            }
            verifyBtn.setDisable(false);
        }
    }

    /**
     * Verify PIN and read card data
     */
    @FXML
    private void onVerifyPin() {
        if (pinBuilder.length() != MAX_PIN_LENGTH) {
            UIUtils.showAlert("Lỗi", "Vui lòng nhập đủ 6 số PIN");
            return;
        }

        String pin = pinBuilder.toString();
        verifyBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang xác thực...");

        Task<Void> verifyTask = new Task<>() {
            private String name;
            private byte age;
            private byte gender;
            private int coins;
            private byte[] avatar;

            @Override
            protected Void call() throws Exception {
                // Verify PIN
                updateMessage("Đang xác thực PIN...");
                cardService.verifyPin(pin);

                // Read card data
                updateMessage("Đang đọc dữ liệu thẻ...");
                name = cardService.readName();
                age = cardService.readAge();
                gender = cardService.readGender();
                coins = cardService.readCoins();

                // Read avatar
                updateMessage("Đang đọc ảnh đại diện...");
                try {
                    avatar = cardService.readAvatar();
                } catch (CardException e) {
                    // Avatar is optional
                    avatar = null;
                }

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Display card info
                    displayCardInfo(name, age, gender, coins, avatar);
                    showState("info");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showState("pin");
                    pinBuilder.setLength(0);
                    updatePinDisplay();
                    
                    Throwable ex = getException();
                    String message = ex != null ? ex.getMessage() : "Xác thực thất bại";
                    pinInstructionLabel.setText(message);
                    pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
                    pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.TIMES, "#ef4444", 14));
                });
            }
        };

        connectingLabel.textProperty().bind(verifyTask.messageProperty());
        new Thread(verifyTask).start();
    }

    /**
     * Display card info on screen
     */
    private void displayCardInfo(String name, byte age, byte gender, int coins, byte[] avatar) {
        // Name
        nameLabel.setText(name != null && !name.isEmpty() ? name : "Chưa có tên");

        // Age
        ageLabel.setText(age > 0 ? String.valueOf(age & 0xFF) : "Chưa xác định");

        // Gender
        String genderText;
        switch (gender) {
            case 1:
                genderText = "Nam";
                break;
            case 2:
                genderText = "Nữ";
                break;
            default:
                genderText = "Chưa xác định";
        }
        genderLabel.setText(genderText);

        // Coins
        coinsLabel.setText(String.valueOf(coins));

        // Avatar
        if (avatar != null && avatar.length > 0) {
            try {
                Image image = new Image(new ByteArrayInputStream(avatar));
                avatarImage.setImage(image);
                avatarPlaceholder.setVisible(false);
            } catch (Exception e) {
                avatarPlaceholder.setVisible(true);
            }
        } else {
            avatarPlaceholder.setVisible(true);
        }
    }

    /**
     * Retry connection
     */
    @FXML
    private void onRetry() {
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
