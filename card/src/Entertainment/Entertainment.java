package Entertainment;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class Entertainment extends Applet {
    // INS codes
    private static final byte INS_INSTALL = (byte) 0x10;
    private static final byte INS_VERIFY_PIN = (byte) 0x20;
    private static final byte INS_VERIFY_ADMIN_PIN = (byte) 0x22;
    private static final byte INS_UNLOCK_BY_ADMIN = (byte) 0x21;
    private static final byte INS_TRY_PLAY_GAME = (byte) 0x30;
    private static final byte INS_TOPUP_COINS = (byte) 0x32;
    private static final byte INS_PURCHASE_COMBO = (byte) 0x33;
    private static final byte INS_SIGN_CHALLENGE = (byte) 0x41;
    private static final byte INS_READ_USER_DATA_BASIC = (byte) 0x50;
    private static final byte INS_WRITE_USER_DATA_BASIC = (byte) 0x51;
    private static final byte INS_WRITE_IMAGE_START = (byte) 0x52;
    private static final byte INS_WRITE_IMAGE_CONTINUE = (byte) 0x53;
    private static final byte INS_READ_IMAGE = (byte) 0x54;
    private static final byte INS_READ_USER_ID = (byte) 0x55;
    private static final byte INS_RESET_CARD = (byte) 0x99;

    // Status words
    private static final short SW_PIN_VERIFICATION_REQUIRED = 0x6982;
    private static final short SW_AUTHENTICATION_BLOCKED = 0x6983;
    private static final short SW_INSUFFICIENT_FUNDS = 0x6985;
    private static final short SW_WRONG_DATA = 0x6A80;
    private static final short SW_NOT_ENOUGH_MEMORY = 0x6A84;

    // Constants
    private static final byte PIN_TRY_LIMIT = (byte) 3;
    private static final byte ADMIN_PIN_TRY_LIMIT = (byte) 3;
    private static final byte MAX_PIN_SIZE = (byte) 16;
    private static final byte SALT_SIZE = (byte) 16;
    private static final short AES_KEY_SIZE = (short) 16; // 128 bits (more compatible)
    private static final short HASH_SIZE = (short) 20; // SHA-1 for compatibility
    private static final short RSA_KEY_SIZE = (short) 1024; // 1024 bits (more compatible)
    private static final byte MAX_NAME_LENGTH = (byte) 64;
    private static final byte MAX_GAMES = (byte) 50;
    private static final short MAX_IMAGE_SIZE = (short) 32767; // ~32KB for image (max short value, close to 64KB with two buffers if needed)
    private static final short PBKDF2_ITERATIONS = (short) 10;
    private static final short MAX_ENCRYPTED_DATA_SIZE = (short) 256;

    // TLV tags for user data
    private static final byte TAG_NAME = (byte) 0x01;
    private static final byte TAG_GENDER = (byte) 0x02;
    private static final byte TAG_COINS = (byte) 0x03;
    private static final byte TAG_BOUGHT_GAMES = (byte) 0x04;
    private static final byte TAG_AGE = (byte) 0x05;

    // Persistent data
    private byte[] userID;
    private byte[] salt;
    private byte[] wrappedMasterKey;
    private byte[] adminWrappedMasterKey;
    private byte[] masterKeyHash;
    private byte[] encryptedUserData;
    private byte pinTryCounter;
    private byte adminPinTryCounter;
    private boolean lockedFlag;
    private boolean adminLockedFlag;
    private boolean initialized;
    private byte[] imageBuffer;
    private short imageSize;
    private byte imageType;

    // RSA keys
    private RSAPrivateKey rsaPrivateKey;
    private RSAPublicKey rsaPublicKey;
    private KeyPair rsaKeyPair;

    // Session variables (transient)
    private boolean sessionAuth;
    private boolean adminSessionAuth;
    private byte[] masterKey; // Transient
    private byte[] tempBuffer; // Transient

    // Crypto objects
    private Cipher aesCipher;
    private MessageDigest sha256;
    private Signature rsaSignature;
    private RandomData randomGen;
    private PBKDF2 pbkdf2;

    private Entertainment() {
        // Initialize persistent storage
        userID = new byte[16];
        salt = new byte[SALT_SIZE];
        wrappedMasterKey = new byte[32]; // IV (16 bytes) + encrypted key (16 bytes)
        adminWrappedMasterKey = new byte[32]; // IV (16 bytes) + encrypted key (16 bytes)
        masterKeyHash = new byte[HASH_SIZE];
        encryptedUserData = new byte[MAX_ENCRYPTED_DATA_SIZE];
        imageBuffer = new byte[MAX_IMAGE_SIZE];

        pinTryCounter = PIN_TRY_LIMIT;
        adminPinTryCounter = ADMIN_PIN_TRY_LIMIT;
        lockedFlag = false;
        adminLockedFlag = false;
        initialized = false;
        imageSize = 0;
        imageType = 0;

        // Initialize transient arrays
        masterKey = JCSystem.makeTransientByteArray(AES_KEY_SIZE, JCSystem.CLEAR_ON_RESET);
        tempBuffer = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);

        // Initialize crypto objects with fallback options
        try {
            // Try AES-128 CBC first
            try {
                aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
            } catch (CryptoException e) {
                // Fallback to ECB if CBC not supported
                aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
            }
            
            // Use SHA-1 for better compatibility
            sha256 = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
            
            // Try RSA signature
            try {
                rsaSignature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
            } catch (CryptoException e) {
                // RSA signature may not be available, will handle in process methods
                rsaSignature = null;
            }
            
            randomGen = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
            
            // PBKDF2 may not be available on all cards
            try {
                pbkdf2 = PBKDF2.getInstance(PBKDF2.ALG_SHA_256);
            } catch (Exception e) {
                pbkdf2 = null; // Will use simple hash-based KDF instead
            }
        } catch (CryptoException e) {
            // Continue without optional features
        }

        // Initialize RSA key pair (may not be supported on all cards)
        try {
            rsaKeyPair = new KeyPair(KeyPair.ALG_RSA, RSA_KEY_SIZE);
            rsaPrivateKey = (RSAPrivateKey) rsaKeyPair.getPrivate();
            rsaPublicKey = (RSAPublicKey) rsaKeyPair.getPublic();
        } catch (CryptoException e) {
            // RSA not supported, continue without it
            rsaKeyPair = null;
            rsaPrivateKey = null;
            rsaPublicKey = null;
        }
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Entertainment().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            sessionAuth = false;
            adminSessionAuth = false;
            return;
        }

        byte[] buf = apdu.getBuffer();
        byte ins = buf[ISO7816.OFFSET_INS];

        switch (ins) {
            case INS_INSTALL:
                processInstall(apdu);
                break;
            case INS_VERIFY_PIN:
                processVerifyPin(apdu);
                break;
            case INS_VERIFY_ADMIN_PIN:
                processVerifyAdminPin(apdu);
                break;
            case INS_UNLOCK_BY_ADMIN:
                processUnlockByAdmin(apdu);
                break;
            case INS_TRY_PLAY_GAME:
                processTryPlayGame(apdu);
                break;
            case INS_TOPUP_COINS:
                processTopupCoins(apdu);
                break;
            case INS_PURCHASE_COMBO:
                processPurchaseCombo(apdu);
                break;
            case INS_SIGN_CHALLENGE:
                processSignChallenge(apdu);
                break;
            case INS_READ_USER_DATA_BASIC:
                processReadUserDataBasic(apdu);
                break;
            case INS_WRITE_USER_DATA_BASIC:
                processWriteUserDataBasic(apdu);
                break;
            case INS_WRITE_IMAGE_START:
                processWriteImageStart(apdu);
                break;
            case INS_WRITE_IMAGE_CONTINUE:
                processWriteImageContinue(apdu);
                break;
            case INS_READ_IMAGE:
                processReadImage(apdu);
                break;
            case INS_READ_USER_ID:
                processReadUserId(apdu);
                break;
            case INS_RESET_CARD:
                processResetCard(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void processInstall(APDU apdu) {
        if (initialized) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        short receivedBytes = apdu.setIncomingAndReceive();

        if (lc < 17) { // At least 1 byte PIN + 16 bytes userID
            ISOException.throwIt(SW_WRONG_DATA);
        }

        short offset = ISO7816.OFFSET_CDATA;
        byte pinLength = buffer[offset++];

        if (pinLength > MAX_PIN_SIZE || pinLength < 4) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        // Store PIN temporarily for KEK derivation
        byte[] pin = new byte[pinLength];
        Util.arrayCopy(buffer, offset, pin, (short) 0, pinLength);
        offset += pinLength;

        // Copy userID
        Util.arrayCopy(buffer, offset, userID, (short) 0, (short) 16);

        // Generate salt
        randomGen.generateData(salt, (short) 0, SALT_SIZE);

        // Generate master key
        randomGen.generateData(masterKey, (short) 0, AES_KEY_SIZE);

        // Hash master key
        if (sha256 != null) {
            sha256.reset();
            sha256.doFinal(masterKey, (short) 0, AES_KEY_SIZE, masterKeyHash, (short) 0);
        }

        // Derive KEK from PIN + salt
        byte[] kek = new byte[AES_KEY_SIZE];
        if (pbkdf2 != null) {
            pbkdf2.doFinal(pin, (short) 0, pinLength, salt, (short) 0, SALT_SIZE, PBKDF2_ITERATIONS, kek, (short) 0);
        } else {
            // Fallback: simple hash-based KDF
            deriveKeySimple(pin, pinLength, salt, kek);
        }

        // Wrap master key with KEK
        wrapKey(masterKey, AES_KEY_SIZE, kek, wrappedMasterKey);

        // Initialize admin PIN (default: "1234567890123456")
        byte[] adminPin = new byte[16];
        adminPin[0] = '1'; adminPin[1] = '2'; adminPin[2] = '3'; adminPin[3] = '4';
        adminPin[4] = '5'; adminPin[5] = '6'; adminPin[6] = '7'; adminPin[7] = '8';
        adminPin[8] = '9'; adminPin[9] = '0'; adminPin[10] = '1'; adminPin[11] = '2';
        adminPin[12] = '3'; adminPin[13] = '4'; adminPin[14] = '5'; adminPin[15] = '6';
        
        // Derive admin KEK from admin PIN + salt
        byte[] adminKek = new byte[AES_KEY_SIZE];
        if (pbkdf2 != null) {
            pbkdf2.doFinal(adminPin, (short) 0, (short) 16, salt, (short) 0, SALT_SIZE, PBKDF2_ITERATIONS, adminKek, (short) 0);
        } else {
            deriveKeySimple(adminPin, (short) 16, salt, adminKek);
        }
        
        // Wrap master key with admin KEK
        wrapKey(masterKey, AES_KEY_SIZE, adminKek, adminWrappedMasterKey);

        // Clear KEK and PIN
        Util.arrayFillNonAtomic(kek, (short) 0, AES_KEY_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(adminKek, (short) 0, AES_KEY_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(adminPin, (short) 0, (short) 16, (byte) 0);
        Util.arrayFillNonAtomic(pin, (short) 0, pinLength, (byte) 0);

        // Generate RSA key pair if supported
        if (rsaKeyPair != null) {
            rsaKeyPair.genKeyPair();
        }

        // Initialize encrypted user data with empty values
        initializeUserData();

        initialized = true;
        pinTryCounter = PIN_TRY_LIMIT;
        adminPinTryCounter = ADMIN_PIN_TRY_LIMIT;
        lockedFlag = false;
        adminLockedFlag = false;
        sessionAuth = true; // Auto-authenticate after successful installation
        adminSessionAuth = false;

        // Return public key (modulus and exponent) if RSA is supported
        if (rsaPublicKey != null) {
            short modulusLen = rsaPublicKey.getModulus(buffer, (short) 0);
            short exponentLen = rsaPublicKey.getExponent(buffer, modulusLen);
            short totalLen = (short) (modulusLen + exponentLen);
            apdu.setOutgoingAndSend((short) 0, totalLen);
        }
    }

    private void processVerifyPin(APDU apdu) {
        if (!initialized) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        if (lockedFlag) {
            ISOException.throwIt(SW_AUTHENTICATION_BLOCKED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        apdu.setIncomingAndReceive();

        if (lc < 4 || lc > MAX_PIN_SIZE) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        byte[] pin = new byte[lc];
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, pin, (short) 0, lc);

        // Derive KEK from PIN + salt
        byte[] kek = new byte[AES_KEY_SIZE];
        if (pbkdf2 != null) {
            pbkdf2.doFinal(pin, (short) 0, (short) lc, salt, (short) 0, SALT_SIZE, PBKDF2_ITERATIONS, kek, (short) 0);
        } else {
            deriveKeySimple(pin, (short) lc, salt, kek);
        }

        // Clear PIN
        Util.arrayFillNonAtomic(pin, (short) 0, lc, (byte) 0);

        // Unwrap master key
        boolean unwrapSuccess = unwrapKey(wrappedMasterKey, kek, masterKey);

        // Clear KEK
        Util.arrayFillNonAtomic(kek, (short) 0, AES_KEY_SIZE, (byte) 0);

        if (!unwrapSuccess) {
            handleWrongPin();
            return;
        }

        // Verify master key hash
        byte[] computedHash = new byte[HASH_SIZE];
        if (sha256 != null) {
            sha256.reset();
            sha256.doFinal(masterKey, (short) 0, AES_KEY_SIZE, computedHash, (short) 0);
        }

        boolean hashMatch = true;
        for (short i = 0; i < HASH_SIZE; i++) {
            if (computedHash[i] != masterKeyHash[i]) {
                hashMatch = false;
                break;
            }
        }

        if (hashMatch) {
            sessionAuth = true;
            pinTryCounter = PIN_TRY_LIMIT;
        } else {
            Util.arrayFillNonAtomic(masterKey, (short) 0, AES_KEY_SIZE, (byte) 0);
            handleWrongPin();
        }
    }

    private void handleWrongPin() {
        pinTryCounter--;
        if (pinTryCounter == 0) {
            lockedFlag = true;
            ISOException.throwIt(SW_AUTHENTICATION_BLOCKED);
        } else {
            ISOException.throwIt((short) (0x63C0 | pinTryCounter));
        }
    }

    private void processVerifyAdminPin(APDU apdu) {
        if (!initialized) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }

        if (adminLockedFlag) {
            ISOException.throwIt(SW_AUTHENTICATION_BLOCKED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        apdu.setIncomingAndReceive();

        if (lc < 4 || lc > MAX_PIN_SIZE) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        byte[] adminPin = new byte[lc];
        Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, adminPin, (short) 0, lc);

        // Derive admin KEK from admin PIN + salt
        byte[] adminKek = new byte[AES_KEY_SIZE];
        if (pbkdf2 != null) {
            pbkdf2.doFinal(adminPin, (short) 0, (short) lc, salt, (short) 0, SALT_SIZE, PBKDF2_ITERATIONS, adminKek, (short) 0);
        } else {
            deriveKeySimple(adminPin, (short) lc, salt, adminKek);
        }

        // Clear admin PIN
        Util.arrayFillNonAtomic(adminPin, (short) 0, lc, (byte) 0);

        // Unwrap master key with admin KEK
        byte[] tempMasterKey = new byte[AES_KEY_SIZE];
        boolean unwrapSuccess = unwrapKey(adminWrappedMasterKey, adminKek, tempMasterKey);

        // Clear admin KEK
        Util.arrayFillNonAtomic(adminKek, (short) 0, AES_KEY_SIZE, (byte) 0);

        if (!unwrapSuccess) {
            Util.arrayFillNonAtomic(tempMasterKey, (short) 0, AES_KEY_SIZE, (byte) 0);
            handleWrongAdminPin();
            return;
        }

        // Verify master key hash
        byte[] computedHash = new byte[HASH_SIZE];
        if (sha256 != null) {
            sha256.reset();
            sha256.doFinal(tempMasterKey, (short) 0, AES_KEY_SIZE, computedHash, (short) 0);
        }

        boolean hashMatch = true;
        for (short i = 0; i < HASH_SIZE; i++) {
            if (computedHash[i] != masterKeyHash[i]) {
                hashMatch = false;
                break;
            }
        }

        if (hashMatch) {
            adminSessionAuth = true;
            adminPinTryCounter = ADMIN_PIN_TRY_LIMIT;
            // Copy master key for use in unlockByAdmin
            Util.arrayCopy(tempMasterKey, (short) 0, masterKey, (short) 0, AES_KEY_SIZE);
            Util.arrayFillNonAtomic(tempMasterKey, (short) 0, AES_KEY_SIZE, (byte) 0);
        } else {
            Util.arrayFillNonAtomic(tempMasterKey, (short) 0, AES_KEY_SIZE, (byte) 0);
            handleWrongAdminPin();
        }
    }

    private void handleWrongAdminPin() {
        adminPinTryCounter--;
        if (adminPinTryCounter == 0) {
            adminLockedFlag = true;
            ISOException.throwIt(SW_AUTHENTICATION_BLOCKED);
        } else {
            ISOException.throwIt((short) (0x63C0 | adminPinTryCounter));
        }
    }

    private void processUnlockByAdmin(APDU apdu) {
        if (!initialized) {
            ISOException.throwIt(ISO7816.SW_COMMAND_NOT_ALLOWED);
        }
        
        // Require admin authentication
        if (!adminSessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        apdu.setIncomingAndReceive();

        // Reset counter and unlock
        pinTryCounter = PIN_TRY_LIMIT;
        lockedFlag = false;

        // Optionally change PIN if new PIN provided
        if (lc >= 4 && lc <= MAX_PIN_SIZE) {
            byte[] newPin = new byte[lc];
            Util.arrayCopy(buffer, ISO7816.OFFSET_CDATA, newPin, (short) 0, lc);

            // Re-wrap master key with new PIN
            byte[] kek = new byte[AES_KEY_SIZE];
            if (pbkdf2 != null) {
                pbkdf2.doFinal(newPin, (short) 0, (short) lc, salt, (short) 0, SALT_SIZE, PBKDF2_ITERATIONS, kek, (short) 0);
            } else {
                deriveKeySimple(newPin, (short) lc, salt, kek);
            }
            wrapKey(masterKey, AES_KEY_SIZE, kek, wrappedMasterKey);

            Util.arrayFillNonAtomic(kek, (short) 0, AES_KEY_SIZE, (byte) 0);
            Util.arrayFillNonAtomic(newPin, (short) 0, lc, (byte) 0);
        }
    }

    private void processTryPlayGame(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        byte gameID = buffer[ISO7816.OFFSET_CDATA];
        short requiredCoins = Util.getShort(buffer, (short) (ISO7816.OFFSET_CDATA + 1));

        // Decrypt user data
        decryptUserData(tempBuffer);

        // Check if game is already bought (unlimited access)
        short gamesOffset = findTag(tempBuffer, TAG_BOUGHT_GAMES);
        if (gamesOffset >= 0) {
            byte gameCount = tempBuffer[(short) (gamesOffset + 1)];
            for (byte i = 0; i < gameCount; i++) {
                if (tempBuffer[(short) (gamesOffset + 2 + i)] == gameID) {
                    // Game already purchased - allow free play
                    buffer[0] = (byte) 0x01;
                    apdu.setOutgoingAndSend((short) 0, (short) 1);
                    return;
                }
            }
        }

        // Game not purchased - pay per play
        short coinsOffset = findTag(tempBuffer, TAG_COINS);
        if (coinsOffset < 0) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        short currentCoins = Util.getShort(tempBuffer, (short) (coinsOffset + 2));
        if (currentCoins < requiredCoins) {
            // Not enough coins
            ISOException.throwIt(SW_INSUFFICIENT_FUNDS);
        }

        // Deduct coins for pay-per-play
        short newCoins = (short)(currentCoins - requiredCoins);
        Util.setShort(tempBuffer, (short) (coinsOffset + 2), newCoins);

        // Encrypt and save
        encryptUserData(tempBuffer);

        // Return success
        buffer[0] = (byte) 0x01;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void processTopupCoins(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short amount = Util.getShort(buffer, ISO7816.OFFSET_CDATA);

        // Decrypt user data
        decryptUserData(tempBuffer);

        // Update coins
        short coinsOffset = findTag(tempBuffer, TAG_COINS);
        if (coinsOffset < 0) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        short currentCoins = Util.getShort(tempBuffer, (short) (coinsOffset + 2));
        short newCoins = (short)(currentCoins + amount);
        Util.setShort(tempBuffer, (short) (coinsOffset + 2), newCoins);

        // Encrypt and save
        encryptUserData(tempBuffer);
    }

    private void processPurchaseCombo(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short offset = ISO7816.OFFSET_CDATA;
        byte numGames = buffer[offset++];

        if (numGames == 0 || numGames > MAX_GAMES) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        byte[] gamesToAdd = new byte[numGames];
        Util.arrayCopy(buffer, offset, gamesToAdd, (short) 0, numGames);
        offset += numGames;

        short totalPrice = Util.getShort(buffer, offset);

        // Decrypt user data
        decryptUserData(tempBuffer);

        // Check coins first
        short coinsOffset = findTag(tempBuffer, TAG_COINS);
        if (coinsOffset < 0) {
            ISOException.throwIt(SW_WRONG_DATA);
        }
        short currentCoins = Util.getShort(tempBuffer, (short) (coinsOffset + 2));
        if (currentCoins < totalPrice) {
            ISOException.throwIt(SW_INSUFFICIENT_FUNDS);
        }

        // Get current bought games
        short gamesOffset = findTag(tempBuffer, TAG_BOUGHT_GAMES);
        if (gamesOffset < 0) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        byte currentGameCount = tempBuffer[(short) (gamesOffset + 1)];
        
        // Build new game list (avoid duplicates)
        byte[] newGameList = new byte[MAX_GAMES];
        byte newGameCount = 0;
        
        // Copy existing games
        short gamesDataOffset = (short) (gamesOffset + 2);
        for (byte i = 0; i < currentGameCount; i++) {
            newGameList[newGameCount++] = tempBuffer[(short) (gamesDataOffset + i)];
        }
        
        // Add new games if not already in list
        for (byte i = 0; i < numGames; i++) {
            boolean exists = false;
            for (byte j = 0; j < newGameCount; j++) {
                if (newGameList[j] == gamesToAdd[i]) {
                    exists = true;
                    break;
                }
            }
            if (!exists && newGameCount < MAX_GAMES) {
                newGameList[newGameCount++] = gamesToAdd[i];
            }
        }

        // Rebuild TLV with updated games (this prevents overwriting other tags)
        rebuildTLVWithUpdatedField(tempBuffer, TAG_BOUGHT_GAMES, newGameList, (short) 0, newGameCount);

        // Deduct coins
        short newCoins = (short)(currentCoins - totalPrice);
        Util.setShort(tempBuffer, (short) (findTag(tempBuffer, TAG_COINS) + 2), newCoins);

        // Encrypt and save
        encryptUserData(tempBuffer);
    }

    private void processSignChallenge(APDU apdu) {
        if (rsaSignature == null || rsaPrivateKey == null) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }
        
        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        apdu.setIncomingAndReceive();

        // Sign the challenge
        rsaSignature.init(rsaPrivateKey, Signature.MODE_SIGN);
        short sigLen = rsaSignature.sign(buffer, ISO7816.OFFSET_CDATA, lc, buffer, (short) 0);

        apdu.setOutgoingAndSend((short) 0, sigLen);
    }

    private void processReadUserDataBasic(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        byte fieldSelector = buffer[ISO7816.OFFSET_CDATA];

        // Decrypt user data
        decryptUserData(tempBuffer);

        // Find and return requested field
        short offset = 0;
        short fieldOffset = findTag(tempBuffer, fieldSelector);

        if (fieldOffset < 0) {
            ISOException.throwIt(SW_WRONG_DATA);
        }

        byte fieldLen = tempBuffer[(short) (fieldOffset + 1)];
        Util.arrayCopy(tempBuffer, (short) (fieldOffset + 2), buffer, (short) 0, fieldLen);

        apdu.setOutgoingAndSend((short) 0, fieldLen);
    }

    private void processWriteUserDataBasic(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        short lc = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);
        apdu.setIncomingAndReceive();

        // Decrypt current user data
        decryptUserData(tempBuffer);

        // Parse input data and update fields
        // We need to rebuild TLV structure because field sizes can change
        short inOffset = ISO7816.OFFSET_CDATA;
        while (inOffset < (short) (ISO7816.OFFSET_CDATA + lc)) {
            byte tag = buffer[inOffset++];
            byte len = buffer[inOffset++];

            // Find and update the field value in tempBuffer
            short fieldOffset = findTag(tempBuffer, tag);
            if (fieldOffset >= 0) {
                byte oldLen = tempBuffer[(short) (fieldOffset + 1)];
                
                // If length changed, we need to rebuild the TLV structure
                if (oldLen != len) {
                    rebuildTLVWithUpdatedField(tempBuffer, tag, buffer, inOffset, len);
                    inOffset += len;
                    continue;
                }
                
                // Same length - can update in place
                Util.arrayCopy(buffer, inOffset, tempBuffer, (short) (fieldOffset + 2), len);
            }

            inOffset += len;
        }

        // Encrypt and save
        encryptUserData(tempBuffer);
    }
    
    private void rebuildTLVWithUpdatedField(byte[] tlvData, byte targetTag, byte[] newValue, short valueOffset, byte valueLen) {
        // Create a temporary buffer to rebuild TLV
        byte[] newTLV = new byte[160];
        short readOffset = 0;
        short writeOffset = 0;
        
        // Copy all fields, replacing the target field with new value
        while (readOffset < 160 && tlvData[readOffset] != 0) {
            byte tag = tlvData[readOffset];
            byte len = tlvData[(short) (readOffset + 1)];
            
            if (tag == targetTag) {
                // Write updated field
                newTLV[writeOffset++] = tag;
                newTLV[writeOffset++] = valueLen;
                Util.arrayCopy(newValue, valueOffset, newTLV, writeOffset, valueLen);
                writeOffset += valueLen;
                
                // Skip old field
                readOffset += 2 + len;
            } else {
                // Copy existing field as-is
                newTLV[writeOffset++] = tag;
                newTLV[writeOffset++] = len;
                Util.arrayCopy(tlvData, (short) (readOffset + 2), newTLV, writeOffset, len);
                writeOffset += len;
                readOffset += 2 + len;
            }
        }
        
        // Copy rebuilt TLV back to original buffer
        Util.arrayCopy(newTLV, (short) 0, tlvData, (short) 0, (short) 160);
    }

    private void processWriteImageStart(APDU apdu) {
        // if (!sessionAuth) {
            // ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        // }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short offset = ISO7816.OFFSET_CDATA;
        short totalImageSize = Util.getShort(buffer, offset);
        offset += 2;

        if (totalImageSize > MAX_IMAGE_SIZE) {
            ISOException.throwIt(SW_NOT_ENOUGH_MEMORY);
        }

        imageType = buffer[offset++];
        imageSize = totalImageSize;

        // Copy first chunk
        short chunkLen = (short) ((buffer[ISO7816.OFFSET_LC] & 0xFF) - 3);
        Util.arrayCopy(buffer, offset, imageBuffer, (short) 0, chunkLen);
    }

    private void processWriteImageContinue(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short offset = ISO7816.OFFSET_CDATA;
        short imageOffset = Util.getShort(buffer, offset);
        offset += 2;

        short chunkLen = (short) ((buffer[ISO7816.OFFSET_LC] & 0xFF) - 2);

        if ((short) (imageOffset + chunkLen) > MAX_IMAGE_SIZE) {
            ISOException.throwIt(SW_NOT_ENOUGH_MEMORY);
        }

        Util.arrayCopy(buffer, offset, imageBuffer, imageOffset, chunkLen);
    }

    private void processReadImage(APDU apdu) {
        if (!sessionAuth) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();

        short offset = ISO7816.OFFSET_CDATA;
        short imageOffset = Util.getShort(buffer, offset);
        offset += 2;
        short length = Util.getShort(buffer, offset);

        if ((short) (imageOffset + length) > imageSize) {
            length = (short) (imageSize - imageOffset);
        }

        Util.arrayCopy(imageBuffer, imageOffset, buffer, (short) 0, length);
        apdu.setOutgoingAndSend((short) 0, length);
    }

    private void processReadUserId(APDU apdu) {
        if (!initialized) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        
        byte[] buffer = apdu.getBuffer();
        // Return userID (16 bytes)
        Util.arrayCopy(userID, (short) 0, buffer, (short) 0, (short) 16);
        apdu.setOutgoingAndSend((short) 0, (short) 16);
    }

    private void processResetCard(APDU apdu) {
        // Admin authentication should be required here
        // Clear all sensitive data
        Util.arrayFillNonAtomic(userID, (short) 0, (short) 16, (byte) 0);
        Util.arrayFillNonAtomic(salt, (short) 0, SALT_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(wrappedMasterKey, (short) 0, (short) wrappedMasterKey.length, (byte) 0);
        Util.arrayFillNonAtomic(adminWrappedMasterKey, (short) 0, (short) adminWrappedMasterKey.length, (byte) 0);
        Util.arrayFillNonAtomic(masterKeyHash, (short) 0, HASH_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(encryptedUserData, (short) 0, MAX_ENCRYPTED_DATA_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(imageBuffer, (short) 0, MAX_IMAGE_SIZE, (byte) 0);
        Util.arrayFillNonAtomic(masterKey, (short) 0, AES_KEY_SIZE, (byte) 0);

        // Clear RSA keys
        if (rsaPrivateKey != null) {
            rsaPrivateKey.clearKey();
        }
        if (rsaPublicKey != null) {
            rsaPublicKey.clearKey();
        }

        initialized = false;
        sessionAuth = false;
        adminSessionAuth = false;
        pinTryCounter = PIN_TRY_LIMIT;
        adminPinTryCounter = ADMIN_PIN_TRY_LIMIT;
        lockedFlag = false;
        adminLockedFlag = false;
        imageSize = 0;
        imageType = 0;
    }

    // Helper methods

    private void initializeUserData() {
        // Initialize TLV structure: TAG | LENGTH | VALUE
        // TLV must be compact - no reserved space, length must match actual value size
        short offset = 0;

        // TAG_NAME - empty initially
        tempBuffer[offset++] = TAG_NAME;
        tempBuffer[offset++] = 0; // Empty name, no value bytes

        // TAG_GENDER
        tempBuffer[offset++] = TAG_GENDER;
        tempBuffer[offset++] = 1;
        tempBuffer[offset++] = 0; // Default gender

        // TAG_COINS
        tempBuffer[offset++] = TAG_COINS;
        tempBuffer[offset++] = 2;
        Util.setShort(tempBuffer, offset, (short)0); // Initial coins = 0
        offset += 2;

        // TAG_BOUGHT_GAMES - empty initially
        tempBuffer[offset++] = TAG_BOUGHT_GAMES;
        tempBuffer[offset++] = 0; // No games initially, no value bytes

        // TAG_AGE
        tempBuffer[offset++] = TAG_AGE;
        tempBuffer[offset++] = 1;
        tempBuffer[offset++] = 0; // Default age = 0

        // Encrypt and save
        encryptUserData(tempBuffer);
    }



    private void wrapKey(byte[] plainKey, short keyLen, byte[] kek, byte[] wrappedKey) {
        // Simplified AES key wrap using AES-CBC
        // For production, use proper AES Key Wrap (RFC 3394)
        AESKey aesKek = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, (short) (kek.length * 8), false);
        aesKek.setKey(kek, (short) 0);

        // Generate IV
        byte[] iv = new byte[16];
        randomGen.generateData(iv, (short) 0, (short) 16);

        // Store IV at the beginning
        Util.arrayCopy(iv, (short) 0, wrappedKey, (short) 0, (short) 16);

        // Encrypt key
        aesCipher.init(aesKek, Cipher.MODE_ENCRYPT, iv, (short) 0, (short) 16);
        aesCipher.doFinal(plainKey, (short) 0, keyLen, wrappedKey, (short) 16);

        aesKek.clearKey();
    }

    private boolean unwrapKey(byte[] wrappedKey, byte[] kek, byte[] plainKey) {
        try {
            AESKey aesKek = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, (short) (kek.length * 8), false);
            aesKek.setKey(kek, (short) 0);

            // Extract IV
            byte[] iv = new byte[16];
            Util.arrayCopy(wrappedKey, (short) 0, iv, (short) 0, (short) 16);

            // Decrypt key
            aesCipher.init(aesKek, Cipher.MODE_DECRYPT, iv, (short) 0, (short) 16);
            aesCipher.doFinal(wrappedKey, (short) 16, AES_KEY_SIZE, plainKey, (short) 0);

            aesKek.clearKey();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void encryptUserData(byte[] plainData) {
        // Encrypt user data with master key
        AESKey aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, (short) (AES_KEY_SIZE * 8), false);
        aesKey.setKey(masterKey, (short) 0);

        // Generate IV
        byte[] iv = new byte[16];
        randomGen.generateData(iv, (short) 0, (short) 16);

        // Store IV at the beginning
        Util.arrayCopy(iv, (short) 0, encryptedUserData, (short) 0, (short) 16);

        // Pad plaintext to block size
        short dataLen = (short) 160; // Fixed size for user data
        aesCipher.init(aesKey, Cipher.MODE_ENCRYPT, iv, (short) 0, (short) 16);
        aesCipher.doFinal(plainData, (short) 0, dataLen, encryptedUserData, (short) 16);

        aesKey.clearKey();
    }

    private void decryptUserData(byte[] plainData) {
        // Decrypt user data with master key
        AESKey aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, (short) (AES_KEY_SIZE * 8), false);
        aesKey.setKey(masterKey, (short) 0);

        // Extract IV
        byte[] iv = new byte[16];
        Util.arrayCopy(encryptedUserData, (short) 0, iv, (short) 0, (short) 16);

        // Decrypt
        aesCipher.init(aesKey, Cipher.MODE_DECRYPT, iv, (short) 0, (short) 16);
        aesCipher.doFinal(encryptedUserData, (short) 16, (short) 160, plainData, (short) 0);

        aesKey.clearKey();
    }

    private short findTag(byte[] data, byte tag) {
        short offset = 0;
        while (offset < (short) 160) {
            if (data[offset] == tag) {
                return offset;
            }
            // Skip to next TLV
            offset++;
            byte len = data[offset];
            offset++;
            offset += len;
        }
        return -1;
    }

    private boolean checkGameAccess(byte[] userData, byte gameID, short requiredCoins) {
        // Check if game is already bought
        short gamesOffset = findTag(userData, TAG_BOUGHT_GAMES);
        if (gamesOffset >= 0) {
            byte gameCount = userData[(short) (gamesOffset + 1)];
            for (byte i = 0; i < gameCount; i++) {
                if (userData[(short) (gamesOffset + 2 + i)] == gameID) {
                    return true; // Game already purchased
                }
            }
        }

        // Check coins
        short coinsOffset = findTag(userData, TAG_COINS);
        if (coinsOffset >= 0) {
            short coins = Util.getShort(userData, (short) (coinsOffset + 2));
            return coins >= requiredCoins;
        }

        return false;
    }

    private void deriveKeySimple(byte[] pin, short pinLen, byte[] salt, byte[] derivedKey) {
        // Simple hash-based KDF fallback when PBKDF2 not available
        // Concatenate PIN + salt and hash multiple times
        byte[] temp = new byte[(short)(pinLen + SALT_SIZE)];
        Util.arrayCopy(pin, (short) 0, temp, (short) 0, pinLen);
        Util.arrayCopy(salt, (short) 0, temp, pinLen, SALT_SIZE);
        
        if (sha256 != null) {
            // SHA-1 produces 20 bytes, but we only need 16 bytes for AES-128
            byte[] hashOutput = new byte[HASH_SIZE];
            sha256.reset();
            sha256.doFinal(temp, (short) 0, (short)(pinLen + SALT_SIZE), hashOutput, (short) 0);
            // Copy only the first 16 bytes for AES-128 key
            Util.arrayCopy(hashOutput, (short) 0, derivedKey, (short) 0, AES_KEY_SIZE);
            Util.arrayFillNonAtomic(hashOutput, (short) 0, HASH_SIZE, (byte) 0);
        } else {
            // Last resort: just copy and pad
            Util.arrayCopy(temp, (short) 0, derivedKey, (short) 0, 
                          (short)((pinLen + SALT_SIZE) < AES_KEY_SIZE ? (pinLen + SALT_SIZE) : AES_KEY_SIZE));
        }
        
        Util.arrayFillNonAtomic(temp, (short) 0, (short)temp.length, (byte) 0);
    }

}
