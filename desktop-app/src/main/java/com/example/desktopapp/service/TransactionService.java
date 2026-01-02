package com.example.desktopapp.service;

import com.example.desktopapp.util.AppConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service for creating transactions in the backend database
 */
public class TransactionService {

    /**
     * Create a transaction record for coin top-up
     * @param cardId Card ID
     * @param userAge User's age at time of transaction
     * @param amount Payment amount in VND
     * @return Transaction ID if successful, null otherwise
     */
    public String createTopupTransaction(String cardId, int userAge, int amount) throws Exception {
        String jsonBody = String.format(
            "{\"card_id\":\"%s\",\"user_age\":%d,\"payment\":%d}",
            cardId, userAge, amount
        );
        
        return createTransaction(jsonBody);
    }

    /**
     * Create a transaction record for game purchase
     * @param cardId Card ID
     * @param userAge User's age at time of transaction
     * @param gameId Game ID
     * @param amount Payment amount (game price in coins converted to VND)
     * @return Transaction ID if successful, null otherwise
     */
    public String createGameTransaction(String cardId, int userAge, int gameId, int amount) throws Exception {
        String jsonBody = String.format(
            "{\"card_id\":\"%s\",\"user_age\":%d,\"payment\":%d,\"game_id\":%d}",
            cardId, userAge, amount, gameId
        );
        
        return createTransaction(jsonBody);
    }

    /**
     * Create a transaction record for combo purchase
     * @param cardId Card ID
     * @param userAge User's age at time of transaction
     * @param comboId Combo ID
     * @param amount Payment amount in VND
     * @return Transaction ID if successful, null otherwise
     */
    public String createComboTransaction(String cardId, int userAge, String comboId, int amount) throws Exception {
        String jsonBody = String.format(
            "{\"card_id\":\"%s\",\"user_age\":%d,\"payment\":%d,\"combo_id\":\"%s\"}",
            cardId, userAge, amount, comboId
        );
        
        return createTransaction(jsonBody);
    }

    /**
     * Internal method to send POST request to create transaction
     */
    private String createTransaction(String jsonBody) throws Exception {
        URL url = new URL(AppConfig.API_TRANSACTIONS);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(AppConfig.CONNECTION_TIMEOUT);
            conn.setReadTimeout(AppConfig.READ_TIMEOUT);

            // Send request body
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            
            // Read response
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

            if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                String jsonResponse = response.toString();
                System.out.println("Transaction created: " + jsonResponse);
                
                // Extract transaction ID from response
                int idStart = jsonResponse.indexOf("\"_id\":\"");
                if (idStart != -1) {
                    idStart += 7;
                    int idEnd = jsonResponse.indexOf("\"", idStart);
                    return jsonResponse.substring(idStart, idEnd);
                }
                return "success";
            } else {
                System.err.println("Failed to create transaction. Status: " + responseCode);
                System.err.println("Response: " + response.toString());
                throw new Exception("Failed to create transaction: " + response.toString());
            }

        } finally {
            conn.disconnect();
        }
    }
}
