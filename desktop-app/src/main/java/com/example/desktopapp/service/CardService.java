package com.example.desktopapp.service;

import com.example.desktopapp.model.UserRegistration;
import com.example.desktopapp.util.AppConfig;

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
            
            // Check if card is initialized
            try {
                boolean initialized = isCardInitialized();
                
                if (!initialized) {
                    if (debugMode) {
                        System.out.println("Card not initialized yet (new card) - authentication skipped");
                    }
                    // Keep connection for card registration
                    return true;
                }
                
                // Card is initialized, perform authentication
                if (debugMode) {
                    System.out.println("\n=== Starting Automatic Authentication ===");
                }
                
                boolean authenticated = authenticateCard();
                
                if (!authenticated) {
                    // Disconnect if authentication fails
                    disconnect();
                    throw new CardException("Xác thực thẻ thất bại! Thẻ không hợp lệ.");
                }
                
                System.out.println("=== Card Authenticated Successfully ===");
                
            } catch (CardException e) {
                // For errors, disconnect and rethrow
                disconnect();
                throw e;
            }
            
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
     * Purchase combo (INS_PURCHASE_COMBO)
     * Buys multiple games at once
     * @param gameIds Array of game IDs to purchase
     * @param totalPrice Total price of the combo
     * @throws CardException if purchase fails
     */
    public void purchaseCombo(short[] gameIds, int totalPrice) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        if (gameIds == null || gameIds.length == 0) {
            throw new CardException("Không có game nào để mua");
        }
        
        if (gameIds.length > 50) {
            throw new CardException("Quá nhiều game trong combo (tối đa 50)");
        }
        
        // Build data: [NUM_GAMES(1)] [GAME_ID_1] [GAME_ID_2] ... [TOTAL_PRICE(4 bytes)]
        byte[] data = new byte[1 + gameIds.length + 4];
        data[0] = (byte) gameIds.length;
        
        // Add game IDs (each game ID is 1 byte)
        for (int i = 0; i < gameIds.length; i++) {
            data[1 + i] = (byte) (gameIds[i] & 0xFF);
        }
        
        // Add total price (4 bytes, big endian)
        int priceOffset = 1 + gameIds.length;
        data[priceOffset] = (byte) ((totalPrice >> 24) & 0xFF);
        data[priceOffset + 1] = (byte) ((totalPrice >> 16) & 0xFF);
        data[priceOffset + 2] = (byte) ((totalPrice >> 8) & 0xFF);
        data[priceOffset + 3] = (byte) (totalPrice & 0xFF);
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_PURCHASE_COMBO,
            0x00, 0x00,
            data
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi mua combo: " + APDUConstants.getErrorMessage(response.getSW()));
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
     * Read purchased game IDs from card (INS_READ_USER_DATA_BASIC with TAG_BOUGHT_GAMES)
     * @return array of game IDs (shorts), or empty array if no games
     */
    public short[] readPurchasedGames() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        byte[] data = { APDUConstants.TAG_BOUGHT_GAMES };
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_DATA_BASIC,
            0x00, 0x00,
            data,
            256 // Max games data
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            // If no games purchased, might return empty or error
            return new short[0];
        }
        
        byte[] gameBytes = response.getData();
        if (gameBytes.length == 0) {
            return new short[0];
        }
        
        // Convert bytes to shorts (each game ID is 1 byte, not 2)
        // Card stores game IDs as byte array: [01, 04, 07, 0A, 05] for games 1, 4, 7, 10, 5
        short[] gameIds = new short[gameBytes.length];
        for (int i = 0; i < gameIds.length; i++) {
            gameIds[i] = (short) (gameBytes[i] & 0xFF); // Convert byte to unsigned short
        }
        
        return gameIds;
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
     * Check if card is initialized (has User ID)
     * @return true if card has been initialized, false if not
     */
    public boolean isCardInitialized() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        try {
            CommandAPDU cmd = new CommandAPDU(
                APDUConstants.CLA,
                APDUConstants.INS_READ_USER_ID,
                0x00, 0x00,
                16
            );
            
            ResponseAPDU response = transmitCommand(cmd);
            
            // If SW = 0x9000, card is initialized
            if (response.getSW() == APDUConstants.SW_SUCCESS) {
                return true;
            }
            
            // If SW = 0x6985 (SW_CONDITIONS_NOT_SATISFIED), card is not initialized
            if (response.getSW() == 0x6985) {
                return false;
            }
            
            // Other errors
            throw new CardException("Lỗi kiểm tra trạng thái thẻ: " + APDUConstants.getErrorMessage(response.getSW()));
            
        } catch (CardException e) {
            // If it's a connection error, rethrow
            throw e;
        }
    }
    
    /**
     * Read user ID from card (INS_READ_USER_ID)
     * @return 16-byte user ID
     */
    public byte[] readUserId() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_READ_USER_ID,
            0x00, 0x00,
            16 // Expected response length
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi đọc User ID: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        return response.getData();
    }
    
    /**
     * Sign challenge with card's RSA private key (INS_SIGN_CHALLENGE)
     * @param challenge Random challenge bytes
     * @return RSA signature
     */
    public byte[] signChallenge(byte[] challenge) throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        CommandAPDU cmd = new CommandAPDU(
            APDUConstants.CLA,
            APDUConstants.INS_SIGN_CHALLENGE,
            0x00, 0x00,
            challenge,
            256 // Expected RSA signature length (1024 bits = 128 bytes)
        );
        
        ResponseAPDU response = transmitCommand(cmd);
        
        if (response.getSW() != APDUConstants.SW_SUCCESS) {
            throw new CardException("Lỗi ký challenge: " + APDUConstants.getErrorMessage(response.getSW()));
        }
        
        return response.getData();
    }
    
    /**
     * Authenticate card using RSA challenge-response
     * @param publicKeyBytes RSA public key from backend (DER encoded or raw modulus+exponent)
     * @return true if authentication successful
     */
    public boolean authenticateWithChallenge(byte[] publicKeyBytes) throws CardException {
        try {
            // Generate random challenge (32 bytes)
            java.security.SecureRandom random = new java.security.SecureRandom();
            byte[] challenge = new byte[32];
            random.nextBytes(challenge);
            
            if (debugMode) {
                System.out.println("=== RSA Authentication ===");
                System.out.println("Challenge: " + bytesToHex(challenge));
            }
            
            // Get signature from card
            byte[] signature = signChallenge(challenge);
            
            if (debugMode) {
                System.out.println("Signature: " + bytesToHex(signature));
            }
            
            // Parse public key and verify signature
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(publicKeyBytes);
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // Verify signature
            java.security.Signature verifier = java.security.Signature.getInstance("SHA1withRSA");
            verifier.initVerify(publicKey);
            verifier.update(challenge);
            boolean verified = verifier.verify(signature);
            
            if (debugMode) {
                System.out.println("Verification result: " + verified);
            }
            
            return verified;
            
        } catch (Exception e) {
            if (debugMode) {
                System.out.println("Authentication error: " + e.getMessage());
                e.printStackTrace();
            }
            throw new CardException("Lỗi xác thực RSA: " + e.getMessage());
        }
    }
    
    /**
     * Get public key from backend API using user ID
     * @param userId 16-byte user ID from card
     * @return RSA public key bytes (DER encoded)
     */
    private byte[] getPublicKeyFromBackend(byte[] userId) throws CardException {
        try {
            // Convert user ID to hex string
            String userIdHex = bytesToHex(userId).replace(" ", "");
            
            if (debugMode) {
                System.out.println("=== Fetching Public Key from Backend ===");
                System.out.println("User ID: " + userIdHex);
            }
            
            // Call backend API
            String apiUrl = AppConfig.API_CARDS + "/" + userIdHex;
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new CardException("Không tìm thấy thẻ trong hệ thống (HTTP " + responseCode + ")");
            }
            
            // Read response
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            if (debugMode) {
                System.out.println("Backend response: " + response.toString());
            }
            
            // Parse JSON to extract public_key field
            // Simple parsing (for production, use Jackson or Gson)
            String jsonResponse = response.toString();
            int keyStart = jsonResponse.indexOf("\"public_key\"");
            if (keyStart == -1) {
                throw new CardException("Không tìm thấy public key trong response");
            }
            
            int valueStart = jsonResponse.indexOf("\"", keyStart + 13) + 1;
            int valueEnd = jsonResponse.indexOf("\"", valueStart);
            String publicKeyBase64 = jsonResponse.substring(valueStart, valueEnd);
            
            if (debugMode) {
                System.out.println("Public Key (Base64): " + publicKeyBase64.substring(0, Math.min(50, publicKeyBase64.length())) + "...");
            }
            
            // Decode Base64 to get public key bytes
            return java.util.Base64.getDecoder().decode(publicKeyBase64);
            
        } catch (Exception e) {
            if (debugMode) {
                System.out.println("Error fetching public key: " + e.getMessage());
                e.printStackTrace();
            }
            throw new CardException("Lỗi lấy public key từ backend: " + e.getMessage());
        }
    }
    
    /**
     * Full authentication flow when connecting to card:
     * 1. Read user ID from card
     * 2. Get public key from backend using user ID
     * 3. Perform RSA challenge-response authentication
     * @return true if authentication successful
     */
    public boolean authenticateCard() throws CardException {
        if (!isConnected()) {
            throw new CardException("Chưa kết nối với thẻ");
        }
        
        if (debugMode) {
            System.out.println("\n========== Card Authentication Flow ==========");
        }
        
        // Step 1: Read user ID from card
        byte[] userId = readUserId();
        if (debugMode) {
            System.out.println("Step 1: Read User ID - " + bytesToHex(userId));
        }
        
        // Step 2: Get public key from backend
        byte[] publicKey = getPublicKeyFromBackend(userId);
        if (debugMode) {
            System.out.println("Step 2: Retrieved Public Key from backend");
        }
        
        // Step 3: Authenticate using RSA challenge-response
        boolean authenticated = authenticateWithChallenge(publicKey);
        if (debugMode) {
            System.out.println("Step 3: Authentication " + (authenticated ? "SUCCESS" : "FAILED"));
            System.out.println("==============================================\n");
        }
        
        return authenticated;
    }
    
    /**
     * Register card to backend API
     * @param userId User ID from card (16 bytes)
     * @param publicKeyBytes RSA public key bytes (raw modulus + exponent from card)
     * @param userName User name
     * @param userAge User age
     * @param userGender User gender (true=male, false=female)
     * @return true if registration successful
     */
    public boolean registerCardToBackend(byte[] userId, byte[] publicKeyBytes, String userName, int userAge, boolean userGender) throws CardException {
        try {
            // Convert user ID to hex string (use as _id in MongoDB)
            String userIdHex = bytesToHex(userId).replace(" ", "");
            
            // Convert raw RSA key (modulus + exponent) to X.509 DER format
            byte[] x509Key = convertRawRSAKeyToX509(publicKeyBytes);
            
            // Convert public key to Base64
            String publicKeyBase64 = java.util.Base64.getEncoder().encodeToString(x509Key);
            
            if (debugMode) {
                System.out.println("=== Registering Card to Backend ===");
                System.out.println("User ID: " + userIdHex);
                System.out.println("User Name: " + userName);
                System.out.println("User Age: " + userAge);
                System.out.println("User Gender: " + (userGender ? "Male" : "Female"));
                System.out.println("Public Key (Base64): " + publicKeyBase64.substring(0, Math.min(50, publicKeyBase64.length())) + "...");
            }
            
            // Build JSON payload
            String jsonPayload = String.format(
                "{\"_id\":\"%s\",\"user_name\":\"%s\",\"user_age\":%d,\"user_gender\":%b,\"public_key\":\"%s\"}",
                userIdHex, userName, userAge, userGender, publicKeyBase64
            );
            
            // Call backend API
            String apiUrl = AppConfig.API_CARDS;
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            
            // Write payload
            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            
            // Read response
            java.io.BufferedReader reader;
            if (responseCode >= 200 && responseCode < 300) {
                reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream())
                );
            } else {
                reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getErrorStream())
                );
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            if (debugMode) {
                System.out.println("Backend response (HTTP " + responseCode + "): " + response.toString());
            }
            
            if (responseCode < 200 || responseCode >= 300) {
                throw new CardException("Lỗi đăng ký thẻ vào hệ thống (HTTP " + responseCode + "): " + response.toString());
            }
            
            System.out.println("=== Card Registered Successfully ===");
            return true;
            
        } catch (Exception e) {
            if (debugMode) {
                System.out.println("Error registering card: " + e.getMessage());
                e.printStackTrace();
            }
            throw new CardException("Lỗi đăng ký thẻ vào backend: " + e.getMessage());
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
     * Convert raw RSA key (modulus + exponent) to X.509 DER format
     * JavaCard returns: [modulus bytes][exponent bytes]
     * For RSA 1024-bit: modulus = 128 bytes, exponent = typically 3 bytes (0x010001)
     */
    private byte[] convertRawRSAKeyToX509(byte[] rawKey) throws CardException {
        try {
            // For RSA 1024-bit: modulus = 128 bytes
            // Exponent is typically 3 bytes (65537 = 0x010001)
            int modulusLen = 128; // 1024 bits / 8
            int exponentLen = rawKey.length - modulusLen;
            
            if (exponentLen <= 0 || exponentLen > 4) {
                throw new CardException("Invalid RSA key format: exponent length = " + exponentLen);
            }
            
            // Extract modulus and exponent
            byte[] modulus = new byte[modulusLen];
            byte[] exponent = new byte[exponentLen];
            System.arraycopy(rawKey, 0, modulus, 0, modulusLen);
            System.arraycopy(rawKey, modulusLen, exponent, 0, exponentLen);
            
            if (debugMode) {
                System.out.println("=== Converting Raw RSA Key to X.509 ===");
                System.out.println("Modulus length: " + modulusLen);
                System.out.println("Exponent length: " + exponentLen);
                System.out.println("Exponent: " + bytesToHex(exponent));
            }
            
            // Convert to BigInteger (remove leading zeros if needed)
            java.math.BigInteger modulusBigInt = new java.math.BigInteger(1, modulus);
            java.math.BigInteger exponentBigInt = new java.math.BigInteger(1, exponent);
            
            // Create RSA public key
            java.security.spec.RSAPublicKeySpec keySpec = new java.security.spec.RSAPublicKeySpec(modulusBigInt, exponentBigInt);
            java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
            java.security.PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // Get X.509 encoded bytes
            return publicKey.getEncoded();
            
        } catch (Exception e) {
            throw new CardException("Lỗi chuyển đổi RSA key: " + e.getMessage());
        }
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
