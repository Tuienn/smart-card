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
    @FXML private FlowPane purchasedGamesContainer;
    @FXML private Label noGamesLabel;
    @FXML private HBox actionButtonsBox;

    // Error state
    @FXML private Label errorLabel;
    @FXML private Label titleLabel;
    @FXML private Button unlockCardBtn;
    @FXML private VBox unlockWarningBox;

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
            }
            return;
        }
        
        if (noGamesLabel != null) {
            noGamesLabel.setVisible(false);
        }
        
        // Load game names from backend and display
        Task<Void> loadGamesTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (short gameId : gameIds) {
                    try {
                        String gameName = fetchGameName(gameId);
                        Platform.runLater(() -> {
                            Label gameLabel = createGameLabel(gameId, gameName);
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
    private Label createGameLabel(short gameId, String gameName) {
        Label label = new Label(gameName);
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
        // TODO: Implement top-up flow
        UIUtils.showAlert("Thông báo", "Tính năng nạp thêm coins đang được phát triển");
    }
    
    /**
     * Buy combo
     */
    @FXML
    private void onBuyCombo() {
        // TODO: Implement combo purchase flow
        UIUtils.showAlert("Thông báo", "Tính năng mua combo đang được phát triển");
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
}
