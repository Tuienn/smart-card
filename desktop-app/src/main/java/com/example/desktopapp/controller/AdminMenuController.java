package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Controller for Admin Menu Screen
 */
public class AdminMenuController {

    @FXML
    private FlowPane functionsContainer;

    @FXML
    public void initialize() {
        loadAdminFunctions();
    }

    /**
     * Load admin functions as cards
     */
    private void loadAdminFunctions() {
        // Define admin functions
        AdminFunction[] functions = {
            new AdminFunction(
                "fas-unlock",
                "Mở khóa thẻ",
                "Mở khóa thẻ người dùng bị khóa do nhập sai PIN",
                "admin-unlock-card.fxml"
            ),
            new AdminFunction(
                "fas-key",
                "Đổi mật khẩu thẻ",
                "Thay đổi PIN cho thẻ người dùng",
                "admin-change-pin.fxml"
            ),
            new AdminFunction(
                "fas-sync-alt",
                "Reset thẻ",
                "Reset thẻ về trạng thái ban đầu (Xóa toàn bộ dữ liệu)",
                "admin-reset-card.fxml"
            ),
            new AdminFunction(
                "fas-history",
                "Lịch sử giao dịch",
                "Xem lịch sử giao dịch của thẻ người dùng",
                "admin-transaction-history.fxml"
            ),
            new AdminFunction(
                "fas-info-circle",
                "Thông tin thẻ",
                "Xem thông tin chi tiết của thẻ người dùng",
                "admin-card-info.fxml"
            ),
            new AdminFunction(
                "fas-coins",
                "Quản lý số dư",
                "Nạp tiền hoặc điều chỉnh số dư cho thẻ",
                "admin-manage-balance.fxml"
            ),
            new AdminFunction(
                "fas-gamepad",
                "Quản lý trò chơi",
                "Thêm, sửa, xóa trò chơi trong hệ thống",
                "admin-manage-games.fxml"
            )
        };

        for (AdminFunction function : functions) {
            functionsContainer.getChildren().add(createFunctionCard(function));
        }
    }

    /**
     * Create a function card
     */
    private VBox createFunctionCard(AdminFunction function) {
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

        // Function icon
        StackPane iconContainer = new StackPane();
        iconContainer.setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(99, 102, 241, 0.2), rgba(139, 92, 246, 0.2)); " +
                              "-fx-background-radius: 50; " +
                              "-fx-min-width: 60; -fx-min-height: 60; -fx-max-width: 60; -fx-max-height: 60;");
        FontIcon functionIcon = new FontIcon(function.icon);
        functionIcon.setIconSize(30);
        functionIcon.getStyleClass().add("icon-primary");
        iconContainer.getChildren().add(functionIcon);

        // Function name
        Label nameLabel = new Label(function.name);
        nameLabel.getStyleClass().add("title-small");
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setMaxWidth(190);

        // Function description
        Label descLabel = new Label(function.description);
        descLabel.getStyleClass().add("subtitle");
        descLabel.setWrapText(true);
        descLabel.setTextAlignment(TextAlignment.CENTER);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxWidth(190);
        descLabel.setMaxHeight(60);
        descLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Click handler
        card.setOnMouseClicked(e -> {
            AdminApp.setRoot(function.fxmlFile);
        });

        card.getChildren().addAll(iconContainer, nameLabel, descLabel, spacer);
        return card;
    }

    /**
     * Admin Function data class
     */
    private static class AdminFunction {
        String icon;
        String name;
        String description;
        String fxmlFile;

        AdminFunction(String icon, String name, String description, String fxmlFile) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.fxmlFile = fxmlFile;
        }
    }
}
