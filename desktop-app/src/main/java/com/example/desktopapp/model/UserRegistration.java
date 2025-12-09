package com.example.desktopapp.model;

/**
 * Model class for user registration data
 */
public class UserRegistration {
    private String name;
    private String age;
    private byte gender; // 0 = Not specified, 1 = Male, 2 = Female
    private String pin;
    private byte[] avatar;
    private int amountVND;
    private int coins;
    private byte[] userId; // 16 bytes unique ID

    public UserRegistration() {
        this.userId = new byte[16];
        this.gender = 0;
        this.coins = 0;
        this.amountVND = 0;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

    public int getAmountVND() {
        return amountVND;
    }

    public void setAmountVND(int amountVND) {
        this.amountVND = amountVND;
        // Convert VND to coins: 100,000 VND = 10 coins (10,000 VND = 1 coin)
        this.coins = amountVND / 10000;
    }

    public int getCoins() {
        return coins;
    }

    public byte[] getUserId() {
        return userId;
    }

    public void setUserId(byte[] userId) {
        this.userId = userId;
    }

    /**
     * Generate random user ID
     */
    public void generateUserId() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        random.nextBytes(this.userId);
    }

    /**
     * Get PIN as byte array (ASCII values)
     */
    public byte[] getPinBytes() {
        if (pin == null) return new byte[0];
        return pin.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    /**
     * Get name as byte array (UTF-8)
     */
    public byte[] getNameBytes() {
        if (name == null) return new byte[0];
        byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Limit to 64 bytes (max name length in card)
        if (nameBytes.length > 64) {
            byte[] truncated = new byte[64];
            System.arraycopy(nameBytes, 0, truncated, 0, 64);
            return truncated;
        }
        return nameBytes;
    }

    /**
     * Get age as byte value for smart card storage
     * @return age as byte (0-255)
     */
    public byte getAgeAsByte() {
        if (age == null || age.isEmpty()) return 0;
        try {
            int ageInt = Integer.parseInt(age);
            // Clamp to valid byte range (0-255)
            if (ageInt < 0) return 0;
            if (ageInt > 255) return (byte) 255;
            return (byte) ageInt;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "UserRegistration{" +
                "name='" + name + '\'' +
                ", age='" + age + '\'' +
                ", gender=" + gender +
                ", coins=" + coins +
                ", amountVND=" + amountVND +
                '}';
    }
}
