package com.example.desktopapp.service;

/**
 * APDU Constants for Entertainment JavaCard Applet
 * Based on card/README.md documentation
 */
public class APDUConstants {
    
    // CLA byte - always 0x00
    public static final byte CLA = (byte) 0x00;
    
    // INS codes
    public static final byte INS_INSTALL = (byte) 0x10;
    public static final byte INS_VERIFY_PIN = (byte) 0x20;
    public static final byte INS_UNLOCK_BY_ADMIN = (byte) 0x21;
    public static final byte INS_CHECK_ACCESS_FOR_GAME = (byte) 0x30;
    public static final byte INS_TOPUP_COINS = (byte) 0x32;
    public static final byte INS_PURCHASE_COMBO = (byte) 0x33;
    public static final byte INS_SIGN_CHALLENGE = (byte) 0x41;
    public static final byte INS_READ_USER_DATA_BASIC = (byte) 0x50;
    public static final byte INS_WRITE_USER_DATA_BASIC = (byte) 0x51;
    public static final byte INS_WRITE_IMAGE_START = (byte) 0x52;
    public static final byte INS_WRITE_IMAGE_CONTINUE = (byte) 0x53;
    public static final byte INS_READ_IMAGE = (byte) 0x54;
    public static final byte INS_RESET_CARD = (byte) 0x99;
    
    // TLV Tags for user data
    public static final byte TAG_NAME = (byte) 0x01;
    public static final byte TAG_GENDER = (byte) 0x02;
    public static final byte TAG_COINS = (byte) 0x03;
    public static final byte TAG_BOUGHT_GAMES = (byte) 0x04;
    
    // Status Words
    public static final int SW_SUCCESS = 0x9000;
    public static final int SW_PIN_VERIFICATION_REQUIRED = 0x6982;
    public static final int SW_AUTHENTICATION_BLOCKED = 0x6983;
    public static final int SW_INSUFFICIENT_FUNDS = 0x6985;
    public static final int SW_WRONG_DATA = 0x6A80;
    public static final int SW_NOT_ENOUGH_MEMORY = 0x6A84;
    public static final int SW_COMMAND_NOT_ALLOWED = 0x6986;
    
    // Applet AID - Entertainment applet (jCIDE simulator)
    public static final byte[] APPLET_AID = {
        (byte) 0x11, (byte) 0x22, (byte) 0x33, (byte) 0x44, 
        (byte) 0x55, (byte) 0x00
    };
    
    // Limits
    public static final int MIN_PIN_LENGTH = 4;
    public static final int MAX_PIN_LENGTH = 16;
    public static final int USER_ID_LENGTH = 16;
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MAX_IMAGE_SIZE = 32767; // 32KB
    public static final int IMAGE_CHUNK_SIZE = 200; // Bytes per chunk
    
    // Image types
    public static final byte IMAGE_TYPE_JPG = (byte) 0x01;
    public static final byte IMAGE_TYPE_PNG = (byte) 0x02;
    
    /**
     * Check if status word indicates success
     */
    public static boolean isSuccess(int sw) {
        return sw == SW_SUCCESS;
    }
    
    /**
     * Get human-readable error message from status word
     */
    public static String getErrorMessage(int sw) {
        switch (sw) {
            case SW_SUCCESS:
                return "Thành công";
            case SW_PIN_VERIFICATION_REQUIRED:
                return "Cần xác thực PIN";
            case SW_AUTHENTICATION_BLOCKED:
                return "Thẻ bị khóa";
            case SW_INSUFFICIENT_FUNDS:
                return "Không đủ tiền";
            case SW_WRONG_DATA:
                return "Dữ liệu không hợp lệ";
            case SW_NOT_ENOUGH_MEMORY:
                return "Không đủ bộ nhớ";
            case SW_COMMAND_NOT_ALLOWED:
                return "Lệnh không được phép";
            default:
                if ((sw & 0xFFF0) == 0x63C0) {
                    int remaining = sw & 0x0F;
                    return "PIN sai, còn " + remaining + " lần thử";
                }
                return "Lỗi không xác định: 0x" + Integer.toHexString(sw).toUpperCase();
        }
    }
    
    /**
     * Convert int to 4-byte Big Endian array
     */
    public static byte[] intToBytes(int value) {
        return new byte[] {
            (byte) ((value >> 24) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }
    
    /**
     * Convert short to 2-byte Big Endian array
     */
    public static byte[] shortToBytes(short value) {
        return new byte[] {
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }
}
