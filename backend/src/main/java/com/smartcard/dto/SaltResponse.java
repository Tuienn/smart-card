package com.smartcard.dto;

public class SaltResponse {
    private String userSalt;
    private String adminSalt;

    public SaltResponse() {}

    public SaltResponse(String userSalt, String adminSalt) {
        this.userSalt = userSalt;
        this.adminSalt = adminSalt;
    }

    public String getUserSalt() {
        return userSalt;
    }

    public void setUserSalt(String userSalt) {
        this.userSalt = userSalt;
    }

    public String getAdminSalt() {
        return adminSalt;
    }

    public void setAdminSalt(String adminSalt) {
        this.adminSalt = adminSalt;
    }
}
