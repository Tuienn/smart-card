package com.example.desktopapp.controller;

import com.example.desktopapp.MainApp;
import com.example.desktopapp.model.UserRegistration;
import com.example.desktopapp.service.APDUConstants;
import com.example.desktopapp.service.CardService;
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
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for Card Registration wizard
 */
public class CardRegistrationController implements Initializable {

    // Step indicators
    @FXML private Label step1Label, step2Label, step3Label, step4Label, step5Label;
    @FXML private Region line1, line2, line3, line4;
    @FXML private Label stepTitle;

    // Content panels
    @FXML private StackPane contentStack;
    @FXML private VBox step1Content, step2Content, step3Content, step4Content, step5Content;

    // Step 1: User Info
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private FontIcon avatarPlaceholderText;
    @FXML private TextField nameField, ageField;
    @FXML private ToggleButton maleBtn, femaleBtn;

    // Step 2: PIN Keypad
    @FXML private Label pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6;
    @FXML private Label pinInstructionLabel;
    private Label[] pinDots;
    private StringBuilder pinBuilder = new StringBuilder();

    // Step 3: Payment
    @FXML private Button buyCoinsBtn, buyComboBtn;
    @FXML private VBox coinsSection, comboSection;
    @FXML private VBox customAmountBox;
    @FXML private TextField customAmountField;
    @FXML private Label coinDisplay, selectedAmountLabel;
    @FXML private VBox comboListContainer;
    @FXML private HBox comboLoadingBox;
    @FXML private Label comboErrorLabel;
    private Label selectedCombosSummaryLabel; // Label hiển thị tổng quan các combo đã chọn

    // Step 4: Card Writing
    @FXML private VBox cardWaitingState, cardWritingState, cardSuccessState, cardErrorState;
    @FXML private ProgressIndicator writeProgress;
    @FXML private Label writeStatusLabel, errorMessageLabel;
    @FXML private Button writeCardBtn;

    // Step 5: Success
    @FXML private Label summaryName, summaryAge, summaryCoins;

    // Navigation
    @FXML private Button backBtn, nextBtn;

    // State
    private int currentStep = 1;
    private UserRegistration user = new UserRegistration();
    private byte[] avatarBytes = null;
    private int selectedAmount = 0;
    private java.util.List<Integer> selectedComboIds = new java.util.ArrayList<>(); // Danh sách combo đã chọn
    private java.util.Map<Integer, short[]> comboGameIdsMap = new java.util.HashMap<>(); // Map combo ID -> game IDs
    private int totalComboPrice = 0; // Tổng giá các combo đã chọn
    private short[] selectedComboGameIds = null; // Merged game IDs from all selected combos
    private boolean loadingComboDetails = false; // Loading state for combo details
    private String paymentType = "coins"; // "coins" or "combo"
    private ToggleGroup genderGroup;
    private CardService cardService;
    private NumberFormat currencyFormat;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Setup gender toggle group
        genderGroup = new ToggleGroup();
        maleBtn.setToggleGroup(genderGroup);
        femaleBtn.setToggleGroup(genderGroup);

        // Setup currency format
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Setup card service
        cardService = new CardService();

        // Setup custom amount field listener
        customAmountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                customAmountField.setText(newVal.replaceAll("[^\\d]", ""));
            } else {
                updateCoinDisplay();
            }
        });

        // Initialize PIN dots array
        pinDots = new Label[]{pinDot1, pinDot2, pinDot3, pinDot4, pinDot5, pinDot6};

        // Initialize UI
        updateStepUI();
    }

    // ============ PIN Keypad Handlers ============

    private static final int MAX_PIN_LENGTH = 6;

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

        // Update instruction based on state
        if (length == 0) {
            pinInstructionLabel.setText("Nhập mã PIN 6 số");
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
        } else if (length < MAX_PIN_LENGTH) {
            pinInstructionLabel.setText("Còn " + (MAX_PIN_LENGTH - length) + " số nữa");
            pinInstructionLabel.getStyleClass().remove("pin-instruction-complete");
        } else {
            pinInstructionLabel.setText("Hoàn tất");
            pinInstructionLabel.setGraphic(createIcon(FontAwesomeSolid.CHECK, "#22c55e", 14));
            if (!pinInstructionLabel.getStyleClass().contains("pin-instruction-complete")) {
                pinInstructionLabel.getStyleClass().add("pin-instruction-complete");
            }
            
            // Auto-advance to next step when PIN is complete
            Platform.runLater(() -> onNext());
        }
    }

    @FXML
    private void onAvatarClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file != null) {
            try {
                avatarBytes = Files.readAllBytes(file.toPath());
                
                // Resize if too large (max 32KB for smart card)
                if (avatarBytes.length > APDUConstants.MAX_IMAGE_SIZE) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Cảnh báo");
                    confirmAlert.setHeaderText("Ảnh quá lớn (" + formatFileSize(avatarBytes.length) + ")");
                    confirmAlert.setContentText("Ảnh sẽ được tự động thu nhỏ để phù hợp với thẻ (tối đa " + 
                                                formatFileSize(APDUConstants.MAX_IMAGE_SIZE) + ").\n" +
                                                "Ảnh có thể bị mờ sau khi thu nhỏ.\n\n" +
                                                "Bạn có muốn tiếp tục?");
                    
                    ButtonType result = confirmAlert.showAndWait().orElse(ButtonType.CANCEL);
                    if (result != ButtonType.OK) {
                        avatarBytes = null;
                        return;
                    }
                    
                    // Resize the image
                    avatarBytes = resizeImage(avatarBytes, APDUConstants.MAX_IMAGE_SIZE);
                    if (avatarBytes == null) {
                        showAlert("Lỗi", "Không thể thu nhỏ ảnh. Vui lòng chọn ảnh khác.");
                        return;
                    }
                }

                Image image = new Image(new ByteArrayInputStream(avatarBytes));
                avatarImage.setImage(image);
                avatarPlaceholderText.setVisible(false);
            } catch (IOException e) {
                showAlert("Lỗi", "Không thể đọc file ảnh: " + e.getMessage());
            }
        }
    }
    
    private byte[] resizeImage(byte[] originalBytes, int maxSize) {
        try {
            // Load original image using javax.imageio
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (originalImage == null) {
                return null;
            }
            
            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            
            // Try progressively smaller sizes until we fit under maxSize
            double scale = 1.0;
            for (int attempt = 0; attempt < 10; attempt++) {
                // Reduce dimensions by 20% each iteration
                scale *= 0.8;
                int targetWidth = (int) (originalWidth * scale);
                int targetHeight = (int) (originalHeight * scale);
                
                // Ensure minimum size
                if (targetWidth < 50 || targetHeight < 50) {
                    break;
                }
                
                // Resize image with high-quality scaling
                BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
                g2d.dispose();
                
                // Convert to JPEG bytes with compression
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                if (!writers.hasNext()) {
                    return null;
                }
                
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.7f); // 70% quality
                }
                
                ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
                writer.setOutput(ios);
                writer.write(null, new IIOImage(resizedImage, null, null), param);
                writer.dispose();
                ios.close();
                
                byte[] resizedBytes = baos.toByteArray();
                
                // Check if size is acceptable
                if (resizedBytes.length <= maxSize) {
                    return resizedBytes;
                }
            }
            
            return null; // Could not resize to fit
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private String formatFileSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    @FXML
    private void onGenderSelect(ActionEvent event) {
        ToggleButton selected = (ToggleButton) event.getSource();
        byte gender = Byte.parseByte((String) selected.getUserData());
        user.setGender(gender);
    }

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
        
        // Show coins section, hide combo section
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
        
        // Show combo section, hide coins section
        coinsSection.setVisible(false);
        coinsSection.setManaged(false);
        comboSection.setVisible(true);
        comboSection.setManaged(true);
        
        // Load combos if not loaded yet
        if (comboListContainer.getChildren().isEmpty()) {
            loadCombos();
        }
    }

    private void loadCombos() {
        comboLoadingBox.setVisible(true);
        comboErrorLabel.setVisible(false);
        
        Task<String> loadTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Call backend API to get combos
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
            // Parse JSON more carefully
            // Expected format: {"success":true,"data":[{combo1},{combo2},...]}
            
            // Find the data array
            int dataStart = jsonResponse.indexOf("\"data\":[");
            if (dataStart == -1) {
                throw new Exception("Invalid response format: missing 'data' field");
            }
            dataStart += 8; // Move past "data":[
            
            // Find matching closing bracket for data array
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
                throw new Exception("Invalid response format: unclosed array");
            }
            
            String dataArray = jsonResponse.substring(dataStart, dataEnd).trim();
            
            if (dataArray.isEmpty() || dataArray.equals("")) {
                comboErrorLabel.setText("Không có combo nào.");
                comboErrorLabel.setVisible(true);
                return;
            }
            
            comboListContainer.getChildren().clear();
            
            // Parse each combo object manually
            // Split by },{ pattern but only at top level
            java.util.List<String> comboStrings = new java.util.ArrayList<>();
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
                            // Found end of a top-level object
                            String comboStr = dataArray.substring(start, i + 1);
                            comboStrings.add(comboStr);
                            // Skip comma and whitespace
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
            
            // Create cards for each combo
            for (String comboStr : comboStrings) {
                try {
                    // Remove outer braces
                    comboStr = comboStr.trim();
                    if (comboStr.startsWith("{")) comboStr = comboStr.substring(1);
                    if (comboStr.endsWith("}")) comboStr = comboStr.substring(0, comboStr.length() - 1);
                    
                    // Parse fields
                    int id = parseJsonInt(comboStr, "_id");
                    String name = parseJsonString(comboStr, "name");
                    int price = parseJsonInt(comboStr, "priceVND");
                    int discount = parseJsonInt(comboStr, "discountPercentage");
                    String description = parseJsonString(comboStr, "description");
                    
                    // Skip if essential fields are missing
                    if (name.isEmpty() || price == 0) {
                        continue;
                    }
                    
                    // Create combo card
                    VBox comboCard = createComboCard(id, name, price, discount, description);
                    comboListContainer.getChildren().add(comboCard);
                } catch (Exception e) {
                    System.err.println("Error parsing combo: " + e.getMessage());
                    // Skip this combo and continue with others
                }
            }
            
            if (comboListContainer.getChildren().isEmpty()) {
                comboErrorLabel.setText("Không tìm thấy combo nào.");
                comboErrorLabel.setVisible(true);
            }
            
        } catch (Exception e) {
            comboErrorLabel.setText("Lỗi khi hiển thị combo: " + e.getMessage());
            comboErrorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private int parseJsonInt(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":")+key.length()+3;
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
        
        // Header with name and discount badge
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        nameLabel.getStyleClass().add("text-primary");
        
        Label discountBadge = new Label("-" + discount + "%");
        discountBadge.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; "
                + "-fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        header.getChildren().addAll(nameLabel, discountBadge);
        
        // Description
        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("text-secondary");
        descLabel.setWrapText(true);
        
        // Price
        Label priceLabel = new Label("Giá: " + currencyFormat.format(price) + "đ");
        priceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        
        card.getChildren().addAll(header, descLabel, priceLabel);
        
        // Click handler
        card.setOnMouseClicked(e -> onComboSelect(id, price, card));
        
        return card;
    }

    private void onComboSelect(int comboId, int price, VBox selectedCard) {
        // Toggle chọn/bỏ chọn combo
        if (selectedComboIds.contains(comboId)) {
            // Bỏ chọn combo
            selectedComboIds.remove(Integer.valueOf(comboId));
            comboGameIdsMap.remove(comboId);
            totalComboPrice -= price;
            
            // Xóa highlight
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-color: #3b82f6;", ""));
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-width: 2;", ""));
            selectedCard.setStyle(selectedCard.getStyle().replace("-fx-border-radius: 8;", ""));
        } else {
            // Chọn combo
            selectedComboIds.add(comboId);
            totalComboPrice += price;
            
            // Thêm highlight
            selectedCard.setStyle(selectedCard.getStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 8;");
            
            // Fetch combo details from backend to get game IDs
            fetchComboDetails(comboId, price);
        }
        
        // Cập nhật tổng giá
        user.setAmountVND(totalComboPrice);
        updateSelectedCombosSummary();
    }

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
        
        user.setAmountVND(amount);
    }
    
    /**
     * Fetch combo details from backend to get game IDs
     */
    private void fetchComboDetails(int comboId, int price) {
        loadingComboDetails = true;
        Task<short[]> fetchTask = new Task<>() {
            @Override
            protected short[] call() throws Exception {
                // Call backend API to get combo details
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
                    
                    // Parse game IDs from response
                    String jsonResponse = response.toString();
                    return parseComboGameIds(jsonResponse);
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
                    System.out.println("Loaded " + gameIds.length + " games for combo " + comboId);
                }
                loadingComboDetails = false;
            }

            @Override
            protected void failed() {
                loadingComboDetails = false;
                // Remove combo khỏi danh sách đã chọn nếu không load được
                selectedComboIds.remove(Integer.valueOf(comboId));
                totalComboPrice -= price;
                user.setAmountVND(totalComboPrice);
                updateSelectedCombosSummary();
                System.err.println("Failed to fetch combo details: " + getException().getMessage());
                showAlert("Lỗi", "Không thể tải chi tiết combo. Vui lòng thử lại.");
            }
        };
        
        new Thread(fetchTask).start();
    }
    
    /**
     * Merge game IDs from all selected combos
     */
    private void mergeComboGameIds() {
        java.util.List<Short> allGameIds = new java.util.ArrayList<>();
        for (Integer comboId : selectedComboIds) {
            short[] gameIds = comboGameIdsMap.get(comboId);
            if (gameIds != null) {
                for (short gameId : gameIds) {
                    allGameIds.add(gameId);
                }
            }
        }
        
        // Convert to array
        selectedComboGameIds = new short[allGameIds.size()];
        for (int i = 0; i < allGameIds.size(); i++) {
            selectedComboGameIds[i] = allGameIds.get(i);
        }
        
        System.out.println("Merged " + selectedComboGameIds.length + " total games from " + selectedComboIds.size() + " combos");
    }
    
    /**
     * Update summary label showing selected combos
     */
    private void updateSelectedCombosSummary() {
        if (selectedCombosSummaryLabel == null) {
            // Create label if not exists
            selectedCombosSummaryLabel = new Label();
            selectedCombosSummaryLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3b82f6; -fx-padding: 15;");
            selectedCombosSummaryLabel.setWrapText(true);
            // Insert at top of combo section
            if (!comboSection.getChildren().isEmpty()) {
                comboSection.getChildren().add(0, selectedCombosSummaryLabel);
            }
        }
        
        if (selectedComboIds.isEmpty()) {
            selectedCombosSummaryLabel.setText("");
            selectedCombosSummaryLabel.setVisible(false);
        } else {
            selectedCombosSummaryLabel.setText("Đã chọn " + selectedComboIds.size() + " combo - Tổng: " + currencyFormat.format(totalComboPrice) + "đ");
            selectedCombosSummaryLabel.setVisible(true);
        }
    }
    
    /**
     * Parse game IDs from combo JSON response
     * Expected: {"success":true,"data":{"games":[1,2,3,...]}}
     */
    private short[] parseComboGameIds(String jsonResponse) {
        try {
            // Find games array in data.games
            int gamesStart = jsonResponse.indexOf("\"games\":[");
            if (gamesStart == -1) {
                // Try alternative format: "gameIds"
                gamesStart = jsonResponse.indexOf("\"gameIds\":[");
            }
            
            if (gamesStart == -1) {
                throw new Exception("No games array found in response");
            }
            
            gamesStart += (jsonResponse.charAt(gamesStart + 1) == 'g' && jsonResponse.charAt(gamesStart + 2) == 'a' && jsonResponse.charAt(gamesStart + 3) == 'm' && jsonResponse.charAt(gamesStart + 4) == 'e' && jsonResponse.charAt(gamesStart + 5) == 's') ? 9 : 11; // Skip \"games\":[ or \"gameIds\":[
            
            int gamesEnd = jsonResponse.indexOf(']', gamesStart);
            if (gamesEnd == -1) {
                throw new Exception("Unclosed games array");
            }
            
            String gamesStr = jsonResponse.substring(gamesStart, gamesEnd).trim();
            if (gamesStr.isEmpty()) {
                return new short[0];
            }
            
            // Split by comma and parse
            String[] gameStrs = gamesStr.split(",");
            short[] gameIds = new short[gameStrs.length];
            
            for (int i = 0; i < gameStrs.length; i++) {
                gameIds[i] = Short.parseShort(gameStrs[i].trim());
            }
            
            return gameIds;
        } catch (Exception e) {
            System.err.println("Error parsing game IDs: " + e.getMessage());
            return new short[0];
        }
    }

    @FXML
    private void onWriteCard() {
        // Show writing state
        cardWaitingState.setVisible(false);
        cardWritingState.setVisible(true);
        cardSuccessState.setVisible(false);
        cardErrorState.setVisible(false);
        writeCardBtn.setDisable(true);

        // Run card writing in background
        Task<Void> writeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Đang kết nối với thẻ...");
                cardService.connect();

                // Check if card is already initialized
                updateMessage("Đang kiểm tra trạng thái thẻ...");
                if (cardService.isCardInitialized()) {
                    throw new Exception("Thẻ đã được khởi tạo! Không thể đăng ký lại.\nVui lòng sử dụng thẻ mới hoặc reset thẻ cũ.");
                }

                updateMessage("Đang khởi tạo thẻ...");
                user.generateUserId();
                byte[] publicKey = cardService.installCard(user);

                updateMessage("Đang đăng ký thẻ vào hệ thống...");
                // Register card to backend with public key
                Toggle selectedGender = genderGroup.getSelectedToggle();
                boolean isMale = (selectedGender == maleBtn);
                cardService.registerCardToBackend(
                    user.getUserId(),
                    publicKey,
                    user.getName(),
                    Integer.parseInt(user.getAge()),
                    isMale
                );

                updateMessage("Đang ghi thông tin người dùng...");
                cardService.writeUserData(user);

                // Handle payment based on type
                System.out.println("Payment type: " + paymentType);
                System.out.println("Combo game IDs: " + (selectedComboGameIds != null ? selectedComboGameIds.length : "null"));
                System.out.println("User coins: " + user.getCoins());
                
                if (paymentType.equals("combo") && selectedComboGameIds != null && selectedComboGameIds.length > 0) {
                    // Purchase combo
                    updateMessage("Đang mua combo (" + selectedComboGameIds.length + " games)...");
                    int totalPrice = user.getCoins(); // Price is already converted to coins
                    System.out.println("Calling purchaseCombo with gameIds: " + java.util.Arrays.toString(selectedComboGameIds) + ", price: " + totalPrice);
                    cardService.purchaseCombo(selectedComboGameIds, totalPrice);
                } else if (user.getCoins() > 0) {
                    // Top up coins
                    updateMessage("Đang nạp " + user.getCoins() + " coins...");
                    cardService.topupCoins(user.getCoins());
                }

                if (avatarBytes != null) {
                    updateMessage("Đang ghi ảnh đại diện...");
                    cardService.writeAvatar(avatarBytes, (byte) 0x01);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                cardService.disconnect();
                Platform.runLater(() -> {
                    cardWritingState.setVisible(false);
                    cardSuccessState.setVisible(true);
                    writeCardBtn.setVisible(false);
                    
                    // Auto advance to step 4 after 2 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(() -> goToStep(5));
                        } catch (InterruptedException ignored) {}
                    }).start();
                });
            }

            @Override
            protected void failed() {
                cardService.disconnect();
                Platform.runLater(() -> {
                    cardWritingState.setVisible(false);
                    cardErrorState.setVisible(true);
                    errorMessageLabel.setText(getException().getMessage());
                    writeCardBtn.setDisable(false);
                    writeCardBtn.setText(" THỬ LẠI");
                    writeCardBtn.setGraphic(createIcon(FontAwesomeSolid.REDO, "white", 16));
                });
            }
        };

        writeStatusLabel.textProperty().bind(writeTask.messageProperty());
        new Thread(writeTask).start();
    }

    @FXML
    private void onBack() {
        if (currentStep > 1) {
            goToStep(currentStep - 1);
        }
    }

    @FXML
    private void onNext() {
        if (currentStep == 5) {
            // Reset for new registration
            resetForm();
            goToStep(1);
            return;
        }

        // Validate current step
        if (!validateCurrentStep()) {
            return;
        }

        // Save data and go to next step
        if (currentStep < 5) {
            goToStep(currentStep + 1);
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                // Validate user info
                String name = nameField.getText().trim();
                String age = ageField.getText().trim();

                if (name.isEmpty()) {
                    showAlert("Lỗi", "Vui lòng nhập họ tên");
                    nameField.requestFocus();
                    return false;
                }
                if (age.isEmpty()) {
                    showAlert("Lỗi", "Vui lòng nhập tuổi");
                    ageField.requestFocus();
                    return false;
                }
                if (!age.matches("\\d+") || Integer.parseInt(age) < 1 || Integer.parseInt(age) > 150) {
                    showAlert("Lỗi", "Tuổi không hợp lệ");
                    ageField.requestFocus();
                    return false;
                }

                // Save user data
                user.setName(name);
                user.setAge(age);
                user.setAvatar(avatarBytes);
                return true;

            case 2:
                // Validate PIN
                if (pinBuilder.length() != MAX_PIN_LENGTH) {
                    showAlert("Lỗi", "Vui lòng nhập đủ 6 số PIN");
                    return false;
                }
                user.setPin(pinBuilder.toString());
                return true;

            case 3:
                // Validate payment
                if (paymentType.equals("coins")) {
                    updateCoinDisplay();
                    if (user.getAmountVND() <= 0) {
                        showAlert("Lỗi", "Vui lòng chọn số tiền nạp");
                        return false;
                    }
                    if (user.getAmountVND() < 10000) {
                        showAlert("Lỗi", "Số tiền tối thiểu là 10,000đ");
                        return false;
                    }
                } else if (paymentType.equals("combo")) {
                    if (selectedComboIds.isEmpty()) {
                        showAlert("Lỗi", "Vui lòng chọn ít nhất một combo");
                        return false;
                    }
                    if (loadingComboDetails) {
                        showAlert("Vui lòng đợi", "Đang tải thông tin combo...");
                        return false;
                    }
                    if (selectedComboGameIds == null || selectedComboGameIds.length == 0) {
                        showAlert("Lỗi", "Không thể tải danh sách game trong combo. Vui lòng chọn lại.");
                        return false;
                    }
                }
                return true;

            case 4:
                // Card writing - handled by onWriteCard
                return true;

            default:
                return true;
        }
    }

    private void goToStep(int step) {
        currentStep = step;
        updateStepUI();
    }

    private void updateStepUI() {
        // Update step indicators
        updateStepIndicator(step1Label, line1, currentStep > 1, currentStep == 1);
        updateStepIndicator(step2Label, line2, currentStep > 2, currentStep == 2);
        updateStepIndicator(step3Label, line3, currentStep > 3, currentStep == 3);
        updateStepIndicator(step4Label, line4, currentStep > 4, currentStep == 4);
        updateStepIndicator(step5Label, null, false, currentStep == 5);

        // Update content visibility
        step1Content.setVisible(currentStep == 1);
        step2Content.setVisible(currentStep == 2);
        step3Content.setVisible(currentStep == 3);
        step4Content.setVisible(currentStep == 4);
        step5Content.setVisible(currentStep == 5);

        // Update navigation buttons
        backBtn.setVisible(currentStep > 1 && currentStep < 5);
        
        switch (currentStep) {
            case 1:
                stepTitle.setText("Đăng ký thông tin");
                nextBtn.setVisible(true);
                nextBtn.setText(" TIẾP TỤC");
                nextBtn.setGraphic(createIcon(FontAwesomeSolid.ARROW_RIGHT, "white", 14));
                break;
            case 2:
                stepTitle.setText("Tạo mã PIN");
                nextBtn.setVisible(true);
                nextBtn.setText(" TIẾP TỤC");
                nextBtn.setGraphic(createIcon(FontAwesomeSolid.ARROW_RIGHT, "white", 14));
                break;
            case 3:
                stepTitle.setText("Chọn hình thức nạp");
                nextBtn.setVisible(true);
                nextBtn.setText(" TIẾP TỤC");
                nextBtn.setGraphic(createIcon(FontAwesomeSolid.ARROW_RIGHT, "white", 14));
                break;
            case 4:
                stepTitle.setText("Ghi thẻ");
                nextBtn.setVisible(false);
                // Reset card states
                cardWaitingState.setVisible(true);
                cardWritingState.setVisible(false);
                cardSuccessState.setVisible(false);
                cardErrorState.setVisible(false);
                writeCardBtn.setVisible(true);
                writeCardBtn.setDisable(false);
                writeCardBtn.setText(" BẮT ĐẦU GHI THẺ");
                writeCardBtn.setGraphic(createIcon(FontAwesomeSolid.LOCK, "white", 16));
                break;
            case 5:
                stepTitle.setText("Hoàn thành");
                nextBtn.setVisible(true);
                nextBtn.setText(" VỀ BƯỚC ĐẦU");
                nextBtn.setGraphic(createIcon(FontAwesomeSolid.REDO, "white", 16));
                backBtn.setVisible(false);
                // Show summary
                summaryName.setText(user.getName());
                summaryAge.setText(user.getAge());
                summaryCoins.setText(user.getCoins() + " coins");
                break;
        }
    }

    private void updateStepIndicator(Label stepLabel, Region line, boolean completed, boolean active) {
        stepLabel.getStyleClass().removeAll("step-active", "step-completed", "step-inactive");
        if (completed) {
            stepLabel.getStyleClass().add("step-completed");
            FontIcon checkIcon = createIcon(FontAwesomeSolid.CHECK, "white", 14);
            stepLabel.setGraphic(checkIcon);
            stepLabel.setText("");
        } else if (active) {
            stepLabel.getStyleClass().add("step-active");
        } else {
            stepLabel.getStyleClass().add("step-inactive");
        }

        if (line != null) {
            line.getStyleClass().remove("step-line-active");
            if (completed) {
                line.getStyleClass().add("step-line-active");
            }
        }
    }

    private void resetForm() {
        user = new UserRegistration();
        avatarBytes = null;
        selectedAmount = 0;
        selectedComboIds = null;
        selectedComboGameIds = null;
        loadingComboDetails = false;

        nameField.clear();
        ageField.clear();
        customAmountField.clear();
        
        // Reset PIN
        pinBuilder.setLength(0);
        updatePinDisplay();
        
        avatarImage.setImage(null);
        avatarPlaceholderText.setVisible(true);
        // Reset step indicators graphics
        step1Label.setGraphic(null);
        step1Label.setText("1");
        step2Label.setGraphic(null);
        step2Label.setText("2");
        step3Label.setGraphic(null);
        step3Label.setText("3");
        step4Label.setGraphic(null);
        step4Label.setText("4");
        step5Label.setGraphic(null);
        step5Label.setText("5");
        
        if (genderGroup.getSelectedToggle() != null) {
            genderGroup.getSelectedToggle().setSelected(false);
        }

        coinDisplay.setText("0");
        selectedAmountLabel.setText("");
        customAmountBox.setVisible(false);
        customAmountBox.setManaged(false);
    }

    /**
     * Navigate back to main menu
     */
    @FXML
    private void onGoHome() {
        MainApp.setRoot("main-menu.fxml");
    }

    private void showAlert(String title, String message) {
        UIUtils.showAlert(title, message);
    }

    private FontIcon createIcon(FontAwesomeSolid iconCode, String color, int size) {
        return UIUtils.createIcon(iconCode, color, size);
    }
}
