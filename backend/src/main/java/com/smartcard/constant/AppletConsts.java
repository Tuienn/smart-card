package com.smartcard.constant;

public class AppletConsts {
    // AID của bạn (11 22 33 44 55 00)
    public static final byte[] AID = {(byte)0x11, (byte)0x22, (byte)0x33, (byte)0x44, (byte)0x55, (byte)0x00};

    // CLA (Class byte - định danh nhóm lệnh)
    public static final byte CLA = (byte) 0x00;

    // INS (Instruction - Các lệnh theo APPLET_DOCUMENTATION)
    public static final byte INS_GET_SALT         = (byte) 0x10;
    public static final byte INS_VERIFY_USER_PIN  = (byte) 0x20;
    public static final byte INS_VERIFY_ADMIN_PIN = (byte) 0x21;
    public static final byte INS_CHANGE_USER_PIN  = (byte) 0x30;
    public static final byte INS_RESET_USER_PIN   = (byte) 0x31;
    public static final byte INS_GET_DATA         = (byte) 0x40;
    public static final byte INS_SET_DATA         = (byte) 0x50;
    public static final byte INS_INITIALIZE       = (byte) 0x60;

    // SW (Status Word - Mã trạng thái)
    public static final int SW_SUCCESS = 0x9000;
    public static final int SW_WRONG_LENGTH = 0x6700;
    public static final int SW_SECURITY_STATUS_NOT_SATISFIED = 0x6982;
    public static final int SW_AUTHENTICATION_METHOD_BLOCKED = 0x6983;
    public static final int SW_FILE_NOT_FOUND = 0x6A82;
    public static final int SW_INS_NOT_SUPPORTED = 0x6D00;
}