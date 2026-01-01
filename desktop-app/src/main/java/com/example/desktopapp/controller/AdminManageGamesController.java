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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Controller for Admin Manage Games
 */
public class AdminManageGamesController {

    @FXML private VBox adminPinBox, gamesManagementBox, gamesList, loadingBox;
    @FXML private PasswordField adminPinField;
    @FXML private Label adminPinErrorLabel;

    private CardService cardService;
    private boolean adminVerified = false;

    @FXML
    public void initialize() {
        cardService = new CardService();
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
                cardService.connect();
                cardService.verifyAdminPin(adminPin);
                cardService.disconnect();

                adminVerified = true;

                Platform.runLater(() -> {
                    adminPinBox.setVisible(false);
                    adminPinBox.setManaged(false);
                    gamesManagementBox.setVisible(true);
                    gamesManagementBox.setManaged(true);
                    loadGames();
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

    private void loadGames() {
        showLoading(true);
        gamesList.getChildren().clear();

        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_GAMES;
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
                    JSONArray games = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        showLoading(false);
                        displayGames(games);
                    });

                } else {
                    Platform.runLater(() -> {
                        showLoading(false);
                        UIUtils.showError("Lỗi", "Không thể tải danh sách games", "Response code: " + responseCode);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showLoading(false);
                    UIUtils.showError("Lỗi", "Không thể tải danh sách games", e.getMessage());
                });
            }
        }).start();
    }

    private void displayGames(JSONArray games) {
        for (int i = 0; i < games.length(); i++) {
            JSONObject game = games.getJSONObject(i);
            gamesList.getChildren().add(createGameCard(game));
        }
    }

    private HBox createGameCard(JSONObject game) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); " +
                     "-fx-border-color: rgba(99, 102, 241, 0.3); " +
                     "-fx-border-radius: 8; -fx-background-radius: 8; " +
                     "-fx-padding: 15;");

        // Icon
        FontIcon icon = new FontIcon("fas-gamepad");
        icon.setIconSize(30);
        icon.setStyle("-fx-icon-color: #6366f1;");

        // Info
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(game.optString("name", "N/A"));
        nameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        Label descLabel = new Label(game.optString("description", ""));
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        descLabel.setWrapText(true);

        Label pointsLabel = new Label("Giá: " + game.optInt("points", 0) + " điểm");
        pointsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

        infoBox.getChildren().addAll(nameLabel, descLabel, pointsLabel);

        // Action buttons
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Sửa");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16;");
        editBtn.setOnAction(e -> onEditGame(game));

        Button deleteBtn = new Button("Xóa");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> onDeleteGame(game));

        actionsBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(icon, infoBox, actionsBox);
        return card;
    }

    @FXML
    private void onAddGame() {
        showGameDialog(null);
    }

    private void onEditGame(JSONObject game) {
        showGameDialog(game);
    }

    private void onDeleteGame(JSONObject game) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Xóa trò chơi: " + game.optString("name"));
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa trò chơi này?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteGame(game.optInt("_id"));
            }
        });
    }

    private void showGameDialog(JSONObject existingGame) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingGame == null ? "Thêm trò chơi mới" : "Sửa trò chơi");
        dialog.setHeaderText(existingGame == null ? "Nhập thông tin trò chơi" : "Cập nhật thông tin trò chơi");

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        nameField.setPromptText("Tên trò chơi");
        TextField pointsField = new TextField();
        pointsField.setPromptText("Giá (điểm)");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Mô tả");
        descArea.setPrefRowCount(3);

        int rowIndex = 0;
        
        if (existingGame != null) {
            // Hiển thị ID khi sửa (read-only)
            idField.setText(String.valueOf(existingGame.optInt("_id")));
            idField.setDisable(true);
            idField.setPromptText("ID (tự động)");
            nameField.setText(existingGame.optString("name"));
            pointsField.setText(String.valueOf(existingGame.optInt("points")));
            descArea.setText(existingGame.optString("description"));
            
            grid.add(new Label("ID:"), 0, rowIndex);
            grid.add(idField, 1, rowIndex);
            rowIndex++;
        }
        // Khi thêm mới: không hiển thị trường ID (sẽ tự động sinh)

        grid.add(new Label("Tên:"), 0, rowIndex);
        grid.add(nameField, 1, rowIndex++);
        grid.add(new Label("Giá (điểm):"), 0, rowIndex);
        grid.add(pointsField, 1, rowIndex++);
        grid.add(new Label("Mô tả:"), 0, rowIndex);
        grid.add(descArea, 1, rowIndex);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String name = nameField.getText();
                    int points = Integer.parseInt(pointsField.getText());
                    String description = descArea.getText();

                    if (existingGame == null) {
                        createGame(name, points, description);
                    } else {
                        int id = Integer.parseInt(idField.getText());
                        updateGame(id, name, points, description);
                    }
                } catch (NumberFormatException e) {
                    UIUtils.showError("Lỗi", "Dữ liệu không hợp lệ", "Giá phải là số nguyên");
                }
            }
        });
    }

    private void createGame(String name, int points, String description) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_GAMES;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject gameData = new JSONObject();
                // Không gửi _id, để backend tự sinh
                gameData.put("name", name);
                gameData.put("points", points);
                gameData.put("description", description);

                OutputStream os = conn.getOutputStream();
                os.write(gameData.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Thêm trò chơi thành công", "");
                        loadGames();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể thêm trò chơi", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể thêm trò chơi", e.getMessage());
                });
            }
        }).start();
    }

    private void updateGame(int id, String name, int points, String description) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_GAMES + "/" + id;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject gameData = new JSONObject();
                gameData.put("name", name);
                gameData.put("points", points);
                gameData.put("description", description);

                OutputStream os = conn.getOutputStream();
                os.write(gameData.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Cập nhật trò chơi thành công", "");
                        loadGames();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể cập nhật trò chơi", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể cập nhật trò chơi", e.getMessage());
                });
            }
        }).start();
    }

    private void deleteGame(int id) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_GAMES + "/" + id;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Xóa trò chơi thành công", "");
                        loadGames();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể xóa trò chơi", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể xóa trò chơi", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onRefresh() {
        loadGames();
    }

    private void showLoading(boolean show) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
    }

    private void showAdminPinError(String message) {
        adminPinErrorLabel.setText(message);
        adminPinErrorLabel.setVisible(true);
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
