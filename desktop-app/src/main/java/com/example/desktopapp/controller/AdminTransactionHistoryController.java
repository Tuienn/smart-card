package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import com.example.desktopapp.service.CardService;
import com.example.desktopapp.util.AppConfig;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.smartcardio.CardException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Controller for Admin Transaction History
 */
public class AdminTransactionHistoryController {

    @FXML private TextField cardIdField;
    @FXML private PasswordField adminPinField;
    @FXML private Label cardIdStatus, errorLabel, totalLabel;
    @FXML private VBox transactionContainer, transactionList, loadingBox, emptyBox;

    private CardService cardService;
    private boolean adminVerified = false;
    private NumberFormat currencyFormat;
    private SimpleDateFormat dateFormat;

    @FXML
    public void initialize() {
        cardService = new CardService();
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    }

    @FXML
    private void onReadCardId() {
        cardIdStatus.setText("Đang đọc thẻ...");
        cardIdStatus.setStyle("-fx-text-fill: #94a3b8;");
        cardIdStatus.setVisible(true);

        new Thread(() -> {
            try {
                cardService.connect();
                byte[] userIdBytes = cardService.readUserId();
                String userId = bytesToHex(userIdBytes);
                cardService.disconnect();

                Platform.runLater(() -> {
                    cardIdField.setText(userId);
                    cardIdStatus.setText("✓ Đọc thẻ thành công");
                    cardIdStatus.setStyle("-fx-text-fill: #22c55e;");
                });

            } catch (CardException e) {
                Platform.runLater(() -> {
                    cardIdStatus.setText("Lỗi: " + e.getMessage());
                    cardIdStatus.setStyle("-fx-text-fill: #ef4444;");
                    UIUtils.showError("Lỗi", "Không thể đọc thẻ", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onViewHistory() {
        String cardId = cardIdField.getText().trim();
        String adminPin = adminPinField.getText();

        if (cardId.isEmpty()) {
            showError("Vui lòng nhập Card ID hoặc đọc từ thẻ");
            return;
        }

        if (adminPin.isEmpty()) {
            showError("Vui lòng nhập Admin PIN");
            return;
        }

        // Verify admin PIN if not yet verified
        if (!adminVerified) {
            verifyAdminPinAndLoadHistory(cardId, adminPin);
        } else {
            loadTransactionHistory(cardId);
        }
    }

    private void verifyAdminPinAndLoadHistory(String cardId, String adminPin) {
        errorLabel.setVisible(false);
        showLoading(true);

        new Thread(() -> {
            try {
                // Connect and verify admin PIN
                cardService.connect();
                cardService.verifyAdminPin(adminPin);
                cardService.disconnect();

                adminVerified = true;

                Platform.runLater(() -> {
                    loadTransactionHistory(cardId);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Admin PIN sai: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadTransactionHistory(String cardId) {
        showLoading(true);
        transactionContainer.setVisible(false);
        transactionContainer.setManaged(false);
        emptyBox.setVisible(false);
        emptyBox.setManaged(false);

        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_BASE_URL + "/api/transactions/card/" + cardId;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray transactions = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        showLoading(false);
                        displayTransactions(transactions);
                    });

                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showEmpty();
                    });
                } else {
                    Platform.runLater(() -> {
                        showLoading(false);
                        showError("Lỗi kết nối server: " + responseCode);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi: " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayTransactions(JSONArray transactions) {
        transactionList.getChildren().clear();

        if (transactions.length() == 0) {
            showEmpty();
            return;
        }

        double total = 0;

        for (int i = 0; i < transactions.length(); i++) {
            JSONObject transaction = transactions.getJSONObject(i);
            transactionList.getChildren().add(createTransactionCard(transaction));
            total += transaction.optDouble("payment", 0);
        }

        totalLabel.setText("Tổng cộng: " + currencyFormat.format(total) + " VNĐ (" + transactions.length() + " giao dịch)");
        
        transactionContainer.setVisible(true);
        transactionContainer.setManaged(true);
    }

    private VBox createTransactionCard(JSONObject transaction) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); " +
                     "-fx-border-color: rgba(99, 102, 241, 0.3); " +
                     "-fx-border-radius: 8; -fx-background-radius: 8; " +
                     "-fx-padding: 15;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        FontIcon icon = new FontIcon("fas-receipt");
        icon.setIconSize(20);
        icon.setStyle("-fx-icon-color: #6366f1;");

        String date = transaction.optString("time_stamp", "N/A");
        Label dateLabel = new Label(formatDate(date));
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1; -fx-font-weight: bold;");

        header.getChildren().addAll(icon, dateLabel);

        // Payment amount
        double payment = transaction.optDouble("payment", 0);
        Label amountLabel = new Label(currencyFormat.format(payment) + " VNĐ");
        amountLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

        // Combo info (if exists)
        VBox infoBox = new VBox(5);
        if (transaction.has("combo_id") && !transaction.isNull("combo_id")) {
            JSONObject combo = transaction.optJSONObject("combo_id");
            if (combo != null) {
                Label comboLabel = new Label("📦 Combo: " + combo.optString("name", "N/A"));
                comboLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
                infoBox.getChildren().add(comboLabel);
            }
        } else {
            Label typeLabel = new Label("💰 Nạp tiền");
            typeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            infoBox.getChildren().add(typeLabel);
        }

        card.getChildren().addAll(header, amountLabel, infoBox);
        return card;
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date date = isoFormat.parse(isoDate);
            return dateFormat.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private void showLoading(boolean show) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
    }

    private void showEmpty() {
        emptyBox.setVisible(true);
        emptyBox.setManaged(true);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
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
}
