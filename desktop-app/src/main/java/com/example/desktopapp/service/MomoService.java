package com.example.desktopapp.service;

import com.example.desktopapp.util.AppConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service for MoMo payment integration
 * Handles QR code generation and payment status polling
 */
public class MomoService {

    /**
     * Response from creating QR payment
     */
    public static class QrPaymentResponse {
        public String qrCodeUrl;
        public String orderId;
        public String qrData;
        public int resultCode;
        public String message;

        @Override
        public String toString() {
            return "QrPaymentResponse{orderId='" + orderId + "', resultCode=" + resultCode + ", message='" + message + "'}";
        }
    }

    /**
     * Response from checking payment status
     */
    public static class PaymentStatusResponse {
        public String orderId;
        public String status; // "pending", "success", "failed"
        public String amount;
        public String description;
        public String transId;
        public String message;

        public boolean isSuccess() {
            return "success".equals(status);
        }

        public boolean isPending() {
            return "pending".equals(status);
        }

        @Override
        public String toString() {
            return "PaymentStatusResponse{orderId='" + orderId + "', status='" + status + "', message='" + message + "'}";
        }
    }

    /**
     * Create QR payment with MoMo
     * @param amount Amount in VND
     * @param description Payment description (e.g., user ID)
     * @return QR payment response with QR code URL and order ID
     */
    public QrPaymentResponse createQrPayment(int amount, String description) throws Exception {
        URL url = new URL(AppConfig.API_MOMO_QR);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(AppConfig.READ_TIMEOUT);

            // Build JSON request body
            String jsonBody = String.format(
                "{\"amount\":\"%d\",\"orderInfo\":\"Nap tien\",\"description\":\"%s\"}",
                amount, description
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String jsonResponse = response.toString();
            System.out.println("MoMo QR Response: " + jsonResponse);

            return parseQrPaymentResponse(jsonResponse);

        } finally {
            conn.disconnect();
        }
    }

    /**
     * Check payment status by order ID
     * @param orderId Order ID to check
     * @return Payment status response
     */
    public PaymentStatusResponse checkPaymentStatus(String orderId) throws Exception {
        URL url = new URL(AppConfig.API_MOMO_STATUS + "/" + orderId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(AppConfig.READ_TIMEOUT);

            int responseCode = conn.getResponseCode();
            
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                        responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                        StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            String jsonResponse = response.toString();
            System.out.println("MoMo Status Response: " + jsonResponse);

            return parsePaymentStatusResponse(jsonResponse);

        } finally {
            conn.disconnect();
        }
    }

    private QrPaymentResponse parseQrPaymentResponse(String json) {
        QrPaymentResponse response = new QrPaymentResponse();
        response.qrCodeUrl = parseJsonString(json, "qrCodeUrl");
        response.orderId = parseJsonString(json, "orderId");
        response.qrData = parseJsonString(json, "qrData");
        response.resultCode = parseJsonInt(json, "resultCode");
        response.message = parseJsonString(json, "message");
        return response;
    }

    private PaymentStatusResponse parsePaymentStatusResponse(String json) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.orderId = parseJsonString(json, "orderId");
        response.status = parseJsonString(json, "status");
        response.amount = parseJsonString(json, "amount");
        response.description = parseJsonString(json, "description");
        response.transId = parseJsonString(json, "transId");
        response.message = parseJsonString(json, "message");
        return response;
    }

    private String parseJsonString(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        
        start += pattern.length();
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        if (start >= json.length()) return null;
        
        // Check for null
        if (json.substring(start).startsWith("null")) {
            return null;
        }
        
        // Check for string value
        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf('"', start);
            if (end == -1) return null;
            return json.substring(start, end);
        }
        
        return null;
    }

    private int parseJsonInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return -1;
        
        start += pattern.length();
        // Skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        
        StringBuilder sb = new StringBuilder();
        while (start < json.length() && (Character.isDigit(json.charAt(start)) || json.charAt(start) == '-')) {
            sb.append(json.charAt(start));
            start++;
        }
        
        if (sb.length() == 0) return -1;
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
