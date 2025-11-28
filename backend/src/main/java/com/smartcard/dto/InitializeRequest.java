package com.smartcard.dto;

public class InitializeRequest {
    private String userPin;
    private String adminPin;

    public InitializeRequest() {}

    public InitializeRequest(String userPin, String adminPin) {
        this.userPin = userPin;
        this.adminPin = adminPin;
    }

    public String getUserPin() {
        return userPin;
    }

    public void setUserPin(String userPin) {
        this.userPin = userPin;
    }

    public String getAdminPin() {
        return adminPin;
    }

    public void setAdminPin(String adminPin) {
        this.adminPin = adminPin;
    }
}
