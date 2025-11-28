package com.smartcard.utils;

public class HexUtils {
    // Chuyển byte[] sang chuỗi Hex (VD: [10, 255] -> "0A FF")
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    // Chuyển chuỗi Hex sang byte[] (VD: "0A FF" -> [10, 255])
    public static byte[] hexToBytes(String s) {
        s = s.replace(" ", ""); // Xóa khoảng trắng
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}