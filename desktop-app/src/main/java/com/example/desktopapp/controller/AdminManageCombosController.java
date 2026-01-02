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
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controller for Admin Manage Combos
 */
public class AdminManageCombosController {

    @FXML private VBox combosManagementBox, combosList, loadingBox;

    private CardService cardService;
    private NumberFormat currencyFormat;
    private JSONArray allGames; // Lưu danh sách games để dùng khi hiển thị

    @FXML
    public void initialize() {
        cardService = new CardService();
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        // Tự động load combos khi mở trang
        loadGamesFirst(); // Load games trước để có thể hiển thị tên
    }
    
    private void loadGamesFirst() {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_GAMES;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/json");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    allGames = jsonResponse.getJSONArray("data");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.runLater(() -> loadCombos());
            }
        }).start();
    }

    private void loadCombos() {
        showLoading(true);
        combosList.getChildren().clear();

        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_COMBOS;
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
                    JSONArray combos = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        showLoading(false);
                        displayCombos(combos);
                    });

                } else {
                    Platform.runLater(() -> {
                        showLoading(false);
                        UIUtils.showError("Lỗi", "Không thể tải danh sách combo", "Response code: " + responseCode);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showLoading(false);
                    UIUtils.showError("Lỗi", "Không thể tải danh sách combo", e.getMessage());
                });
            }
        }).start();
    }

    private void displayCombos(JSONArray combos) {
        for (int i = 0; i < combos.length(); i++) {
            JSONObject combo = combos.getJSONObject(i);
            combosList.getChildren().add(createComboCard(combo));
        }
    }

    private HBox createComboCard(JSONObject combo) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.8); " +
                     "-fx-border-color: rgba(99, 102, 241, 0.3); " +
                     "-fx-border-radius: 8; -fx-background-radius: 8; " +
                     "-fx-padding: 15;");

        // Icon
        FontIcon icon = new FontIcon("fas-box-open");
        icon.setIconSize(30);
        icon.setStyle("-fx-icon-color: #8b5cf6;");

        // Info
        VBox infoBox = new VBox(5);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(combo.optString("name", "N/A"));
        nameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #f8fafc; -fx-font-weight: bold;");

        Label descLabel = new Label(combo.optString("description", ""));
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        descLabel.setWrapText(true);

        int priceVND = combo.optInt("priceVND", 0);
        int discount = combo.optInt("discountPercentage", 0);
        Label priceLabel = new Label("Giá: " + currencyFormat.format(priceVND) + " VNĐ (Giảm " + discount + "%)");
        priceLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #22c55e; -fx-font-weight: bold;");

        // Hiển thị tên games thay vì IDs
        JSONArray gameIds = combo.optJSONArray("game_ids");
        StringBuilder gamesText = new StringBuilder("Trò chơi: ");
        if (gameIds != null && gameIds.length() > 0) {
            for (int i = 0; i < gameIds.length(); i++) {
                if (i > 0) gamesText.append(", ");
                
                // Xử lý cả trường hợp game_ids là số hoặc object
                Object gameItem = gameIds.get(i);
                if (gameItem instanceof Integer) {
                    // Trường hợp game_ids là array of integers
                    int gameId = (Integer) gameItem;
                    gamesText.append(getGameNameById(gameId));
                } else if (gameItem instanceof JSONObject) {
                    // Trường hợp game_ids đã được populate thành objects
                    JSONObject gameObj = (JSONObject) gameItem;
                    gamesText.append(gameObj.optString("name", "N/A"));
                } else {
                    gamesText.append("N/A");
                }
            }
        } else {
            gamesText.append("N/A");
        }
        Label gameNamesLabel = new Label(gamesText.toString());
        gameNamesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");
        gameNamesLabel.setWrapText(true);

        infoBox.getChildren().addAll(nameLabel, descLabel, priceLabel, gameNamesLabel);

        // Action buttons
        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("Sửa");
        editBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16;");
        editBtn.setOnAction(e -> onEditCombo(combo));

        Button deleteBtn = new Button("Xóa");
        deleteBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 8 16;");
        deleteBtn.setOnAction(e -> onDeleteCombo(combo));

        actionsBox.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(icon, infoBox, actionsBox);
        return card;
    }
    
    private String getGameNameById(int gameId) {
        if (allGames == null) return "Game #" + gameId;
        
        for (int i = 0; i < allGames.length(); i++) {
            JSONObject game = allGames.getJSONObject(i);
            if (game.optInt("_id") == gameId) {
                return game.optString("name", "Game #" + gameId);
            }
        }
        return "Game #" + gameId;
    }

    @FXML
    private void onAddCombo() {
        showComboDialog(null);
    }

    private void onEditCombo(JSONObject combo) {
        showComboDialog(combo);
    }

    private void onDeleteCombo(JSONObject combo) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Xóa combo: " + combo.optString("name"));
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa combo này?");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteCombo(combo.optString("_id"));
            }
        });
    }

    private void showComboDialog(JSONObject existingCombo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existingCombo == null ? "Thêm combo mới" : "Sửa combo");
        dialog.setHeaderText(existingCombo == null ? "Nhập thông tin combo" : "Cập nhật thông tin combo");

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField idField = new TextField();
        TextField nameField = new TextField();
        nameField.setPromptText("Tên combo");
        TextField priceField = new TextField();
        priceField.setPromptText("Giá (VNĐ)");
        TextField discountField = new TextField();
        discountField.setPromptText("% Giảm giá");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Mô tả");
        descArea.setPrefRowCount(3);
        
        // Game selection checkboxes
        VBox gamesBox = new VBox(5);
        gamesBox.setStyle("-fx-padding: 5; -fx-border-color: #cbd5e1; -fx-border-radius: 5; -fx-max-height: 200;");
        ScrollPane gamesScrollPane = new ScrollPane(gamesBox);
        gamesScrollPane.setFitToWidth(true);
        gamesScrollPane.setPrefHeight(150);
        
        // Get existing selected game IDs
        JSONArray existingGameIds = existingCombo != null ? existingCombo.optJSONArray("game_ids") : null;
        
        // Create checkboxes for each game
        if (allGames != null) {
            for (int i = 0; i < allGames.length(); i++) {
                JSONObject game = allGames.getJSONObject(i);
                int gameId = game.optInt("_id");
                String gameName = game.optString("name");
                int gamePoints = game.optInt("points");
                
                CheckBox cb = new CheckBox(gameName + " (" + gamePoints + " điểm)");
                cb.setUserData(gameId);
                
                // Check if this game is already selected
                if (existingGameIds != null) {
                    for (int j = 0; j < existingGameIds.length(); j++) {
                        Object gameItem = existingGameIds.get(j);
                        int existingGameId;
                        
                        // Xử lý cả trường hợp integer hoặc object
                        if (gameItem instanceof Integer) {
                            existingGameId = (Integer) gameItem;
                        } else if (gameItem instanceof JSONObject) {
                            existingGameId = ((JSONObject) gameItem).optInt("_id");
                        } else {
                            continue;
                        }
                        
                        if (existingGameId == gameId) {
                            cb.setSelected(true);
                            break;
                        }
                    }
                }
                
                gamesBox.getChildren().add(cb);
            }
        }

        int rowIndex = 0;
        
        if (existingCombo != null) {
            // Hiển thị ID khi sửa (read-only)
            idField.setText(existingCombo.optString("_id"));
            idField.setDisable(true);
            nameField.setText(existingCombo.optString("name"));
            priceField.setText(String.valueOf(existingCombo.optInt("priceVND")));
            discountField.setText(String.valueOf(existingCombo.optInt("discountPercentage")));
            descArea.setText(existingCombo.optString("description"));
            
            grid.add(new Label("ID:"), 0, rowIndex);
            grid.add(idField, 1, rowIndex);
            rowIndex++;
        }

        grid.add(new Label("Tên:"), 0, rowIndex);
        grid.add(nameField, 1, rowIndex++);
        grid.add(new Label("Giá (VNĐ):"), 0, rowIndex);
        grid.add(priceField, 1, rowIndex++);
        grid.add(new Label("% Giảm giá:"), 0, rowIndex);
        grid.add(discountField, 1, rowIndex++);
        grid.add(new Label("Chọn trò chơi:"), 0, rowIndex);
        grid.add(gamesScrollPane, 1, rowIndex++);
        grid.add(new Label("Mô tả:"), 0, rowIndex);
        grid.add(descArea, 1, rowIndex);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    int price = Integer.parseInt(priceField.getText());
                    int discount = Integer.parseInt(discountField.getText());
                    String description = descArea.getText().trim();
                    
                    // Validation
                    if (name.isEmpty()) {
                        UIUtils.showError("Lỗi", "Tên combo không được để trống", "");
                        return;
                    }
                    if (price <= 0) {
                        UIUtils.showError("Lỗi", "Giá phải lớn hơn 0", "");
                        return;
                    }
                    if (discount < 0 || discount > 100) {
                        UIUtils.showError("Lỗi", "% Giảm giá phải từ 0-100", "");
                        return;
                    }
                    
                    // Set default description if empty
                    if (description.isEmpty()) {
                        description = "Combo " + name;
                    }
                    
                    // Collect selected game IDs from checkboxes
                    JSONArray gameIdsArray = new JSONArray();
                    for (var node : gamesBox.getChildren()) {
                        if (node instanceof CheckBox) {
                            CheckBox cb = (CheckBox) node;
                            if (cb.isSelected()) {
                                gameIdsArray.put((int) cb.getUserData());
                            }
                        }
                    }
                    
                    if (gameIdsArray.length() == 0) {
                        UIUtils.showError("Lỗi", "Vui lòng chọn ít nhất 1 trò chơi", "");
                        return;
                    }

                    if (existingCombo == null) {
                        createCombo(name, price, discount, description, gameIdsArray);
                    } else {
                        String id = idField.getText();
                        updateCombo(id, name, price, discount, description, gameIdsArray);
                    }
                } catch (NumberFormatException e) {
                    UIUtils.showError("Lỗi", "Dữ liệu không hợp lệ", "Giá và % giảm giá phải là số");
                }
            }
        });
    }

    private void createCombo(String name, int priceVND, int discountPercentage, String description, JSONArray gameIds) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_COMBOS;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject comboData = new JSONObject();
                comboData.put("name", name);
                comboData.put("priceVND", priceVND);
                comboData.put("discountPercentage", discountPercentage);
                comboData.put("description", description);
                comboData.put("game_ids", gameIds);

                OutputStream os = conn.getOutputStream();
                os.write(comboData.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Thêm combo thành công", "");
                        loadCombos();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể thêm combo", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể thêm combo", e.getMessage());
                });
            }
        }).start();
    }

    private void updateCombo(String id, String name, int priceVND, int discountPercentage, String description, JSONArray gameIds) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_COMBOS + "/" + id;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject comboData = new JSONObject();
                comboData.put("name", name);
                comboData.put("priceVND", priceVND);
                comboData.put("discountPercentage", discountPercentage);
                comboData.put("description", description);
                comboData.put("game_ids", gameIds);

                OutputStream os = conn.getOutputStream();
                os.write(comboData.toString().getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Cập nhật combo thành công", "");
                        loadCombos();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể cập nhật combo", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể cập nhật combo", e.getMessage());
                });
            }
        }).start();
    }

    private void deleteCombo(String id) {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_COMBOS + "/" + id;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("DELETE");

                int responseCode = conn.getResponseCode();
                
                Platform.runLater(() -> {
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        UIUtils.showSuccess("Thành công", "Xóa combo thành công", "");
                        loadCombos();
                    } else {
                        UIUtils.showError("Lỗi", "Không thể xóa combo", "Response code: " + responseCode);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    UIUtils.showError("Lỗi", "Không thể xóa combo", e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void onRefresh() {
        loadGamesFirst(); // Reload cả games và combos
    }

    private void showLoading(boolean show) {
        loadingBox.setVisible(show);
        loadingBox.setManaged(show);
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
