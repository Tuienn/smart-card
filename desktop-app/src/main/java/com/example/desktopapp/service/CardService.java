package com.example.desktopapp.service;

import com.example.desktopapp.model.UserRegistration;

import javax.smartcardio.*;
import java.util.List;

/**
 * Service for communicating with JavaCard Entertainment Applet
 */
public class CardService {
    
    private Card card;
    private CardChannel channel;
    private boolean connected = false;
    
    // Debug mode for logging APDU commands
    private boolean debugMode = true;
    
    /**
     * Connect to smart card reader and select applet
     * @return true if connection successful
     */
    public boolean connect() throws CardException {
        return connect(null); // Use first available terminal
    }
    
    /**
     * Connect to a specific terminal and select applet
     * @param terminalName name of terminal to connect (null for first available)
     * @return true if connection successful
     */
    public boolean connect(String terminalName) throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        
        if (terminals.isEmpty()) {
            throw new CardException("Không tìm thấy đầu đọc thẻ. Hãy đảm bảo jCIDE simulator đang chạy với PC/SC được bật.");
        }
        
        // Log available terminals
        if (debugMode) {
            System.out.println("=== Available Card Terminals ===");
            for (CardTerminal t : terminals) {
                System.out.println("  - " + t.getName());
            }
        }
        
        // Find terminal
        CardTerminal terminal = null;
        if (terminalName != null && !terminalName.isEmpty()) {
            for (CardTerminal t : terminals) {
                if (t.getName().contains(terminalName)) {
                    terminal = t;
                    break;
                }
            }
            if (terminal == null) {
                throw new CardException("Không tìm thấy terminal: " + terminalName);
            }
        } else {
            // Use first available terminal
            terminal = terminals.get(0);
        }
        
        if (debugMode) {
            System.out.println("Using terminal: " + terminal.getName());
        }
        
        if (!terminal.isCardPresent()) {
            throw new CardException("Không có thẻ trong đầu đọc. Hãy đảm bảo jCIDE simulator đang chạy và thẻ đã được power on.");
        }
        
        // Connect to card - try T=1 first (preferred for jCIDE), then T=0, then any
        String[] protocols = {"T=1", "T=0", "*"};
        CardException lastException = null;
        
        for (String protocol : protocols) {
            try {
                if (debugMode) {
                    System.out.println("Trying protocol: " + protocol);
                }
                card = terminal.connect(protocol);
                if (debugMode) {
                    System.out.println("Connected with protocol: " + card.getProtocol());
                    System.out.println("ATR: " + bytesToHex(card.getATR().getBytes()));
                }
                break;
            } catch (CardException e) {
                lastException = e;
                if (debugMode) {
                    System.out.println("Protocol " + protocol + " failed: " + e.getMessage());
                }
            }
        }
        
        if (card == null) {
            throw new CardException("Không thể kết nối với thẻ: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"));
        }
        
        channel = card.getBasicChannel();
        
        // Select applet
        if (debugMode) {
            System.out.println("=== Selecting Applet ===");
            System.out.println("AID: " + bytesToHex(APDUConstants.APPLET_AID));
        }
        
        CommandAPDU selectCmd = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APDUConstants.APPLET_AID);
        ResponseAPDU response = transmitCommand(selectCmd);
        
        if (response.getSW() == APDUConstants.SW_SUCCESS) {
            connected = true;
            System.out.println("=== Applet Selected Successfully ===");
            return true;
        } else {
            String errorMsg = "Không thể chọn applet. SW=" + String.format("%04X", response.getSW()) + 
                " (" + APDUConstants.getErrorMessage(response.getSW()) + ")";
            if (response.getSW() == 0x6A82) {
                errorMsg += "\nApplet chưa được cài đặt. Hãy cài đặt Entertainment applet trên jCIDE với AID: " + 
                    bytesToHex(APDUConstants.APPLET_AID);
            }
            throw new CardException(errorMsg);
        }
    }
    
    /**
     * Transmit APDU command with debug logging
     */
    private ResponseAPDU transmitCommand(CommandAPDU cmd) throws CardException {
        if (debugMode) {
            System.out.println(">> " + bytesToHex(cmd.getBytes()));
        }
        ResponseAPDU response = channel.transmit(cmd);
        if (debugMode) {
            System.out.println("<< " + bytesToHex(response.getBytes()) + 
                " (SW=" + String.format("%04X", response.getSW()) + ")");
        }
        return response;
    }
    
    /**
     * Disconnect from smart card
     */
    public void disconnect() {
        if (card != null) {
            try {
                card.disconnect(false);
            } catch (CardException e) {
                // Ignore
            }
        }
        connected = false;
        card = null;
        channel = null;
    }
    
    /**
     * Check if connected to card
     */
    public boolean isConnected() {
        return connected && card != null;
    }
    
    /**
     * Initialize card with PIN and User ID (INS_INSTALL)
     * @return RSA public key bytes
     */
    public byte[] installCard(UserRegistration user) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] pinBytes = user.getPinBytes();
        byte[] userId = user.getUserId();
        
        // Build data: [PIN_LENGTH(1)] [PIN(4-16)] [USER_ID(16)]
        byte[] data = new byte[1 + pinBytes.length + userId.length];
        data[0] = (byte) pinBytes.length;
        System.arraycopy(pinBytes, 0, data, 1, pinBytes.length);
        System.arraycopy(userId, 0, data, 1 + pinBytes.length, userId.length);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_INSTALL,
            0x00, 0x00,
            data,
            256 // Expected response length for RSA public key
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi khởi tạo thẻ: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        return response.getData();
    }
    
    /**
     * Verify PIN to start session (INS_VERIFY_PIN)
     * @throws PinVerificationException if PIN verification fails (with status word for error handling)
     * @throws CardException for other card communication errors
     */
    public void verifyPin(String pin) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] pinBytes = pin.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_VERIFY_PIN,
            0x00, 0x00,
            pinBytes
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            // Throw PinVerificationException with status word for proper error handling
            throw new PinVerificationException(
                APDUConstants.getErrorMessage(response.getSW()),
                response.getSW()
            );
        }
    }
    
    /**
     * Verify Admin PIN (INS_VERIFY_ADMIN_PIN)
     * Required before calling unlockByAdmin()
     * @param adminPin Admin PIN (16 characters: 1234567890123456)
     * @throws PinVerificationException if Admin PIN verification fails
     * @throws CardException for other card communication errors
     */
    public void verifyAdminPin(String adminPin) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] adminPinBytes = adminPin.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_VERIFY_ADMIN_PIN,
            0x00, 0x00,
            adminPinBytes
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new PinVerificationException(
                "Admin PIN sai: " + APDUConstants.getErrorMessage(response.getSW()),
                response.getSW()
            );
        }
    }
    
    /**
     * Unlock card by Admin (INS_UNLOCK_BY_ADMIN)
     * Requires Admin PIN verification first (call verifyAdminPin())
     * Optionally set a new user PIN
     * @param newPin New user PIN (optional, null to keep current PIN)
     * @throws CardException for card communication errors
     */
    public void unlockByAdmin(String newPin) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data;
        if (newPin != null && !newPin.isEmpty()) {
            data = newPin.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        } else {
            data = new byte[0];
        }
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_UNLOCK_BY_ADMIN,
            0x00, 0x00,
            data
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi mở khóa thẻ: " + APDUConstants.getErrorMessage(response.getSW()));
        }
    }
    
    /**
     * Top up coins (INS_TOPUP_COINS)
     */
    public void topupCoins(int coins) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        // Coins are stored as short (2 bytes) on the card
        if (coins < 0 || coins > 32767) {
            throw new CardException("Số coins không hợp lệ (0-32767)");
        }
        
        byte[] data = APDUConstants.shortToBytes((short) coins);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_TOPUP_COINS,
            0x00, 0x00,
            data
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi nạp coins: " + APDUConstants.getErrorMessage(response.getSW()));
        }
    }
    
    /**
     * Write user data (name, gender, age) using TLV format (INS_WRITE_USER_DATA_BASIC)
     */
    public void writeUserData(UserRegistration user) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] nameBytes = user.getNameBytes();
        byte age = user.getAgeAsByte();
        
        // Build TLV data: [TAG_NAME][LEN][VALUE] [TAG_GENDER][LEN][VALUE] [TAG_AGE][LEN][VALUE]
        int dataLen = 2 + nameBytes.length + 3 + 3; // Name TLV + Gender TLV + Age TLV
        byte[] data = new byte[dataLen];
        int offset = 0;
        
        // Name TLV
        data[offset++] = APDUConstants.TAG_NAME;
        data[offset++] = (byte) nameBytes.length;
        System.arraycopy(nameBytes, 0, data, offset, nameBytes.length);
        offset += nameBytes.length;
        
        // Gender TLV
        data[offset++] = APDUConstants.TAG_GENDER;
        data[offset++] = (byte) 1;
        data[offset++] = user.getGender();
        
        // Age TLV
        data[offset++] = APDUConstants.TAG_AGE;
        data[offset++] = (byte) 1;
        data[offset++] = age;
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_WRITE_USER_DATA_BASIC,
            0x00, 0x00,
            data
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi ghi dữ liệu: " + APDUConstants.getErrorMessage(response.getSW()));
        }
    }
    
    /**
     * Write avatar image to card in chunks
     */
    public void writeAvatar(byte[] imageData, byte imageType) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        if (imageData == null || imageData.length == 0) {
            return; // No avatar to write
        }
        
        if (imageData.length > APDUConstants.MAX_IMAGE_SIZE) {
            throw new CardException("Ảnh quá lớn (tối đa " + APDUConstants.MAX_IMAGE_SIZE + " bytes)");
        }
        
        int totalSize = imageData.length;
        int chunkSize = APDUConstants.IMAGE_CHUNK_SIZE;
        
        // First chunk with INS_WRITE_IMAGE_START
        int firstChunkLen = Math.min(chunkSize, totalSize);
        byte[] firstData = new byte[3 + firstChunkLen]; // 2 bytes total size + 1 byte type + data
        firstData[0] = (byte) ((totalSize >> 8) & 0xFF);
        firstData[1] = (byte) (totalSize & 0xFF);
        firstData[2] = imageType;
        System.arraycopy(imageData, 0, firstData, 3, firstChunkLen);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_WRITE_IMAGE_START,
            0x00, 0x00,
            firstData
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi ghi ảnh: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        // Continue with remaining chunks
        int offset = firstChunkLen;
        while (offset < totalSize) {
            int remaining = totalSize - offset;
            int thisChunkLen = Math.min(chunkSize, remaining);
            
            byte[] chunkData = new byte[2 + thisChunkLen]; // 2 bytes offset + data
            chunkData[0] = (byte) ((offset >> 8) & 0xFF);
            chunkData[1] = (byte) (offset & 0xFF);
            System.arraycopy(imageData, offset, chunkData, 2, thisChunkLen);
            
            cmd = new CommandAPDU(
                APDUConstants.CLA,
                APDUConstants.INS_WRITE_IMAGE_CONTINUE,
                0x00, 0x00,
                chunkData
            );
            
            response = transmitCommand(cmd);
            if (response.getSW() != APDUConstants.SW_SUCCESS) {
                throw new CardException("Lỗi ghi ảnh (chunk): " + APDUConstants.getErrorMessage(response.getSW()));
            }
            
            offset += thisChunkLen;
        }
    }
    
    /**
     * Read current coins from card (INS_READ_USER_DATA_BASIC)
     */
    public int readCoins() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data = { APDUConstants.TAG_COINS };
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_DATA_BASIC,
            0x00, 0x00,
            data,
            4 // 4 bytes for coins
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi đọc coins: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        byte[] coinBytes = response.getData();
        if (coinBytes.length >= 2) {
            return ((coinBytes[0] & 0xFF) << 8) | (coinBytes[1] & 0xFF);
        }
        return 0;
    }
    
    /**
     * Read user name from card (INS_READ_USER_DATA_BASIC with TAG_NAME)
     */
    public String readName() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data = { APDUConstants.TAG_NAME };
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_DATA_BASIC,
            0x00, 0x00,
            data,
            64 // Max name length
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi đọc tên: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        byte[] nameBytes = response.getData();
        return new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8).trim();
    }
    
    /**
     * Read gender from card (INS_READ_USER_DATA_BASIC with TAG_GENDER)
     * @return 0=Unknown, 1=Male, 2=Female
     */
    public byte readGender() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data = { APDUConstants.TAG_GENDER };
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_DATA_BASIC,
            0x00, 0x00,
            data,
            1
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi đọc giới tính: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        byte[] genderBytes = response.getData();
        return genderBytes.length > 0 ? genderBytes[0] : 0;
    }
    
    /**
     * Read age from card (INS_READ_USER_DATA_BASIC with TAG_AGE)
     */
    public byte readAge() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data = { APDUConstants.TAG_AGE };
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_DATA_BASIC,
            0x00, 0x00,
            data,
            1
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi đọc tuổi: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        byte[] ageBytes = response.getData();
        return ageBytes.length > 0 ? ageBytes[0] : 0;
    }
    
    /**
     * Read avatar image from card (INS_READ_IMAGE)
     * @return image bytes or null if no image
     */
    public byte[] readAvatar() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        // First, read a small chunk to get image info
        int chunkSize = APDUConstants.IMAGE_CHUNK_SIZE;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        int offset = 0;
        
        while (true) {
            // Build read command: [offset 2 bytes] [length 2 bytes]
            byte[] data = new byte[4];
            data[0] = (byte) ((offset >> 8) & 0xFF);
            data[1] = (byte) (offset & 0xFF);
            data[2] = (byte) ((chunkSize >> 8) & 0xFF);
            data[3] = (byte) (chunkSize & 0xFF);
            
            CommandAPDU cmd = new CommandAPDU(
                APDUConstants.CLA,
                APDUConstants.INS_READ_IMAGE,
                0x00, 0x00,
                data,
                chunkSize
            );
            
            ResponseAPDU response = transmitCommand(cmd);
            
            if (response.getSW() != APDUConstants.SW_SUCCESS) {
                if (offset == 0) {
                    // No image stored
                    return null;
                }
                break;
            }
            
            byte[] chunk = response.getData();
            if (chunk == null || chunk.length == 0) {
                break;
            }
            
            baos.write(chunk, 0, chunk.length);
            offset += chunk.length;
            
            // If we got less than requested, we're done
            if (chunk.length < chunkSize) {
                break;
            }
        }
        
        byte[] result = baos.toByteArray();
        return result.length > 0 ? result : null;
    }
    
    /**
     * Reset card (INS_RESET_CARD)
     */
    public void resetCard() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_RESET_CARD,
            0x00, 0x00
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi reset thẻ: " + APDUConstants.getErrorMessage(response.getSW()));
        }
    }
    
    /**
     * Full card setup process for new user registration
     */
    public void setupNewCard(UserRegistration user) throws CardException {
        // Step 1: Install card with PIN and UserID
        installCard(user);
        
        // Step 2: Verify PIN to start session
        verifyPin(user.getPin());
        
        // Step 3: Write user data (name, gender)
        writeUserData(user);
        
        // Step 4: Top up coins
        if (user.getCoins() > 0) {
            topupCoins(user.getCoins());
        }
        
        // Step 5: Write avatar if available
        if (user.getAvatar() != null && user.getAvatar().length > 0) {
            writeAvatar(user.getAvatar(), APDUConstants.IMAGE_TYPE_JPG);
        }
    }
    
    /**
     * Get list of available card terminals
     */
    public static List<CardTerminal> getAvailableTerminals() throws CardException {
        TerminalFactory factory = TerminalFactory.getDefault();
        return factory.terminals().list();
    }
    
    /**
     * Check if any card terminal is available
     */
    public static boolean hasCardReader() {
        try {
            return !getAvailableTerminals().isEmpty();
        } catch (CardException e) {
            return false;
        }
    }
    
    /**
     * Set debug mode for APDU logging
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugMode() {
        return debugMode;
    }
    
    /**
     * Convert byte array to hex string for debugging
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    /**
     * Test connection to jCIDE simulator
     * This method can be used to diagnose connection issues
     */
    public static void testConnection() {
        System.out.println("========================================");
        System.out.println("    JavaCard Connection Test");
        System.out.println("========================================");
        
        try {
            // Step 1: Check PC/SC service
            System.out.println("\n[1] Checking PC/SC service...");
            TerminalFactory factory = TerminalFactory.getDefault();
            System.out.println("    Provider: " + factory.getProvider().getName());
            
            // Step 2: List terminals
            System.out.println("\n[2] Listing card terminals...");
            List<CardTerminal> terminals = factory.terminals().list();
            
            if (terminals.isEmpty()) {
                System.out.println("    ERROR: No card terminals found!");
                System.out.println("    - Make sure jCIDE is running");
                System.out.println("    - Enable PC/SC in jCIDE: Tools -> Options -> PC/SC");
                System.out.println("    - Check if PC/SC service is running");
                return;
            }
            
            for (CardTerminal t : terminals) {
                System.out.println("    Found: " + t.getName());
                System.out.println("    Card present: " + t.isCardPresent());
            }
            
            // Step 3: Try to connect
            System.out.println("\n[3] Attempting to connect...");
            CardTerminal terminal = terminals.get(0);
            
            if (!terminal.isCardPresent()) {
                System.out.println("    ERROR: No card in terminal!");
                System.out.println("    - In jCIDE, make sure the card is powered on");
                System.out.println("    - Click 'Power On' button in jCIDE");
                return;
            }
            
            Card card = terminal.connect("*");
            System.out.println("    Connected! Protocol: " + card.getProtocol());
            System.out.println("    ATR: " + bytesToHex(card.getATR().getBytes()));
            
            // Step 4: Try to select applet
            System.out.println("\n[4] Selecting Entertainment applet...");
            System.out.println("    AID: " + bytesToHex(APDUConstants.APPLET_AID));
            
            CardChannel channel = card.getBasicChannel();
            CommandAPDU selectCmd = new CommandAPDU(0x00, 0xA4, 0x04, 0x00, APDUConstants.APPLET_AID);
            System.out.println("    >> " + bytesToHex(selectCmd.getBytes()));
            
            ResponseAPDU response = channel.transmit(selectCmd);
            System.out.println("    << " + bytesToHex(response.getBytes()));
            System.out.println("    SW: " + String.format("%04X", response.getSW()));
            
            if (response.getSW() == 0x9000) {
                System.out.println("    SUCCESS: Applet selected!");
            } else if (response.getSW() == 0x6A82) {
                System.out.println("    ERROR: Applet not found!");
                System.out.println("    - Install Entertainment applet in jCIDE");
                System.out.println("    - AID should be: 11 22 33 44 55 00");
            } else {
                System.out.println("    ERROR: " + APDUConstants.getErrorMessage(response.getSW()));
            }
            
            card.disconnect(false);
            
        } catch (CardException e) {
            System.out.println("    ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========================================");
    }
    
    /**
     * Main method for testing connection
     */
    public static void main(String[] args) {
        testConnection();
    }
}
