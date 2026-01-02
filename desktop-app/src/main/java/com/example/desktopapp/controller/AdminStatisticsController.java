package com.example.desktopapp.controller;

import com.example.desktopapp.AdminApp;
import com.example.desktopapp.util.AppConfig;
import com.example.desktopapp.util.UIUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controller for Admin Statistics Screen
 * Displays 3 main charts: Revenue by Month, Top Games/Combos, Revenue by Age Group
 */
public class AdminStatisticsController {

    @FXML private VBox loadingBox;
    @FXML private VBox contentBox;
    @FXML private ScrollPane contentScrollPane;
    @FXML private Label errorLabel;
    
    // Chart 1: Revenue by Month
    @FXML private BarChart<String, Number> revenueByMonthChart;
    @FXML private CategoryAxis monthAxis;
    @FXML private NumberAxis revenueAxis;
    @FXML private ComboBox<String> yearSelector;
    
    // Chart 2: Top Products (Games & Combos)
    @FXML private BarChart<String, Number> topProductsChart;
    @FXML private CategoryAxis productAxis;
    @FXML private NumberAxis countAxis;
    @FXML private ToggleButton showGamesBtn;
    @FXML private ToggleButton showCombosBtn;
    
    // Chart 3: Revenue by Age Group
    @FXML private BarChart<String, Number> revenueByAgeChart;
    @FXML private CategoryAxis ageAxis;
    @FXML private NumberAxis ageRevenueAxis;
    
    // Summary cards
    @FXML private Label totalRevenueLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label avgTransactionLabel;
    
    private NumberFormat currencyFormat;
    private ToggleGroup productToggleGroup;

    @FXML
    public void initialize() {
        currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
        
        // Setup year selector
        int currentYear = java.time.Year.now().getValue();
        for (int i = currentYear; i >= currentYear - 5; i--) {
            yearSelector.getItems().add(String.valueOf(i));
        }
        yearSelector.setValue(String.valueOf(currentYear));
        yearSelector.setOnAction(e -> loadRevenueByMonth());
        
        // Setup product toggle group
        productToggleGroup = new ToggleGroup();
        showGamesBtn.setToggleGroup(productToggleGroup);
        showCombosBtn.setToggleGroup(productToggleGroup);
        showGamesBtn.setSelected(true);
        
        showGamesBtn.setOnAction(e -> loadTopProducts());
        showCombosBtn.setOnAction(e -> loadTopProducts());
        
        // Configure charts
        configureCharts();
        
        // Load all data
        loadAllStatistics();
    }

    private void configureCharts() {
        // Chart 1: Revenue by Month
        revenueByMonthChart.setLegendVisible(false);
        revenueByMonthChart.setAnimated(true);
        monthAxis.setLabel("Tháng");
        revenueAxis.setLabel("Doanh thu (VNĐ)");
        
        // Chart 2: Top Products
        topProductsChart.setLegendVisible(false);
        topProductsChart.setAnimated(true);
        productAxis.setLabel("Sản phẩm");
        productAxis.setTickLabelRotation(45); // Xoay label 45 độ
        countAxis.setLabel("Số lượng bán");
        
        // Chart 3: Revenue by Age
        revenueByAgeChart.setLegendVisible(false);
        revenueByAgeChart.setAnimated(true);
        ageAxis.setLabel("Nhóm tuổi");
        ageAxis.setTickLabelRotation(45); // Xoay label 45 độ
        ageRevenueAxis.setLabel("Doanh thu (VNĐ)");
    }

    private void loadAllStatistics() {
        showLoading(true);
        errorLabel.setVisible(false);
        
        new Thread(() -> {
            try {
                loadSummary();
                loadRevenueByMonth();
                loadTopProducts();
                loadRevenueByAge();
                
                Platform.runLater(() -> {
                    showLoading(false);
                    errorLabel.setVisible(false);
                    contentScrollPane.setVisible(true);
                    contentScrollPane.setManaged(true);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showLoading(false);
                    showError("Lỗi tải dữ liệu: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadSummary() {
        try {
            String apiUrl = AppConfig.API_STATISTICS + "/summary";
            JSONObject response = fetchJSON(apiUrl);
            
            if (response.getBoolean("success")) {
                JSONObject data = response.getJSONObject("data");
                JSONObject total = data.getJSONObject("total");
                
                long totalRevenue = total.optLong("totalRevenue", 0);
                int totalTransactions = total.optInt("totalTransactions", 0);
                double avgTransaction = total.optDouble("avgTransaction", 0);
                
                Platform.runLater(() -> {
                    totalRevenueLabel.setText(currencyFormat.format(totalRevenue) + " VNĐ");
                    totalTransactionsLabel.setText(String.valueOf(totalTransactions));
                    avgTransactionLabel.setText(currencyFormat.format(avgTransaction) + " VNĐ");
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading summary: " + e.getMessage());
        }
    }

    private void loadRevenueByMonth() {
        new Thread(() -> {
            try {
                String year = yearSelector.getValue();
                String apiUrl = AppConfig.API_STATISTICS + "/revenue-by-month?year=" + year;
                JSONObject response = fetchJSON(apiUrl);
                
                if (response.getBoolean("success")) {
                    JSONArray data = response.getJSONArray("data");
                    
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Doanh thu");
                    
                    String[] monthNames = {"T1", "T2", "T3", "T4", "T5", "T6", 
                                          "T7", "T8", "T9", "T10", "T11", "T12"};
                    
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        int month = item.getInt("month");
                        long revenue = item.optLong("totalRevenue", 0);
                        
                        series.getData().add(new XYChart.Data<>(monthNames[month - 1], revenue));
                    }
                    
                    Platform.runLater(() -> {
                        revenueByMonthChart.getData().clear();
                        revenueByMonthChart.getData().add(series);
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading revenue by month: " + e.getMessage());
            }
        }).start();
    }

    private void loadTopProducts() {
        new Thread(() -> {
            try {
                boolean isGames = showGamesBtn.isSelected();
                String endpoint = isGames ? "/top-games" : "/top-combos";
                String apiUrl = AppConfig.API_STATISTICS + endpoint;
                
                JSONObject response = fetchJSON(apiUrl);
                
                if (response.getBoolean("success")) {
                    JSONArray data = response.getJSONArray("data");
                    
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName(isGames ? "Games" : "Combos");
                    
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        String name = item.optString(isGames ? "gameName" : "comboName", "Unknown");
                        int count = item.optInt("purchaseCount", 0);
                        
                        // Truncate long names
                        if (name.length() > 20) {
                            name = name.substring(0, 17) + "...";
                        }
                        
                        series.getData().add(new XYChart.Data<>(name, count));
                    }
                    
                    Platform.runLater(() -> {
                        topProductsChart.getData().clear();
                        topProductsChart.getData().add(series);
                        productAxis.setLabel(isGames ? "Game" : "Combo");
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading top products: " + e.getMessage());
            }
        }).start();
    }

    private void loadRevenueByAge() {
        new Thread(() -> {
            try {
                String apiUrl = AppConfig.API_STATISTICS + "/revenue-by-age";
                JSONObject response = fetchJSON(apiUrl);
                
                if (response.getBoolean("success")) {
                    JSONArray data = response.getJSONArray("data");
                    
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Doanh thu");
                    
                    // Sort data by age numerically
                    java.util.List<JSONObject> sortedData = new java.util.ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        sortedData.add(data.getJSONObject(i));
                    }
                    sortedData.sort((a, b) -> {
                        try {
                            int ageA = Integer.parseInt(a.getString("ageGroup"));
                            int ageB = Integer.parseInt(b.getString("ageGroup"));
                            return Integer.compare(ageA, ageB);
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                    
                    for (JSONObject item : sortedData) {
                        String ageGroup = item.optString("ageGroup", "Unknown");
                        long revenue = item.optLong("totalRevenue", 0);
                        
                        series.getData().add(new XYChart.Data<>(ageGroup, revenue));
                    }
                    
                    Platform.runLater(() -> {
                        revenueByAgeChart.getData().clear();
                        revenueByAgeChart.getData().add(series);
                    });
                }
            } catch (Exception e) {
                System.err.println("Error loading revenue by age: " + e.getMessage());
            }
        }).start();
    }

    private JSONObject fetchJSON(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new Exception("API returned error code: " + responseCode);
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String inputLine;
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return new JSONObject(response.toString());
    }

    private void showLoading(boolean show) {
        Platform.runLater(() -> {
            loadingBox.setVisible(show);
            loadingBox.setManaged(show);
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void onRefresh() {
        loadAllStatistics();
    }

    @FXML
    private void onBack() {
        AdminApp.setRoot("admin-menu.fxml");
    }
}
