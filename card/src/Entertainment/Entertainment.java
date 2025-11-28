package Entertainment;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.*;

public class Entertainment extends Applet
{
	// INS codes
	private static final byte INS_GET_SALT = (byte)0x10;
	private static final byte INS_VERIFY_USER_PIN = (byte)0x20;
	private static final byte INS_VERIFY_ADMIN_PIN = (byte)0x21;
	private static final byte INS_CHANGE_USER_PIN = (byte)0x30;
	private static final byte INS_RESET_USER_PIN = (byte)0x31;
	private static final byte INS_GET_DATA = (byte)0x40;
	private static final byte INS_SET_DATA = (byte)0x50;
	private static final byte INS_INITIALIZE = (byte)0x60;
	
	// Custom status word for blocked authentication
	private static final short SW_AUTHENTICATION_METHOD_BLOCKED = (short)0x6983;
	
	// Constants
	private static final short SALT_LENGTH = 16;
	private static final short MASTER_KEY_LENGTH = 32;
	private static final short HASH_LENGTH = 32;
	private static final short MAX_DATA_LENGTH = 512;
	private static final short AES_BLOCK_SIZE = 16;
	private static final byte MAX_PIN_TRIES = 5;
	
	// EEPROM storage
	private byte[] saltUser;
	private byte[] saltAdmin;
	private byte[] encMUser;
	private byte[] encMAdmin;
	private byte[] hashM;
	private byte[] encData;
	private short encDataLength;
	
	// PIN counters
	private byte userPinTries;
	private byte adminPinTries;
	
	// RAM storage (volatile)
	private byte[] masterKeyRAM;
	private boolean userAuthenticated;
	private boolean adminAuthenticated;
	
	// Crypto objects
	private AESKey aesKey;
	private Cipher aesCipher;
	private MessageDigest sha256;
	private RandomData random;
	
	// Temporary buffers
	private byte[] tempBuffer;
	private byte[] ivBuffer;
	
	public static void install(byte[] bArray, short bOffset, byte bLength) 
	{
		new Entertainment().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
	}
	
	private Entertainment()
	{
		// Allocate EEPROM storage
		saltUser = new byte[SALT_LENGTH];
		saltAdmin = new byte[SALT_LENGTH];
		encMUser = new byte[MASTER_KEY_LENGTH];
		encMAdmin = new byte[MASTER_KEY_LENGTH];
		hashM = new byte[HASH_LENGTH];
		encData = new byte[MAX_DATA_LENGTH];
		encDataLength = 0;
		
		// Initialize PIN counters
		userPinTries = MAX_PIN_TRIES;
		adminPinTries = MAX_PIN_TRIES;
		
		// Allocate RAM storage
		masterKeyRAM = JCSystem.makeTransientByteArray(MASTER_KEY_LENGTH, JCSystem.CLEAR_ON_DESELECT);
		
		// Allocate temporary buffers
		tempBuffer = JCSystem.makeTransientByteArray((short)64, JCSystem.CLEAR_ON_DESELECT);
		ivBuffer = JCSystem.makeTransientByteArray(AES_BLOCK_SIZE, JCSystem.CLEAR_ON_DESELECT);
		
		// Initialize crypto objects
		aesKey = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_256, false);
		aesCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		sha256 = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
		random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
		
		// Initialize authentication flags
		userAuthenticated = false;
		adminAuthenticated = false;
	}

	public void process(APDU apdu)
	{
		if (selectingApplet())
		{
			// Clear authentication and wipe master key on deselect
			userAuthenticated = false;
			adminAuthenticated = false;
			wipeMasterKey();
			return;
		}

		byte[] buf = apdu.getBuffer();
		byte ins = buf[ISO7816.OFFSET_INS];
		
		switch (ins)
		{
			case INS_INITIALIZE:
				initialize(apdu);
				break;
			case INS_GET_SALT:
				getSalt(apdu);
				break;
			case INS_VERIFY_USER_PIN:
				verifyUserPin(apdu);
				break;
			case INS_VERIFY_ADMIN_PIN:
				verifyAdminPin(apdu);
				break;
			case INS_CHANGE_USER_PIN:
				changeUserPin(apdu);
				break;
			case INS_RESET_USER_PIN:
				resetUserPin(apdu);
				break;
			case INS_GET_DATA:
				getData(apdu);
				break;
			case INS_SET_DATA:
				setData(apdu);
				break;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}
	
	/**
	 * Initialize the card with Master Key and initial KEKs
	 * Input: KEK_user(32) + Salt_user(16) + KEK_admin(32) + Salt_admin(16)
	 */
	private void initialize(APDU apdu)
	{
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		short totalLen = (short)(MASTER_KEY_LENGTH * 2 + SALT_LENGTH * 2);
		
		// Receive all data
		short offset = ISO7816.OFFSET_CDATA;
		while (receivedLen < totalLen)
		{
			receivedLen += apdu.receiveBytes(offset);
			offset += receivedLen;
		}
		
		if (receivedLen != totalLen)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// Generate random Master Key M
		random.generateData(masterKeyRAM, (short)0, MASTER_KEY_LENGTH);
		
		// Calculate SHA256(M)
		sha256.reset();
		sha256.doFinal(masterKeyRAM, (short)0, MASTER_KEY_LENGTH, hashM, (short)0);
		
		// Parse input data
		short pos = ISO7816.OFFSET_CDATA;
		
		// KEK_user
		byte[] kekUser = tempBuffer;
		Util.arrayCopy(buf, pos, kekUser, (short)0, MASTER_KEY_LENGTH);
		pos += MASTER_KEY_LENGTH;
		
		// Salt_user
		Util.arrayCopy(buf, pos, saltUser, (short)0, SALT_LENGTH);
		pos += SALT_LENGTH;
		
		// KEK_admin
		byte[] kekAdmin = new byte[MASTER_KEY_LENGTH];
		Util.arrayCopy(buf, pos, kekAdmin, (short)0, MASTER_KEY_LENGTH);
		pos += MASTER_KEY_LENGTH;
		
		// Salt_admin
		Util.arrayCopy(buf, pos, saltAdmin, (short)0, SALT_LENGTH);
		
		// Encrypt M with KEK_user
		encryptMasterKey(kekUser, masterKeyRAM, encMUser);
		
		// Encrypt M with KEK_admin
		encryptMasterKey(kekAdmin, masterKeyRAM, encMAdmin);
		
		// Clear sensitive data
		Util.arrayFillNonAtomic(kekUser, (short)0, MASTER_KEY_LENGTH, (byte)0);
		Util.arrayFillNonAtomic(kekAdmin, (short)0, MASTER_KEY_LENGTH, (byte)0);
		
		// Keep M in RAM for this session
		userAuthenticated = false;
		adminAuthenticated = false;
	}
	
	/**
	 * Get SALT_user and SALT_admin
	 * Output: Salt_user(16) + Salt_admin(16)
	 */
	private void getSalt(APDU apdu)
	{
		byte[] buf = apdu.getBuffer();
		short pos = 0;
		
		Util.arrayCopy(saltUser, (short)0, buf, pos, SALT_LENGTH);
		pos += SALT_LENGTH;
		
		Util.arrayCopy(saltAdmin, (short)0, buf, pos, SALT_LENGTH);
		pos += SALT_LENGTH;
		
		apdu.setOutgoingAndSend((short)0, pos);
	}
	
	/**
	 * Verify user PIN by decrypting Enc_M_user with KEK_user
	 * Input: KEK_user(32)
	 */
	private void verifyUserPin(APDU apdu)
	{
		if (userPinTries == 0)
			ISOException.throwIt(SW_AUTHENTICATION_METHOD_BLOCKED);
		
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		
		if (receivedLen != MASTER_KEY_LENGTH)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// KEK_user from host
		byte[] kekUser = tempBuffer;
		Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, kekUser, (short)0, MASTER_KEY_LENGTH);
		
		// Try to decrypt Enc_M_user
		boolean success = decryptMasterKey(kekUser, encMUser, masterKeyRAM);
		
		// Clear KEK from memory
		Util.arrayFillNonAtomic(kekUser, (short)0, MASTER_KEY_LENGTH, (byte)0);
		
		if (success)
		{
			// Verify hash
			sha256.reset();
			sha256.doFinal(masterKeyRAM, (short)0, MASTER_KEY_LENGTH, tempBuffer, (short)0);
			
			if (Util.arrayCompare(tempBuffer, (short)0, hashM, (short)0, HASH_LENGTH) == 0)
			{
				// Authentication successful
				userAuthenticated = true;
				userPinTries = MAX_PIN_TRIES;
			}
			else
			{
				// Hash mismatch
				wipeMasterKey();
				userPinTries--;
				ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
			}
		}
		else
		{
			// Decryption failed
			userPinTries--;
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
	}
	
	/**
	 * Verify admin PIN by decrypting Enc_M_admin with KEK_admin
	 * Input: KEK_admin(32)
	 */
	private void verifyAdminPin(APDU apdu)
	{
		if (adminPinTries == 0)
			ISOException.throwIt(SW_AUTHENTICATION_METHOD_BLOCKED);
		
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		
		if (receivedLen != MASTER_KEY_LENGTH)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// KEK_admin from host
		byte[] kekAdmin = tempBuffer;
		Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, kekAdmin, (short)0, MASTER_KEY_LENGTH);
		
		// Try to decrypt Enc_M_admin
		boolean success = decryptMasterKey(kekAdmin, encMAdmin, masterKeyRAM);
		
		// Clear KEK from memory
		Util.arrayFillNonAtomic(kekAdmin, (short)0, MASTER_KEY_LENGTH, (byte)0);
		
		if (success)
		{
			// Verify hash
			sha256.reset();
			sha256.doFinal(masterKeyRAM, (short)0, MASTER_KEY_LENGTH, tempBuffer, (short)0);
			
			if (Util.arrayCompare(tempBuffer, (short)0, hashM, (short)0, HASH_LENGTH) == 0)
			{
				// Authentication successful
				adminAuthenticated = true;
				adminPinTries = MAX_PIN_TRIES;
			}
			else
			{
				// Hash mismatch
				wipeMasterKey();
				adminPinTries--;
				ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
			}
		}
		else
		{
			// Decryption failed
			adminPinTries--;
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		}
	}
	
	/**
	 * Change user PIN (requires user authentication)
	 * Input: KEK_new_user(32) + Salt_new_user(16)
	 */
	private void changeUserPin(APDU apdu)
	{
		if (!userAuthenticated)
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		short expectedLen = (short)(MASTER_KEY_LENGTH + SALT_LENGTH);
		
		if (receivedLen != expectedLen)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// Parse input
		short pos = ISO7816.OFFSET_CDATA;
		byte[] kekNewUser = tempBuffer;
		Util.arrayCopy(buf, pos, kekNewUser, (short)0, MASTER_KEY_LENGTH);
		pos += MASTER_KEY_LENGTH;
		
		// Update salt
		Util.arrayCopy(buf, pos, saltUser, (short)0, SALT_LENGTH);
		
		// Re-encrypt M with new KEK
		encryptMasterKey(kekNewUser, masterKeyRAM, encMUser);
		
		// Clear KEK from memory
		Util.arrayFillNonAtomic(kekNewUser, (short)0, MASTER_KEY_LENGTH, (byte)0);
	}
	
	/**
	 * Reset user PIN (requires admin authentication)
	 * Input: KEK_new_user(32) + Salt_new_user(16)
	 */
	private void resetUserPin(APDU apdu)
	{
		if (!adminAuthenticated)
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		short expectedLen = (short)(MASTER_KEY_LENGTH + SALT_LENGTH);
		
		if (receivedLen != expectedLen)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// Parse input
		short pos = ISO7816.OFFSET_CDATA;
		byte[] kekNewUser = tempBuffer;
		Util.arrayCopy(buf, pos, kekNewUser, (short)0, MASTER_KEY_LENGTH);
		pos += MASTER_KEY_LENGTH;
		
		// Update salt
		Util.arrayCopy(buf, pos, saltUser, (short)0, SALT_LENGTH);
		
		// Re-encrypt M with new KEK
		encryptMasterKey(kekNewUser, masterKeyRAM, encMUser);
		
		// Reset user PIN counter
		userPinTries = MAX_PIN_TRIES;
		
		// Clear KEK from memory
		Util.arrayFillNonAtomic(kekNewUser, (short)0, MASTER_KEY_LENGTH, (byte)0);
	}
	
	/**
	 * Get decrypted data (requires authentication)
	 * Output: Decrypted data
	 */
	private void getData(APDU apdu)
	{
		if (!userAuthenticated && !adminAuthenticated)
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		
		if (encDataLength == 0)
			ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
		
		byte[] buf = apdu.getBuffer();
		
		// Decrypt data using M
		short decryptedLen = decryptData(masterKeyRAM, encData, (short)0, encDataLength, buf, (short)0);
		
		apdu.setOutgoingAndSend((short)0, decryptedLen);
	}
	
	/**
	 * Set encrypted data (requires authentication)
	 * Input: Plaintext data
	 */
	private void setData(APDU apdu)
	{
		if (!userAuthenticated && !adminAuthenticated)
			ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
		
		byte[] buf = apdu.getBuffer();
		short receivedLen = apdu.setIncomingAndReceive();
		
		// Receive all data
		short offset = ISO7816.OFFSET_CDATA;
		short totalLen = (short)(buf[ISO7816.OFFSET_LC] & 0xFF);
		
		while (receivedLen < totalLen)
		{
			short len = apdu.receiveBytes((short)(ISO7816.OFFSET_CDATA + receivedLen));
			receivedLen += len;
		}
		
		if (totalLen > MAX_DATA_LENGTH)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
		
		// Pad data to AES block size
		short paddedLen = (short)((totalLen + AES_BLOCK_SIZE - 1) / AES_BLOCK_SIZE * AES_BLOCK_SIZE);
		
		// Encrypt data using M
		encDataLength = encryptData(masterKeyRAM, buf, ISO7816.OFFSET_CDATA, totalLen, encData, (short)0, paddedLen);
	}
	
	/**
	 * Encrypt Master Key M with KEK
	 * @param kek Key Encryption Key (32 bytes)
	 * @param masterKey Master Key to encrypt (32 bytes)
	 * @param output Output buffer for encrypted M (32 bytes)
	 */
	private void encryptMasterKey(byte[] kek, byte[] masterKey, byte[] output)
	{
		// Set AES key
		aesKey.setKey(kek, (short)0);
		aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
		
		// Generate random IV
		random.generateData(ivBuffer, (short)0, AES_BLOCK_SIZE);
		
		// Encrypt M (32 bytes = 2 blocks)
		aesCipher.doFinal(masterKey, (short)0, MASTER_KEY_LENGTH, output, (short)0);
	}
	
	/**
	 * Decrypt Master Key M with KEK
	 * @param kek Key Encryption Key (32 bytes)
	 * @param encMaster Encrypted Master Key (32 bytes)
	 * @param output Output buffer for decrypted M (32 bytes)
	 * @return true if decryption successful, false otherwise
	 */
	private boolean decryptMasterKey(byte[] kek, byte[] encMaster, byte[] output)
	{
		try
		{
			// Set AES key
			aesKey.setKey(kek, (short)0);
			aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
			
			// Decrypt M (32 bytes = 2 blocks)
			aesCipher.doFinal(encMaster, (short)0, MASTER_KEY_LENGTH, output, (short)0);
			
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Encrypt data with Master Key M
	 * @param masterKey Master Key (32 bytes)
	 * @param plaintext Input plaintext
	 * @param ptOffset Plaintext offset
	 * @param ptLen Plaintext length
	 * @param ciphertext Output ciphertext buffer
	 * @param ctOffset Ciphertext offset
	 * @param paddedLen Padded length (must be multiple of 16)
	 * @return Length of encrypted data
	 */
	private short encryptData(byte[] masterKey, byte[] plaintext, short ptOffset, short ptLen, byte[] ciphertext, short ctOffset, short paddedLen)
	{
		// Copy data to temp buffer and pad
		Util.arrayFillNonAtomic(tempBuffer, (short)0, paddedLen, (byte)0);
		Util.arrayCopy(plaintext, ptOffset, tempBuffer, (short)0, ptLen);
		
		// Set AES key
		aesKey.setKey(masterKey, (short)0);
		aesCipher.init(aesKey, Cipher.MODE_ENCRYPT);
		
		// Encrypt data
		aesCipher.doFinal(tempBuffer, (short)0, paddedLen, ciphertext, ctOffset);
		
		return paddedLen;
	}
	
	/**
	 * Decrypt data with Master Key M
	 * @param masterKey Master Key (32 bytes)
	 * @param ciphertext Input ciphertext
	 * @param ctOffset Ciphertext offset
	 * @param ctLen Ciphertext length
	 * @param plaintext Output plaintext buffer
	 * @param ptOffset Plaintext offset
	 * @return Length of decrypted data
	 */
	private short decryptData(byte[] masterKey, byte[] ciphertext, short ctOffset, short ctLen, byte[] plaintext, short ptOffset)
	{
		// Set AES key
		aesKey.setKey(masterKey, (short)0);
		aesCipher.init(aesKey, Cipher.MODE_DECRYPT);
		
		// Decrypt data
		short decryptedLen = aesCipher.doFinal(ciphertext, ctOffset, ctLen, plaintext, ptOffset);
		
		return decryptedLen;
	}
	
	/**
	 * Wipe Master Key from RAM
	 */
	private void wipeMasterKey()
	{
		Util.arrayFillNonAtomic(masterKeyRAM, (short)0, MASTER_KEY_LENGTH, (byte)0);
	}
}
