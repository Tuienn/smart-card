package com.example.desktopapp.controller;

import com.example.desktopapp.model.UserRegistration;
import com.example.desktopapp.service.CardService;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for Card Registration wizard
 */
public class CardRegistrationController implements Initializable {

    // Step indicators
    @FXML private Label step1Label, step2Label, step3Label, step4Label;
    @FXML private Region line1, line2, line3;
    @FXML private Label stepTitle;

    // Content panels
    @FXML private StackPane contentStack;
    @FXML private VBox step1Content, step2Content, step3Content, step4Content;

    // Step 1: User Info
    @FXML private StackPane avatarContainer;
    @FXML private ImageView avatarImage;
    @FXML private Label avatarPlaceholderText;
    @FXML private TextField nameField, phoneField;
    @FXML private PasswordField pinField, confirmPinField;
    @FXML private ToggleButton maleBtn, femaleBtn;

    // Step 2: Payment
    @FXML private VBox customAmountBox;
    @FXML private TextField customAmountField;
    @FXML private Label coinDisplay, selectedAmountLabel;

    // Step 3: Card Writing
    @FXML private VBox cardWaitingState, cardWritingState, cardSuccessState, cardErrorState;
    @FXML private ProgressIndicator writeProgress;
    @FXML private Label writeStatusLabel, errorMessageLabel;
    @FXML private Button writeCardBtn;

    // Step 4: Success
    @FXML private Label summaryName, summaryPhone, summaryCoins;

    // Navigation
    @FXML private Button backBtn, nextBtn;

    // State
    private int currentStep = 1;
    private UserRegistration user = new UserRegistration();
    private byte[] avatarBytes = null;
    private int selectedAmount = 0;
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

        // Initialize UI
        updateStepUI();
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
                
                // Resize if too large (max 8KB for smart card)
                if (avatarBytes.length > 8192) {
                    showAlert("Cảnh báo", "Ảnh quá lớn. Vui lòng chọn ảnh nhỏ hơn 8KB.");
                    avatarBytes = null;
                    return;
                }

                Image image = new Image(new ByteArrayInputStream(avatarBytes));
                avatarImage.setImage(image);
                avatarPlaceholderText.setVisible(false);
            } catch (IOException e) {
                showAlert("Lỗi", "Không thể đọc file ảnh: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onGenderSelect(ActionEvent event) {
        ToggleButton selected = (ToggleButton) event.getSource();
        byte gender = Byte.parseByte((String) selected.getUserData());
        user.setGender(gender);
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

                updateMessage("Đang khởi tạo thẻ...");
                user.generateUserId();
                cardService.installCard(user);

                updateMessage("Đang xác thực...");
                cardService.verifyPin(user.getPin());

                updateMessage("Đang ghi thông tin người dùng...");
                cardService.writeUserData(user);

                if (user.getCoins() > 0) {
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
                            Platform.runLater(() -> goToStep(4));
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
                    writeCardBtn.setText("🔄 THỬ LẠI");
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
        if (currentStep == 4) {
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
        if (currentStep < 4) {
            goToStep(currentStep + 1);
        }
    }

    private boolean validateCurrentStep() {
        switch (currentStep) {
            case 1:
                // Validate user info
                String name = nameField.getText().trim();
                String phone = phoneField.getText().trim();
                String pin = pinField.getText();
                String confirmPin = confirmPinField.getText();

                if (name.isEmpty()) {
                    showAlert("Lỗi", "Vui lòng nhập họ tên");
                    nameField.requestFocus();
                    return false;
                }
                if (phone.isEmpty()) {
                    showAlert("Lỗi", "Vui lòng nhập số điện thoại");
                    phoneField.requestFocus();
                    return false;
                }
                if (!phone.matches("\\d{10,11}")) {
                    showAlert("Lỗi", "Số điện thoại không hợp lệ (10-11 số)");
                    phoneField.requestFocus();
                    return false;
                }
                if (pin.isEmpty() || pin.length() < 4 || pin.length() > 6) {
                    showAlert("Lỗi", "Mã PIN phải từ 4-6 ký tự");
                    pinField.requestFocus();
                    return false;
                }
                if (!pin.equals(confirmPin)) {
                    showAlert("Lỗi", "Mã PIN xác nhận không khớp");
                    confirmPinField.requestFocus();
                    return false;
                }

                // Save user data
                user.setName(name);
                user.setPhone(phone);
                user.setPin(pin);
                user.setAvatar(avatarBytes);
                return true;

            case 2:
                // Validate payment
                updateCoinDisplay();
                if (user.getAmountVND() <= 0) {
                    showAlert("Lỗi", "Vui lòng chọn số tiền nạp");
                    return false;
                }
                if (user.getAmountVND() < 10000) {
                    showAlert("Lỗi", "Số tiền tối thiểu là 10,000đ");
                    return false;
                }
                return true;

            case 3:
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
        updateStepIndicator(step4Label, null, false, currentStep == 4);

        // Update content visibility
        step1Content.setVisible(currentStep == 1);
        step2Content.setVisible(currentStep == 2);
        step3Content.setVisible(currentStep == 3);
        step4Content.setVisible(currentStep == 4);

        // Update navigation buttons
        backBtn.setVisible(currentStep > 1 && currentStep < 4);
        
        switch (currentStep) {
            case 1:
                stepTitle.setText("Đăng Ký Thông Tin");
                nextBtn.setText("TIẾP TỤC ➡");
                break;
            case 2:
                stepTitle.setText("Chọn Gói Nạp Tiền");
                nextBtn.setText("TIẾP TỤC ➡");
                break;
            case 3:
                stepTitle.setText("Ghi Thẻ");
                nextBtn.setVisible(false);
                // Reset card states
                cardWaitingState.setVisible(true);
                cardWritingState.setVisible(false);
                cardSuccessState.setVisible(false);
                cardErrorState.setVisible(false);
                writeCardBtn.setVisible(true);
                writeCardBtn.setDisable(false);
                writeCardBtn.setText("🔐 BẮT ĐẦU GHI THẺ");
                break;
            case 4:
                stepTitle.setText("Hoàn Thành");
                nextBtn.setVisible(true);
                nextBtn.setText("🏠 VỀ TRANG CHỦ");
                backBtn.setVisible(false);
                // Show summary
                summaryName.setText(user.getName());
                summaryPhone.setText(user.getPhone());
                summaryCoins.setText(user.getCoins() + " coins");
                break;
        }
    }

    private void updateStepIndicator(Label stepLabel, Region line, boolean completed, boolean active) {
        stepLabel.getStyleClass().removeAll("step-active", "step-completed", "step-inactive");
        if (completed) {
            stepLabel.getStyleClass().add("step-completed");
            stepLabel.setText("✓");
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

        nameField.clear();
        phoneField.clear();
        pinField.clear();
        confirmPinField.clear();
        customAmountField.clear();
        
        avatarImage.setImage(null);
        avatarPlaceholderText.setVisible(true);
        
        if (genderGroup.getSelectedToggle() != null) {
            genderGroup.getSelectedToggle().setSelected(false);
        }

        coinDisplay.setText("0");
        selectedAmountLabel.setText("");
        customAmountBox.setVisible(false);
        customAmountBox.setManaged(false);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
