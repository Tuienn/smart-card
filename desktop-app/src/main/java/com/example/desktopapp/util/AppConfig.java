package com.example.desktopapp.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Application configuration constants
 * Centralized configuration for API endpoints and other settings
 */
public class AppConfig {
    
    /**
     * Backend API base URL
     * Change this to match your backend server address
     */
    public static final String API_BASE_URL = "http://localhost:4000";
    
    /**
     * API endpoints
     */
    public static final String API_COMBOS = API_BASE_URL + "/api/combos";
    public static final String API_CARDS = API_BASE_URL + "/api/cards";
    public static final String API_GAMES = API_BASE_URL + "/api/games";
    
    // MoMo Payment API
    public static final String API_MOMO_QR = API_BASE_URL + "/api/momo/qr";
    public static final String API_MOMO_STATUS = API_BASE_URL + "/api/momo/status";
    
    /**
     * Connection timeout in milliseconds
     */
    public static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * Read timeout in milliseconds
     */
    public static final int READ_TIMEOUT = 5000;
    
    /**
     * Session storage for client app - simple properties for game selection flow
     */
    private static final Map<String, String> sessionData = new HashMap<>();
    
    public static void setProperty(String key, String value) {
        sessionData.put(key, value);
    }
    
    public static String getProperty(String key, String defaultValue) {
        return sessionData.getOrDefault(key, defaultValue);
    }
    
    // Private constructor to prevent instantiation
    private AppConfig() {
        throw new AssertionError("Cannot instantiate AppConfig class");
    }
}
