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
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.smartcardio.CardException;
import com.example.desktopapp.service.PinVerificationException;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for Card Info screen
 * Handles card connection, PIN verification, and displaying card data
 */
public class CardInfoController implements Initializable {

    private static final int MAX_PIN_LENGTH = 6;
    private static final int MAX_ADMIN_PIN_LENGTH = 16;

    // States
    @FXML private VBox connectingState;
    @FXML private VBox pinInputState;
    @FXML private VBox cardInfoState;
    @FXML private VBox resetCardState;
    @FXML private VBox changePinState;
    @FXML private VBox editInfoState;
    @FXML private VBox errorState;

    // Connecting state
    @FXML private Label connectingLabel;

    // PIN input state
    @FXML private Label pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6;
    @FXML private Label pinInstructionLabel;
    @FXML private Button verifyBtn;
    @FXML private Button resetCardBtn;
    private Label[] pinDots;
    private StringBuilder pinBuilder = new StringBuilder();

    // Card info state
    @FXML private ImageView avatarImage;
    @FXML private FontIcon avatarPlaceholder;
    @FXML private Label nameLabel;
    @FXML private Label ageLabel;
    @FXML private Label genderLabel;
    @FXML private Label coinsLabel;
    @FXML private FlowPane purchasedGamesContainer;
    @FXML private Label noGamesLabel;
    @FXML private HBox actionButtonsBox;

    // Error state
    @FXML private Label errorLabel;
    @FXML private Label titleLabel;
    @FXML private Button unlockCardBtn;
    @FXML private VBox unlockWarningBox;

    // Reset card state
    @FXML private Label resetPinDot1, resetPinDot2, resetPinDot3, resetPinDot4;
    @FXML private Label resetPinDot5, resetPinDot6, resetPinDot7, resetPinDot8;
    @FXML private Label resetPinDot9, resetPinDot10, resetPinDot11, resetPinDot12;
    @FXML private Label resetPinDot13, resetPinDot14, resetPinDot15, resetPinDot16;
    @FXML private Label resetPinInstructionLabel;
    @FXML private Button confirmResetBtn;
    private Label[] resetPinDots;
    private StringBuilder resetPinBuilder = new StringBuilder();
    
    // Change PIN state
    @FXML private Label oldPinDot1, oldPinDot2, oldPinDot3, oldPinDot4, oldPinDot5, oldPinDot6;
    @FXML private Label newPinDot1, newPinDot2, newPinDot3, newPinDot4, newPinDot5, newPinDot6;
    @FXML private Label confirmPinDot1, confirmPinDot2, confirmPinDot3, confirmPinDot4, confirmPinDot5, confirmPinDot6;
    @FXML private Label changePinInstructionLabel;
    @FXML private Button confirmChangePinBtn;
    private Label[] oldPinDots;
    private Label[] newPinDots;
    private Label[] confirmPinDots;
    private StringBuilder oldPinBuilder = new StringBuilder();
    private StringBuilder newPinBuilder = new StringBuilder();
    private StringBuilder confirmPinBuilder = new StringBuilder();
    private int changePinStep = 0; // 0: old PIN, 1: new PIN, 2: confirm PIN
    
    // Edit info state
    @FXML private TextField editNameField;
    @FXML private TextField editAgeField;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private RadioButton otherRadio;
    @FXML private ToggleGroup genderGroup;
    @FXML private Label editInfoInstructionLabel;
    @FXML private Button saveInfoBtn;

    // Service
    private CardService cardService;
    private String verifiedPin; // Store PIN after successful verification

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pinDots = new Label[]{pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6};
        resetPinDots = new Label[]{
            resetPinDot1, resetPinDot2, resetPinDot3, resetPinDot4,
            resetPinDot5, resetPinDot6, resetPinDot7, resetPinDot8,
            resetPinDot9, resetPinDot10, resetPinDot11, resetPinDot12,
            resetPinDot13, resetPinDot14, resetPinDot15, resetPinDot16
        };
        oldPinDots = new Label[]{oldPinDot1, oldPinDot2, oldPinDot3, oldPinDot4, oldPinDot5, oldPinDot6};
        newPinDots = new Label[]{newPinDot1, newPinDot2, newPinDot3, newPinDot4, newPinDot5, newPinDot6};
        confirmPinDots = new Label[]{confirmPinDot1, confirmPinDot2, confirmPinDot3, confirmPinDot4, confirmPinDot5, confirmPinDot6};
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
        resetCardState.setVisible("reset".equals(state));
        changePinState.setVisible("changepin".equals(state));
        editInfoState.setVisible("editinfo".equals(state));
        errorState.setVisible("error".equals(state));
        
        // Show/hide reset button based on state
        if (resetCardBtn != null) {
            resetCardBtn.setVisible("pin".equals(state));
        }

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
            case "reset":
                titleLabel.setText("Reset thẻ");
                break;
            case "changepin":
                titleLabel.setText("Đổi mã PIN");
                break;
            case "editinfo":
                titleLabel.setText("Chỉnh sửa thông tin");
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
            
            // Auto-verify when PIN is complete
            Platform.runLater(() -> onVerifyPin());
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
            private short[] gameIds;

            @Override
            protected Void call() throws Exception {
                // Verify PIN
                updateMessage("Đang xác thực PIN...");
                cardService.verifyPin(pin);
                verifiedPin = pin; // Save PIN for later use

                // Read card data
                updateMessage("Đang đọc dữ liệu thẻ...");
                name = cardService.readName();
                age = cardService.readAge();
                gender = cardService.readGender();
                coins = cardService.readCoins();

                // Read purchased games
                updateMessage("Đang đọc game đã mua...");
                try {
                    gameIds = cardService.readPurchasedGames();
                } catch (CardException e) {
                    gameIds = new short[0];
                }

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
                    // Unbind before updating UI
                    connectingLabel.textProperty().unbind();
                    
                    // Display card info
                    displayCardInfo(name, age, gender, coins, avatar, gameIds);
                    showState("info");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // Unbind before updating UI - critical for subsequent attempts
                    connectingLabel.textProperty().unbind();
                    
                    Throwable ex = getException();
                    
                    // Check if this is a PIN verification error
                    if (ex instanceof PinVerificationException) {
                        PinVerificationException pinEx = (PinVerificationException) ex;
                        
                        if (pinEx.isCardBlocked()) {
                            // Card is blocked (0x6983) - show error state with unlock option
                            showState("error");
                            errorLabel.setText("Thẻ đã bị khóa do nhập sai PIN quá 3 lần.\nBạn cần liên hệ ban tổ chức để tiến hành mở khóa.");
                            unlockCardBtn.setVisible(true);
                            unlockWarningBox.setVisible(true);
                        } else {
                            // Wrong PIN but card not blocked - show remaining attempts
                            showState("pin");
                            pinBuilder.setLength(0);
                            updatePinDisplay();
                            
                            String message;
                            if (pinEx.isWrongPin()) {
                                int remaining = pinEx.getRemainingAttempts();
                                message = "PIN sai! Còn " + remaining + " lần thử";
                            } else {
                                message = pinEx.getMessage();
                            }
                            pinInstructionLabel.setText(message);
                            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
                            pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.EXCLAMATION_TRIANGLE, "#f59e0b", 14));
                        }
                    } else {
                        // Other errors
                        showState("pin");
                        pinBuilder.setLength(0);
                        updatePinDisplay();
                        
                        String message = ex != null ? ex.getMessage() : "Xác thực thất bại";
                        pinInstructionLabel.setText(message);
                        pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
                        pinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.TIMES, "#ef4444", 14));
                    }
                });
            }
        };

        connectingLabel.textProperty().bind(verifyTask.messageProperty());
        new Thread(verifyTask).start();
    }

    /**
     * Display card info on screen
     */
    private void displayCardInfo(String name, byte age, byte gender, int coins, byte[] avatar, short[] gameIds) {
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
        
        // Display purchased games
        displayPurchasedGames(gameIds);
    }
    
    /**
     * Display purchased games list
     */
    private void displayPurchasedGames(short[] gameIds) {
        if (purchasedGamesContainer == null) {
            return;
        }
        
        purchasedGamesContainer.getChildren().clear();
        
        if (gameIds == null || gameIds.length == 0) {
            if (noGamesLabel != null) {
                noGamesLabel.setVisible(true);
                noGamesLabel.setManaged(true);
            }
            return;
        }
        
        if (noGamesLabel != null) {
            noGamesLabel.setVisible(false);
            noGamesLabel.setManaged(false);
        }
        
        // Count occurrences of each game ID
        java.util.Map<Short, Integer> gameCountMap = new java.util.HashMap<>();
        for (short gameId : gameIds) {
            gameCountMap.put(gameId, gameCountMap.getOrDefault(gameId, 0) + 1);
        }
        
        // Load game names from backend and display with count
        Task<Void> loadGamesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (java.util.Map.Entry<Short, Integer> entry : gameCountMap.entrySet()) {
                    short gameId = entry.getKey();
                    int count = entry.getValue();
                    
                    try {
                        String gameName = fetchGameName(gameId);
                        Platform.runLater(() -> {
                            Label gameLabel = createGameLabel(gameId, gameName, count);
                            purchasedGamesContainer.getChildren().add(gameLabel);
                        });
                    } catch (Exception e) {
                        // Skip games that can't be loaded
                        System.err.println("Error loading game " + gameId + ": " + e.getMessage());
                    }
                }
                return null;
            }
        };
        
        new Thread(loadGamesTask).start();
    }
    
    /**
     * Fetch game name from backend API
     */
    private String fetchGameName(short gameId) throws Exception {
        java.net.URL url = new java.net.URL(com.example.desktopapp.util.AppConfig.API_GAMES + "/" + gameId);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON to get name
            String json = response.toString();
            int nameStart = json.indexOf("\"name\":\"") + 8;
            int nameEnd = json.indexOf("\"", nameStart);
            return json.substring(nameStart, nameEnd);
        } else {
            return "Game #" + gameId;
        }
    }
    
    /**
     * Create game label UI component
     */
    private Label createGameLabel(short gameId, String gameName, int count) {
        String displayText = count > 1 ? gameName + " (x" + count + ")" : gameName;
        Label label = new Label(displayText);
        label.setStyle("-fx-padding: 5 10; -fx-background-color: rgba(59, 130, 246, 0.1); " +
                      "-fx-text-fill: #3b82f6; -fx-font-size: 12px; -fx-background-radius: 4;");
        label.setGraphic(UIUtils.createIcon(FontAwesomeSolid.GAMEPAD, "#3b82f6", 12));
        return label;
    }
    
    /**
     * Buy more coins
     */
    @FXML
    private void onBuyCoins() {
        if (verifiedPin == null) {
            UIUtils.showAlert("Lỗi", "Vui lòng xác thực lại thẻ");
            return;
        }
        
        // Disconnect current connection
        if (cardService != null) {
            cardService.disconnect();
        }
        
        // Navigate to payment screen
        MainApp.setRoot("payment-topup.fxml", (PaymentTopupController controller) -> {
            controller.setPin(verifiedPin);
        });
    }
    
    /**
     * Buy combo
     */
    @FXML
    private void onBuyCombo() {
        if (verifiedPin == null) {
            UIUtils.showAlert("Lỗi", "Vui lòng xác thực lại thẻ");
            return;
        }
        
        // Disconnect current connection
        if (cardService != null) {
            cardService.disconnect();
        }
        
        // Navigate to payment screen with combo preselected
        MainApp.setRoot("payment-topup.fxml", (PaymentTopupController controller) -> {
            controller.setPin(verifiedPin);
            controller.preselectComboMode();
        });
    }

    /**
     * Retry connection
     */
    @FXML
    private void onRetry() {
        // Hide unlock button and warning when retrying
        if (unlockCardBtn != null) {
            unlockCardBtn.setVisible(false);
        }
        if (unlockWarningBox != null) {
            unlockWarningBox.setVisible(false);
        }
        connectToCard();
    }

    /**
     * Navigate to unlock card screen
     */
    @FXML
    private void onUnlockCard() {
        if (cardService != null) {
            cardService.disconnect();
        }
        MainApp.setRoot("unlock-card.fxml");
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

    // ============ Reset Card Handlers ============

    /**
     * Handle reset card request
     */
    @FXML
    private void onResetCardRequest() {
        resetPinBuilder.setLength(0);
        updateResetPinDisplay();
        showState("reset");
    }

    /**
     * Cancel reset operation
     */
    @FXML
    private void onCancelReset() {
        resetPinBuilder.setLength(0);
        showState("pin");
    }

    /**
     * Handle reset keypad press
     */
    @FXML
    private void onResetKeypadPress(ActionEvent event) {
        if (resetPinBuilder.length() >= MAX_ADMIN_PIN_LENGTH) {
            return;
        }
        Button btn = (Button) event.getSource();
        String digit = (String) btn.getUserData();
        resetPinBuilder.append(digit);
        updateResetPinDisplay();
    }

    /**
     * Clear all reset PIN digits
     */
    @FXML
    private void onResetPinClearAll() {
        resetPinBuilder.setLength(0);
        updateResetPinDisplay();
    }

    /**
     * Backspace reset PIN
     */
    @FXML
    private void onResetPinBackspace() {
        if (resetPinBuilder.length() > 0) {
            resetPinBuilder.deleteCharAt(resetPinBuilder.length() - 1);
            updateResetPinDisplay();
        }
    }

    /**
     * Update reset PIN display
     */
    private void updateResetPinDisplay() {
        int length = resetPinBuilder.length();
        for (int i = 0; i < resetPinDots.length; i++) {
            resetPinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            if (i < length) {
                resetPinDots[i].getStyleClass().add("pin-dot-filled");
            } else {
                resetPinDots[i].getStyleClass().add("pin-dot-empty");
            }
        }

        // Update instruction and button state
        if (length == 0) {
            resetPinInstructionLabel.setText("Nhập mã Admin PIN 16 số");
            resetPinInstructionLabel.setGraphic(null);
            resetPinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            confirmResetBtn.setDisable(true);
        } else if (length < MAX_ADMIN_PIN_LENGTH) {
            resetPinInstructionLabel.setText("Còn " + (MAX_ADMIN_PIN_LENGTH - length) + " số nữa");
            resetPinInstructionLabel.setGraphic(null);
            resetPinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
            confirmResetBtn.setDisable(true);
        } else {
            resetPinInstructionLabel.setText("Hoàn tất");
            resetPinInstructionLabel.setGraphic(UIUtils.createIcon(FontAwesomeSolid.CHECK, "#22c55e", 14));
            if (!resetPinInstructionLabel.getStyleClass().contains("pin-instruction-complete")) {
                resetPinInstructionLabel.getStyleClass().add("pin-instruction-complete");
            }
            confirmResetBtn.setDisable(false);
        }
    }

    /**
     * Confirm reset card with admin PIN
     */
    @FXML
    private void onConfirmReset() {
        if (resetPinBuilder.length() != MAX_ADMIN_PIN_LENGTH) {
            UIUtils.showAlert("Lỗi", "Vui lòng nhập đủ 16 số Admin PIN");
            return;
        }

        String adminPin = resetPinBuilder.toString();
        confirmResetBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang xác thực Admin PIN...");

        Task<Void> resetTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Step 1: Verify Admin PIN
                updateMessage("Đang xác thực Admin PIN...");
                cardService.verifyAdminPin(adminPin);

                // Step 2: Reset card
                updateMessage("Đang reset thẻ...");
                cardService.resetCard();

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Thành công", 
                        "Reset thẻ thành công!\n\nThẻ đã được đặt lại về trạng thái ban đầu.\n" +
                        "Người dùng cần khởi tạo lại thẻ để sử dụng.");
                    
                    // Disconnect and return to main menu
                    if (cardService != null) {
                        cardService.disconnect();
                    }
                    MainApp.setRoot("main-menu.fxml");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable ex = getException();
                    String errorMsg;
                    
                    if (ex instanceof PinVerificationException) {
                        PinVerificationException pinEx = (PinVerificationException) ex;
                        int remaining = pinEx.getRemainingAttempts();
                        
                        if (remaining == 0) {
                            errorMsg = "Admin PIN bị khóa!\n\nĐã nhập sai quá 3 lần.\nVui lòng liên hệ quản trị viên.";
                        } else {
                            errorMsg = "Admin PIN không đúng!\n\nCòn " + remaining + " lần thử.";
                        }
                    } else if (ex instanceof CardException) {
                        errorMsg = "Lỗi kết nối thẻ: " + ex.getMessage();
                    } else {
                        errorMsg = "Lỗi: " + (ex != null ? ex.getMessage() : "Không xác định");
                    }
                    
                    errorLabel.setText(errorMsg);
                    showState("error");
                    
                    // Reset PIN builder
                    resetPinBuilder.setLength(0);
                    confirmResetBtn.setDisable(false);
                });
            }
        };

        connectingLabel.textProperty().bind(resetTask.messageProperty());
        new Thread(resetTask).start();
    }

    // ============ Change PIN Handlers ============

    /**
     * Handle change PIN request
     */
    @FXML
    private void onChangePin() {
        changePinStep = 0;
        oldPinBuilder.setLength(0);
        newPinBuilder.setLength(0);
        confirmPinBuilder.setLength(0);
        updateChangePinDisplay();
        showState("changepin");
    }

    /**
     * Cancel change PIN operation
     */
    @FXML
    private void onCancelChangePin() {
        showState("info");
    }

    /**
     * Handle change PIN keypad press
     */
    @FXML
    private void onChangePinKeypadPress(ActionEvent event) {
        StringBuilder currentBuilder;
        switch (changePinStep) {
            case 0: currentBuilder = oldPinBuilder; break;
            case 1: currentBuilder = newPinBuilder; break;
            case 2: currentBuilder = confirmPinBuilder; break;
            default: return;
        }
        
        if (currentBuilder.length() >= MAX_PIN_LENGTH) return;
        
        Button btn = (Button) event.getSource();
        String digit = (String) btn.getUserData();
        currentBuilder.append(digit);
        updateChangePinDisplay();
    }

    /**
     * Clear all change PIN digits
     */
    @FXML
    private void onChangePinClearAll() {
        switch (changePinStep) {
            case 0: oldPinBuilder.setLength(0); break;
            case 1: newPinBuilder.setLength(0); break;
            case 2: confirmPinBuilder.setLength(0); break;
        }
        updateChangePinDisplay();
    }

    /**
     * Backspace change PIN
     */
    @FXML
    private void onChangePinBackspace() {
        StringBuilder currentBuilder;
        switch (changePinStep) {
            case 0: currentBuilder = oldPinBuilder; break;
            case 1: currentBuilder = newPinBuilder; break;
            case 2: currentBuilder = confirmPinBuilder; break;
            default: return;
        }
        
        if (currentBuilder.length() > 0) {
            currentBuilder.deleteCharAt(currentBuilder.length() - 1);
            updateChangePinDisplay();
        }
    }

    /**
     * Update change PIN display
     */
    private void updateChangePinDisplay() {
        // Update old PIN dots
        int oldLength = oldPinBuilder.length();
        for (int i = 0; i < oldPinDots.length; i++) {
            oldPinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            oldPinDots[i].getStyleClass().add(i < oldLength ? "pin-dot-filled" : "pin-dot-empty");
        }
        
        // Update new PIN dots
        int newLength = newPinBuilder.length();
        for (int i = 0; i < newPinDots.length; i++) {
            newPinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            newPinDots[i].getStyleClass().add(i < newLength ? "pin-dot-filled" : "pin-dot-empty");
        }
        
        // Update confirm PIN dots
        int confirmLength = confirmPinBuilder.length();
        for (int i = 0; i < confirmPinDots.length; i++) {
            confirmPinDots[i].getStyleClass().removeAll("pin-dot-filled", "pin-dot-empty");
            confirmPinDots[i].getStyleClass().add(i < confirmLength ? "pin-dot-filled" : "pin-dot-empty");
        }
        
        // Update instruction and handle auto-advance
        StringBuilder currentBuilder;
        switch (changePinStep) {
            case 0:
                changePinInstructionLabel.setText("Nhập mã PIN hiện tại");
                currentBuilder = oldPinBuilder;
                break;
            case 1:
                changePinInstructionLabel.setText("Nhập mã PIN mới");
                currentBuilder = newPinBuilder;
                break;
            case 2:
                changePinInstructionLabel.setText("Xác nhận mã PIN mới");
                currentBuilder = confirmPinBuilder;
                break;
            default:
                return;
        }
        
        // Auto-advance to next step when current PIN is complete
        if (currentBuilder.length() == MAX_PIN_LENGTH) {
            if (changePinStep < 2) {
                changePinStep++;
                Platform.runLater(() -> updateChangePinDisplay());
            }
        }
        
        // Enable confirm button only when all PINs are entered
        confirmChangePinBtn.setDisable(oldPinBuilder.length() != MAX_PIN_LENGTH || 
                                        newPinBuilder.length() != MAX_PIN_LENGTH || 
                                        confirmPinBuilder.length() != MAX_PIN_LENGTH);
    }

    /**
     * Confirm change PIN
     */
    @FXML
    private void onConfirmChangePin() {
        if (oldPinBuilder.length() != MAX_PIN_LENGTH || 
            newPinBuilder.length() != MAX_PIN_LENGTH || 
            confirmPinBuilder.length() != MAX_PIN_LENGTH) {
            UIUtils.showAlert("Lỗi", "Vui lòng nhập đủ 6 số cho tất cả các trường PIN");
            return;
        }
        
        String newPin = newPinBuilder.toString();
        String confirmPin = confirmPinBuilder.toString();
        
        if (!newPin.equals(confirmPin)) {
            UIUtils.showAlert("Lỗi", "Mã PIN mới và xác nhận không khớp");
            newPinBuilder.setLength(0);
            confirmPinBuilder.setLength(0);
            changePinStep = 1;
            updateChangePinDisplay();
            return;
        }
        
        String oldPin = oldPinBuilder.toString();
        confirmChangePinBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang đổi mã PIN...");
        
        Task<Void> changePinTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Đang gửi lệnh đổi PIN...");
                cardService.changePin(oldPin, newPin);
                return null;
            }
            
            @Override
            protected void succeeded() {
                verifiedPin = newPin; // Update stored PIN
                Platform.runLater(() -> {
                    UIUtils.showAlert("Thành công", "Đổi mã PIN thành công!");
                    showState("info");
                });
            }
            
            @Override
            protected void failed() {
                Throwable ex = getException();
                Platform.runLater(() -> {
                    confirmChangePinBtn.setDisable(false);
                    if (ex.getMessage().contains("6985")) {
                        UIUtils.showAlert("Lỗi", "Mã PIN cũ không đúng. Vui lòng thử lại.");
                        oldPinBuilder.setLength(0);
                        newPinBuilder.setLength(0);
                        confirmPinBuilder.setLength(0);
                        changePinStep = 0;
                        updateChangePinDisplay();
                        showState("changepin");
                    } else if (ex.getMessage().contains("6983")) {
                        errorLabel.setText("Thẻ đã bị khóa do nhập sai PIN quá nhiều lần");
                        showState("error");
                    } else {
                        UIUtils.showAlert("Lỗi", "Không thể đổi PIN: " + ex.getMessage());
                        showState("changepin");
                    }
                });
            }
        };
        
        connectingLabel.textProperty().bind(changePinTask.messageProperty());
        new Thread(changePinTask).start();
    }

    // ============ Edit Info Handlers ============

    /**
     * Handle edit info request
     */
    @FXML
    private void onEditInfo() {
        // Pre-fill current values
        editNameField.setText(nameLabel.getText().equals("Chưa có tên") ? "" : nameLabel.getText());
        editAgeField.setText(ageLabel.getText().equals("Chưa xác định") ? "" : ageLabel.getText());
        
        // Set gender radio button
        String currentGender = genderLabel.getText();
        if ("Nam".equals(currentGender)) {
            maleRadio.setSelected(true);
        } else if ("Nữ".equals(currentGender)) {
            femaleRadio.setSelected(true);
        } else {
            otherRadio.setSelected(true);
        }
        
        editInfoInstructionLabel.setText("");
        showState("editinfo");
    }

    /**
     * Cancel edit info operation
     */
    @FXML
    private void onCancelEditInfo() {
        showState("info");
    }

    /**
     * Save edited info
     */
    @FXML
    private void onSaveInfo() {
        String name = editNameField.getText().trim();
        String ageStr = editAgeField.getText().trim();
        
        // Validate inputs
        if (name.isEmpty()) {
            editInfoInstructionLabel.setText("⚠ Vui lòng nhập tên");
            editInfoInstructionLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }
        
        byte age = 0;
        if (!ageStr.isEmpty()) {
            try {
                int ageInt = Integer.parseInt(ageStr);
                if (ageInt < 1 || ageInt > 150) {
                    editInfoInstructionLabel.setText("⚠ Tuổi phải từ 1-150");
                    editInfoInstructionLabel.setStyle("-fx-text-fill: #ef4444;");
                    return;
                }
                age = (byte) ageInt;
            } catch (NumberFormatException e) {
                editInfoInstructionLabel.setText("⚠ Tuổi phải là số");
                editInfoInstructionLabel.setStyle("-fx-text-fill: #ef4444;");
                return;
            }
        }
        
        byte gender = 0; // 0: other, 1: male, 2: female
        if (maleRadio.isSelected()) {
            gender = 1;
        } else if (femaleRadio.isSelected()) {
            gender = 2;
        }
        
        // Create final variables for use in Task
        final String finalName = name;
        final byte finalAge = age;
        final byte finalGender = gender;
        
        saveInfoBtn.setDisable(true);
        showState("connecting");
        connectingLabel.setText("Đang lưu thông tin...");
        
        Task<Void> saveInfoTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Đang ghi dữ liệu vào thẻ...");
                cardService.writeUserData(finalName, finalAge, finalGender);
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Update display with new values
                    nameLabel.setText(finalName);
                    ageLabel.setText(finalAge > 0 ? String.valueOf(finalAge & 0xFF) : "Chưa xác định");
                    
                    String genderText;
                    switch (finalGender) {
                        case 1: genderText = "Nam"; break;
                        case 2: genderText = "Nữ"; break;
                        default: genderText = "Khác";
                    }
                    genderLabel.setText(genderText);
                    
                    UIUtils.showAlert("Thành công", "Cập nhật thông tin thành công!");
                    showState("info");
                });
            }
            
            @Override
            protected void failed() {
                Throwable ex = getException();
                Platform.runLater(() -> {
                    saveInfoBtn.setDisable(false);
                    UIUtils.showAlert("Lỗi", "Không thể lưu thông tin: " + ex.getMessage());
                    showState("editinfo");
                });
            }
        };
        
        connectingLabel.textProperty().bind(saveInfoTask.messageProperty());
        new Thread(saveInfoTask).start();
    }
}
