package com.example.desktopapp.controller;

import com.example.desktopapp.ClientApp;
import com.example.desktopapp.util.AppConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Controller for Game Selection Screen
 */
public class GameSelectionController {

    @FXML
    private FlowPane gamesContainer;

    private List<JSONObject> games = new ArrayList<>();
    private JSONObject selectedGame;

    @FXML
    public void initialize() {
        loadGames();
    }

    /**
     * Load games from API
     */
    private void loadGames() {
        
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
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray gamesArray = jsonResponse.getJSONArray("data");

                    Platform.runLater(() -> {
                        games.clear();
                        gamesContainer.getChildren().clear();
                        
                        for (int i = 0; i < gamesArray.length(); i++) {
                            JSONObject game = gamesArray.getJSONObject(i);
                            games.add(game);
                            addGameCard(game);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        showError("Không thể kết nối đến server. Mã lỗi: " + responseCode);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("Lỗi kết nối: " + e.getMessage());
                });
            }
        }).start();
    }

    /**
     * Add a game card to the UI
     */
    private void addGameCard(JSONObject game) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20, 18, 20, 18));
        card.getStyleClass().add("glass-panel");
        card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.95); " +
                     "-fx-border-color: rgba(99, 102, 241, 0.3); " +
                     "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 20, 0, 0, 10);");
        card.setPrefWidth(220);
        card.setPrefHeight(280);
        card.setCursor(Cursor.HAND);
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: rgba(51, 65, 85, 0.98); " +
                         "-fx-border-color: #6366f1; " +
                         "-fx-effect: dropshadow(gaussian, rgba(99, 102, 241, 0.6), 30, 0, 0, 12); " +
                         "-fx-scale-x: 1.02; -fx-scale-y: 1.02;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: rgba(30, 41, 59, 0.95); " +
                         "-fx-border-color: rgba(99, 102, 241, 0.3); " +
                         "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.5), 20, 0, 0, 10);");
        });

        // Game icon
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2)); " +
                              "-fx-background-radius: 50; " +
                              "-fx-min-width: 60; -fx-min-height: 60; -fx-max-width: 60; -fx-max-height: 60;");
        FontIcon gameIcon = new FontIcon("fas-gamepad");
        gameIcon.setIconSize(30);
        gameIcon.getStyleClass().add("icon-primary");
        iconContainer.getChildren().add(gameIcon);

        // Game name
        Label nameLabel = new Label(game.optString("name", "Unnamed Game"));
        nameLabel.getStyleClass().add("title-small");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(190);

        // Game description
        Label descLabel = new Label(game.optString("description", "No description"));
        descLabel.getStyleClass().add("subtitle");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxWidth(190);
        descLabel.setMaxHeight(45);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Price with coin icon
        int price = game.optInt("points", 0);
        HBox priceBox = new HBox(6);
        priceBox.setAlignment(Pos.CENTER);
        priceBox.setStyle("-fx-background-color: rgba(245, 158, 11, 0.15); " +
                         "-fx-background-radius: 12; -fx-padding: 6 12;");
        FontIcon coinIcon = new FontIcon("fas-coins");
        coinIcon.setIconSize(16);
        coinIcon.getStyleClass().add("icon-warning");
        Label priceLabel = new Label(price + " coins");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #fbbf24;");
        priceBox.getChildren().addAll(coinIcon, priceLabel);

        // Play button
        Button playButton = new Button();
        playButton.getStyleClass().addAll("btn-primary");
        playButton.setStyle("-fx-min-width: 160px; -fx-min-height: 42px; -fx-background-radius: 12; -fx-border-radius: 12;");
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);
        FontIcon playIcon = new FontIcon("fas-play");
        playIcon.setIconSize(14);
        playIcon.getStyleClass().add("icon-white");
        Label buttonText = new Label("CHƠI NGAY");
        buttonText.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        buttonContent.getChildren().addAll(playIcon, buttonText);
        playButton.setGraphic(buttonContent);
        playButton.setOnAction(e -> selectGame(game));

        card.getChildren().addAll(iconContainer, nameLabel, descLabel, spacer, priceBox, playButton);
        gamesContainer.getChildren().add(card);
    }

    /**
     * Select a game and proceed to card check
     */
    private void selectGame(JSONObject game) {
        selectedGame = game;
        // Store selected game info in session
        AppConfig.setProperty("selectedGameId", String.valueOf(game.optInt("_id")));
        AppConfig.setProperty("selectedGameName", game.optString("name"));
        AppConfig.setProperty("selectedGamePrice", String.valueOf(game.optInt("points")));
        
        // Navigate to card check screen
        ClientApp.setRoot("card-check-client.fxml");
    }

    /**
     * Show error dialog
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
