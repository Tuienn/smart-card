package com.smartcard.constant;

public class AppletConsts {
    // AID của bạn (00 00 00 00 00 10)
    public static final byte[] AID = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10};

    // CLA (Class byte - định danh nhóm lệnh)
    public static final byte CLA = (byte) 0xA0;

    // INS (Instruction - Các lệnh cụ thể)
    public static final byte INS_VERIFY_PIN  = (byte) 0x10;
    public static final byte INS_CHANGE_PIN  = (byte) 0x11;
    public static final byte INS_GET_INFO    = (byte) 0x20;
    public static final byte INS_UPDATE_INFO = (byte) 0x30;

    // SW (Status Word - Mã trạng thái)
    public static final int SW_SUCCESS = 0x9000;
    public static final int SW_AUTH_FAILED = 0x6300;
}