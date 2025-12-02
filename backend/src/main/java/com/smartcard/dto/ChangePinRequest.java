package com.smartcard.dto;

public class ChangePinRequest {
    private String newPin;

    public ChangePinRequest() {}

    public ChangePinRequest(String newPin) {
        this.newPin = newPin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }
}
