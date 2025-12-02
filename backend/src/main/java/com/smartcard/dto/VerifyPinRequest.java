package com.smartcard.dto;

public class VerifyPinRequest {
    private String pin;

    public VerifyPinRequest() {}

    public VerifyPinRequest(String pin) {
        this.pin = pin;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
