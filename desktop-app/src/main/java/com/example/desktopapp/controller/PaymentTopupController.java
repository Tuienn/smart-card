package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.service.TransactionService;
import com.example.desktopapp.service.MomoService;
import com.example.desktopapp.util.AppConfig;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for Payment Top-up screen
 * Handles buying coins or combos for existing card
 */
public class PaymentTopupController implements Initializable {

    // Payment state
    @FXML private VBox paymentState;
    @FXML private Button buyCoinsBtn, buyComboBtn;
    @FXML private VBox coinsSection, comboSection;
    @FXML private VBox customAmountBox;
    @FXML private TextField customAmountField;
    @FXML private Label coinDisplay, selectedAmountLabel;
    @FXML private VBox comboListContainer;
    @FXML private HBox comboLoadingBox;
    @FXML private Label comboErrorLabel;
    private Label selectedCombosSummaryLabel;
    
    // Writing state
    @FXML private VBox writingState;
    @FXML private VBox qrLoadingState;
    @FXML private VBox qrDisplayState;
    @FXML private VBox writingProgress;
    @FXML private VBox successIndicator;
    @FXML private VBox errorIndicator;
    @FXML private ImageView qrImageView;
    @FXML private Label paymentAmountLabel;
    @FXML private Label paymentStatusLabel;
    @FXML private Label writeStatusLabel;
    @FXML private Label errorMessageLabel;
    @FXML private HBox actionButtons;
    
    // Bottom buttons
    @FXML private HBox bottomButtons;
    @FXML private Button confirmBtn;
    
    // State
    private String paymentType = "coins"; // "coins" or "combo"
    private int selectedAmount = 0;
    private List<Integer> selectedComboIds = new ArrayList<>();
    private Map<Integer, short[]> comboGameIdsMap = new HashMap<>();
    private int totalComboPrice = 0;
    private short[] selectedComboGameIds = null;
    private boolean loadingComboDetails = false;
    
    // Services
    private CardService cardService;
    private TransactionService transactionService;
    private MomoService momoService;
    private NumberFormat currencyFormat;
    private String pin; // PIN from previous screen
    
    // QR Payment polling
    private ScheduledExecutorService paymentPollingExecutor;
    private String currentOrderId;
    private volatile boolean isPolling = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        cardService = new CardService();
        transactionService = new TransactionService();
        momoService = new MomoService();
        
        // Setup custom amount field listener
        customAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                customAmountField.setText(newVal.replaceAll("[^\\d]", ""));
            } else {
                updateCoinDisplay();
            }
        });
    }
    
    /**
     * Set PIN for card operations
     */
    public void setPin(String pin) {
        this.pin = pin;
    }
    
    /**
     * Preselect combo mode instead of coins
     */
    public void preselectComboMode() {
        Platform.runLater(() -> onSelectBuyCombo());
    }

    // ============ Payment Type Selection ============
    
    @FXML
    private void onSelectBuyCoins() {
        paymentType = "coins";
        selectedComboIds.clear();
        comboGameIdsMap.clear();
        totalComboPrice = 0;
        selectedComboGameIds = null;
        
        // Update button styles
        buyCoinsBtn.getStyleClass().remove("btn-secondary");
        buyCoinsBtn.getStyleClass().add("btn-primary");
        buyComboBtn.getStyleClass().remove("btn-primary");
        buyComboBtn.getStyleClass().add("btn-secondary");
        
        // Show coins section
        coinsSection.setVisible(true);
        coinsSection.setManaged(true);
        comboSection.setVisible(false);
        comboSection.setManaged(false);
    }
    
    @FXML
    private void onSelectBuyCombo() {
        paymentType = "combo";
        selectedAmount = 0;
        selectedComboIds.clear();
        comboGameIdsMap.clear();
        totalComboPrice = 0;
        selectedComboGameIds = null;
        
        // Update button styles
        buyComboBtn.getStyleClass().remove("btn-secondary");
        buyComboBtn.getStyleClass().add("btn-primary");
        buyCoinsBtn.getStyleClass().remove("btn-primary");
        buyCoinsBtn.getStyleClass().add("btn-secondary");
        
        // Show combo section
        coinsSection.setVisible(false);
        coinsSection.setManaged(false);
        comboSection.setVisible(true);
        comboSection.setManaged(true);
        
        // Load combos if not loaded yet
        if (comboListContainer.getChildren().isEmpty()) {
            loadCombos();
        }
    }

    // ============ Coins Payment ============
    
    @FXML
    private void onAmountSelect(ActionEvent event) {
        Button btn = (Button) event.getSource();
        selectedAmount = Integer.parseInt((String) btn.getUserData());
        
        // Update UI - highlight selected button
        for (var node : ((GridPane) btn.getParent()).getChildren()) {
            if (node instanceof Button) {
                node.getStyleClass().remove("amount-btn-selected");
            }
        }
        btn.getStyleClass().add("amount-btn-selected");
        
        // Hide custom amount
        customAmountBox.setVisible(false);
        customAmountBox.setManaged(false);
        
        updateCoinDisplay();
    }
    
    @FXML
    private void onCustomAmount() {
        selectedAmount = 0;
        customAmountBox.setVisible(true);
        customAmountBox.setManaged(true);
        customAmountField.requestFocus();
        updateCoinDisplay();
    }
    
    private void updateCoinDisplay() {
        int amount = selectedAmount;
        if (amount == 0 && !customAmountField.getText().isEmpty()) {
            try {
                amount = Integer.parseInt(customAmountField.getText());
            } catch (NumberFormatException e) {
                amount = 0;
            }
        }

        int coins = amount / 10000;
        coinDisplay.setText(String.valueOf(coins));
        
        if (amount > 0) {
            selectedAmountLabel.setText("Số tiền: " + currencyFormat.format(amount) + "đ");
        } else {
            selectedAmountLabel.setText("");
        }
        
        selectedAmount = amount;
    }

    // ============ Combo Payment ============
    
    private void loadCombos() {
        comboLoadingBox.setVisible(true);
        comboErrorLabel.setVisible(false);
        
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                java.net.URL url = new java.net.URL(AppConfig.API_COMBOS);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                
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
                    return response.toString();
                } else {
                    throw new Exception("API trả về lỗi: " + responseCode);
                }
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    comboLoadingBox.setVisible(false);
                    displayCombos(getValue());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    comboLoadingBox.setVisible(false);
                    comboErrorLabel.setText("Không thể tải danh sách combo. Vui lòng thử lại.");
                    comboErrorLabel.setVisible(true);
                });
            }
        };
        
        new Thread(loadTask).start();
    }
    
    private void displayCombos(String jsonResponse) {
        try {
            // Parse JSON (similar to CardRegistrationController)
            int dataStart = jsonResponse.indexOf("\"data\":[");
            if (dataStart == -1) {
                throw new Exception("Invalid response format");
            }
            dataStart += 8;
            
            int bracketCount = 0;
            int dataEnd = -1;
            boolean inString = false;
            boolean escaped = false;
            
            for (int i = dataStart; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (!inString) {
                    if (c == '[' || c == '{') {
                        bracketCount++;
                    } else if (c == ']' || c == '}') {
                        bracketCount--;
                        if (c == ']' && bracketCount == -1) {
                            dataEnd = i;
                            break;
                        }
                    }
                }
            }
            
            if (dataEnd == -1) {
                throw new Exception("Invalid response format");
            }
            
            String dataArray = jsonResponse.substring(dataStart, dataEnd).trim();
            
            if (dataArray.isEmpty()) {
                comboErrorLabel.setText("Không có combo nào.");
                comboErrorLabel.setVisible(true);
                return;
            }
            
            comboListContainer.getChildren().clear();
            
            // Parse combo objects
            List<String> comboStrings = new ArrayList<>();
            int start = 0;
            bracketCount = 0;
            inString = false;
            escaped = false;
            
            for (int i = 0; i < dataArray.length(); i++) {
                char c = dataArray.charAt(i);
                
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (!inString) {
                    if (c == '{' || c == '[') {
                        bracketCount++;
                    } else if (c == '}' || c == ']') {
                        bracketCount--;
                        if (c == '}' && bracketCount == 0) {
                            String comboStr = dataArray.substring(start, i + 1);
                            comboStrings.add(comboStr);
                            while (i + 1 < dataArray.length() && 
                                   (dataArray.charAt(i + 1) == ',' || 
                                    Character.isWhitespace(dataArray.charAt(i + 1)))) {
                                i++;
                            }
                            start = i + 1;
                        }
                    }
                }
            }
            
            // Create combo cards
            for (String comboStr : comboStrings) {
                try {
                    comboStr = comboStr.trim();
                    if (comboStr.startsWith("{")) comboStr = comboStr.substring(1);
                    if (comboStr.endsWith("}")) comboStr = comboStr.substring(0, comboStr.length() - 1);
                    
                    int id = parseJsonInt(comboStr, "_id");
                    String name = parseJsonString(comboStr, "name");
                    int price = parseJsonInt(comboStr, "priceVND");
                    int discount = parseJsonInt(comboStr, "discountPercentage");
                    String description = parseJsonString(comboStr, "description");
                    
                    if (name.isEmpty() || price == 0) {
                        continue;
                    }
                    
                    VBox comboCard = createComboCard(id, name, price, discount, description);
                    comboListContainer.getChildren().add(comboCard);
                } catch (Exception e) {
                    System.err.println("Error parsing combo: " + e.getMessage());
                }
            }
            
            if (comboListContainer.getChildren().isEmpty()) {
                comboErrorLabel.setText("Không tìm thấy combo nào.");
                comboErrorLabel.setVisible(true);
            }
            
        } catch (Exception e) {
            comboErrorLabel.setText("Lỗi khi hiển thị combo: " + e.getMessage());
            comboErrorLabel.setVisible(true);
        }
    }
    
    private int parseJsonInt(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":") + key.length() + 3;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.length();
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String parseJsonString(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":\"") + key.length() + 4;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }
    
    private VBox createComboCard(int id, String name, int price, int discount, String description) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-panel");
        card.setStyle("-fx-padding: 20; -fx-cursor: hand;");
        card.setMaxWidth(650);
        
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        nameLabel.getStyleClass().add("text-primary");
        
        Label discountBadge = new Label("-" + discount + "%");
        discountBadge.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; " +
                "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        header.getChildren().addAll(nameLabel, discountBadge);
        
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-secondary");
        descLabel.setWrapText(true);
        
        Label priceLabel = new Label("Giá: " + currencyFormat.format(price) + "đ");
        priceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        
        card.getChildren().addAll(header, descLabel, priceLabel);
        card.setOnMouseClicked(e -> onComboSelect(id, price, card));
        
        return card;
    }
    
    private void onComboSelect(int comboId, int price, VBox selectedCard) {
        // Toggle selection
        if (selectedComboIds.contains(comboId)) {
            selectedComboIds.remove(Integer.valueOf(comboId));
            comboGameIdsMap.remove(comboId);
            totalComboPrice -= price;
            
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-color: #3b82f6;", ""));
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-width: 2;", ""));
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-radius: 8;", ""));
        } else {
            selectedComboIds.add(comboId);
            totalComboPrice += price;
            
            selectedCard.setStyle(selectedCard.getStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 8;");
            
            fetchComboDetails(comboId, price);
        }
        
        updateSelectedCombosSummary();
    }
    
    private void fetchComboDetails(int comboId, int price) {
        loadingComboDetails = true;
        
        Task<short[]> fetchTask = new Task<>() {
            @Override
            protected short[] call() throws Exception {
                java.net.URL url = new java.net.URL(AppConfig.API_COMBOS + "/" + comboId);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");
                
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
                    
                    return parseComboGameIds(response.toString());
                } else {
                    throw new Exception("API trả về lỗi: " + responseCode);
                }
            }

            @Override
            protected void succeeded() {
                short[] gameIds = getValue();
                if (gameIds != null && gameIds.length > 0) {
                    comboGameIdsMap.put(comboId, gameIds);
                    mergeComboGameIds();
                }
                loadingComboDetails = false;
            }

            @Override
            protected void failed() {
                loadingComboDetails = false;
                selectedComboIds.remove(Integer.valueOf(comboId));
                totalComboPrice -= price;
                updateSelectedCombosSummary();
                UIUtils.showAlert("Lỗi", "Không thể tải chi tiết combo. Vui lòng thử lại.");
            }
        };
        
        new Thread(fetchTask).start();
    }
    
    private short[] parseComboGameIds(String jsonResponse) {
        try {
            int gamesStart = jsonResponse.indexOf("\"games\":[");
            if (gamesStart == -1) {
                return new short[0];
            }
            gamesStart += 9;
            
            int gamesEnd = jsonResponse.indexOf("]", gamesStart);
            String gamesArray = jsonResponse.substring(gamesStart, gamesEnd).trim();
            
            if (gamesArray.isEmpty()) {
                return new short[0];
            }
            
            String[] gameIdStrings = gamesArray.split(",");
            short[] gameIds = new short[gameIdStrings.length];
            
            for (int i = 0; i < gameIdStrings.length; i++) {
                gameIds[i] = Short.parseShort(gameIdStrings[i].trim());
            }
            
            return gameIds;
        } catch (Exception e) {
            return new short[0];
        }
    }
    
    private void mergeComboGameIds() {
        List<Short> allGameIds = new ArrayList<>();
        for (Integer comboId : selectedComboIds) {
            short[] gameIds = comboGameIdsMap.get(comboId);
            if (gameIds != null) {
                for (short gameId : gameIds) {
                    allGameIds.add(gameId);
                }
            }
        }
        
        selectedComboGameIds = new short[allGameIds.size()];
        for (int i = 0; i < allGameIds.size(); i++) {
            selectedComboGameIds[i] = allGameIds.get(i);
        }
    }
    
    private void updateSelectedCombosSummary() {
        if (selectedCombosSummaryLabel == null) {
            selectedCombosSummaryLabel = new Label();
            selectedCombosSummaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3b82f6; -fx-padding: 15;");
            selectedCombosSummaryLabel.setWrapText(true);
            if (!comboSection.getChildren().isEmpty()) {
                comboSection.getChildren().add(0, selectedCombosSummaryLabel);
            }
        }
        
        if (selectedComboIds.isEmpty()) {
            selectedCombosSummaryLabel.setText("");
            selectedCombosSummaryLabel.setVisible(false);
        } else {
            selectedCombosSummaryLabel.setText("Đã chọn " + selectedComboIds.size() + " combo - Tổng: " + 
                currencyFormat.format(totalComboPrice) + "đ");
            selectedCombosSummaryLabel.setVisible(true);
        }
    }

    // ============ Confirm and QR Payment ============
    
    @FXML
    private void onConfirm() {
        // Validate
        if (paymentType.equals("coins")) {
            if (selectedAmount <= 0) {
                UIUtils.showAlert("Lỗi", "Vui lòng chọn số tiền nạp");
                return;
            }
            if (selectedAmount < 10000) {
                UIUtils.showAlert("Lỗi", "Số tiền tối thiểu là 10,000đ");
                return;
            }
        } else if (paymentType.equals("combo")) {
            if (selectedComboIds.isEmpty()) {
                UIUtils.showAlert("Lỗi", "Vui lòng chọn ít nhất một combo");
                return;
            }
            if (loadingComboDetails) {
                UIUtils.showAlert("Vui lòng đợi", "Đang tải thông tin combo...");
                return;
            }
            if (selectedComboGameIds == null || selectedComboGameIds.length == 0) {
                UIUtils.showAlert("Lỗi", "Không thể tải danh sách game trong combo. Vui lòng chọn lại.");
                return;
            }
        }
        
        // Show QR payment state and start payment flow
        showWritingState();
        startQrPayment();
    }
    
    private void showWritingState() {
        paymentState.setVisible(false);
        writingState.setVisible(true);
        bottomButtons.setVisible(false);
        
        qrLoadingState.setVisible(true);
        qrDisplayState.setVisible(false);
        writingProgress.setVisible(false);
        successIndicator.setVisible(false);
        errorIndicator.setVisible(false);
        actionButtons.setVisible(false);
    }
    
    /**
     * Start QR payment flow
     */
    private void startQrPayment() {
        // Calculate amount
        int amount = paymentType.equals("coins") ? selectedAmount : totalComboPrice;
        String description = "TOPUP" + System.currentTimeMillis();
        
        paymentAmountLabel.setText("Số tiền: " + currencyFormat.format(amount) + "đ");
        
        // Create QR in background
        Task<MomoService.QrPaymentResponse> qrTask = new Task<>() {
            @Override
            protected MomoService.QrPaymentResponse call() throws Exception {
                return momoService.createQrPayment(amount, description);
            }
            
            @Override
            protected void succeeded() {
                MomoService.QrPaymentResponse response = getValue();
                Platform.runLater(() -> {
                    if (response.resultCode == 0 && response.qrCodeUrl != null) {
                        // Show QR code
                        qrLoadingState.setVisible(false);
                        qrDisplayState.setVisible(true);
                        
                        // Load QR image
                        Image qrImage = new Image(response.qrCodeUrl, true);
                        qrImageView.setImage(qrImage);
                        
                        currentOrderId = response.orderId;
                        paymentStatusLabel.setText("Đang chờ thanh toán...");
                        
                        // Start polling for payment status
                        startPaymentPolling(response.orderId);
                    } else {
                        showQrError("Lỗi tạo QR: " + (response.message != null ? response.message : "Unknown error"));
                    }
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showQrError("Lỗi kết nối: " + getException().getMessage());
                });
            }
        };
        
        new Thread(qrTask).start();
    }
    
    private void showQrError(String message) {
        qrLoadingState.setVisible(false);
        qrDisplayState.setVisible(false);
        errorIndicator.setVisible(true);
        actionButtons.setVisible(true);
        errorMessageLabel.setText(message);
    }
    
    /**
     * Start polling payment status every 1 second
     */
    private void startPaymentPolling(String orderId) {
        stopPaymentPolling();
        
        isPolling = true;
        paymentPollingExecutor = Executors.newSingleThreadScheduledExecutor();
        
        paymentPollingExecutor.scheduleAtFixedRate(() -> {
            if (!isPolling) return;
            
            try {
                MomoService.PaymentStatusResponse status = momoService.checkPaymentStatus(orderId);
                
                Platform.runLater(() -> {
                    if (status.isSuccess()) {
                        stopPaymentPolling();
                        paymentStatusLabel.setText("Thanh toán thành công! Đang ghi thẻ...");
                        writeToCard();
                    } else if (!status.isPending()) {
                        stopPaymentPolling();
                        showQrError("Thanh toán thất bại: " + status.message);
                    } else {
                        paymentStatusLabel.setText("Đang chờ thanh toán...");
                    }
                });
            } catch (Exception e) {
                System.err.println("Error polling payment status: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    /**
     * Stop payment status polling
     */
    private void stopPaymentPolling() {
        isPolling = false;
        if (paymentPollingExecutor != null && !paymentPollingExecutor.isShutdown()) {
            paymentPollingExecutor.shutdown();
            try {
                paymentPollingExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                paymentPollingExecutor.shutdownNow();
            }
        }
    }
    
    /**
     * Retry payment (called from UI)
     */
    @FXML
    private void onRetryPayment() {
        startQrPayment();
    }
    
    /**
     * Write data to card after successful payment
     */
    private void writeToCard() {
        qrDisplayState.setVisible(false);
        writingProgress.setVisible(true);
        
        Task<Void> writeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Đang kết nối thẻ...");
                cardService.connect();
                
                updateMessage("Đang xác thực PIN...");
                cardService.verifyPin(pin);
                
                if (paymentType.equals("coins")) {
                    updateMessage("Đang nạp coins...");
                    int coins = selectedAmount / 10000;
                    cardService.topupCoins(coins);
                } else {
                    updateMessage("Đang ghi combo vào thẻ...");
                    cardService.purchaseCombo(selectedComboGameIds, totalComboPrice);
                }
                
                // Save transaction to database
                updateMessage("Đang lưu giao dịch...");
                try {
                    byte[] userIdBytes = cardService.readUserId();
                    String cardId = bytesToHex(userIdBytes);
                    int userAge = cardService.readAge() & 0xFF;
                    
                    if (paymentType.equals("coins")) {
                        transactionService.createTopupTransaction(cardId, userAge, selectedAmount);
                    } else {
                        // For combo, need to get combo ID from selectedComboIds
                        String comboId = selectedComboIds.isEmpty() ? null : String.valueOf(selectedComboIds.get(0));
                        if (comboId != null) {
                            transactionService.createComboTransaction(cardId, userAge, comboId, totalComboPrice);
                        }
                    }
                    System.out.println("✓ Transaction saved to database");
                } catch (Exception e) {
                    System.err.println("✗ Failed to save transaction: " + e.getMessage());
                    // Don't fail the whole flow if transaction save fails
                }
                
                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    writingProgress.setVisible(false);
                    successIndicator.setVisible(true);
                    actionButtons.setVisible(true);
                    
                    // Auto return to card info after 3 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            Platform.runLater(() -> goToCardInfo());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    writingProgress.setVisible(false);
                    errorIndicator.setVisible(true);
                    actionButtons.setVisible(true);
                    
                    Throwable ex = getException();
                    errorMessageLabel.setText(ex != null ? ex.getMessage() : "Có lỗi xảy ra");
                });
            }
        };
        
        writeStatusLabel.textProperty().bind(writeTask.messageProperty());
        new Thread(writeTask).start();
    }
    
    @FXML
    private void onCancel() {
        stopPaymentPolling();
        if (cardService != null) {
            cardService.disconnect();
        }
        MainApp.setRoot("card-info.fxml");
    }
    
    /**
     * Navigate back to card info screen after successful payment
     */
    private void goToCardInfo() {
        stopPaymentPolling();
        if (cardService != null) {
            cardService.disconnect();
        }
        MainApp.setRoot("card-info.fxml");
    }

    /**
     * Convert byte array to hex string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
