package com.example.desktopapp.util;

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
    
    /**
     * Connection timeout in milliseconds
     */
    public static final int CONNECTION_TIMEOUT = 5000;
    
    /**
     * Read timeout in milliseconds
     */
    public static final int READ_TIMEOUT = 5000;
    
    // Private constructor to prevent instantiation
    private AppConfig() {
        throw new AssertionError("Cannot instantiate AppConfig class");
    }
}
