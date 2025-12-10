package com.example.desktopapp.service;

import javax.smartcardio.CardException;

/**
 * Custom exception for PIN verification errors
 * Carries the status word (SW) to allow proper handling of different error cases
 */
public class PinVerificationException extends CardException {
    
    private final int statusWord;
    
    public PinVerificationException(String message, int statusWord) {
        super(message);
        this.statusWord = statusWord;
    }
    
    /**
     * Get the status word returned by the card
     */
    public int getStatusWord() {
        return statusWord;
    }
    
    /**
     * Check if the card is blocked (SW = 0x6983)
     */
    public boolean isCardBlocked() {
        return statusWord == APDUConstants.SW_AUTHENTICATION_BLOCKED;
    }
    
    /**
     * Check if PIN was wrong but card is not blocked (SW = 0x63CX)
     */
    public boolean isWrongPin() {
        return (statusWord & 0xFFF0) == 0x63C0;
    }
    
    /**
     * Get remaining PIN attempts (only valid if isWrongPin() returns true)
     */
    public int getRemainingAttempts() {
        if (isWrongPin()) {
            return statusWord & 0x0F;
        }
        return 0;
    }
}
