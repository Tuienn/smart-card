# Entertainment JavaCard Applet - API Documentation

## Kiến trúc mã hóa dữ liệu trong thẻ

### Tổng quan
Hệ thống sử dụng **3-layer encryption architecture** để bảo vệ dữ liệu:
1. **PIN Layer** (PBKDF2 + AES Key Wrap) - Bảo vệ Master Key
2. **Data Layer** (AES-CBC) - Mã hóa dữ liệu user
3. **Image Layer** (AES-CBC streaming) - Mã hóa ảnh đại diện

---

## LUỒNG 1: Khởi tạo thẻ và sinh Master Key

### Mục đích
Tạo cấu trúc mã hóa ban đầu khi thẻ được cấp phát lần đầu

### Input
- User PIN (4-16 bytes)
- User ID (16 bytes)

### Quy trình chi tiết

```
┌─────────────────────────────────────────────────────────────┐
│  1. SINH CÁC THÀNH PHẦN NGẪU NHIÊN                          │
└─────────────────────────────────────────────────────────────┘
    ↓
    randomGen.generateData(salt, 16 bytes)          // Salt cho PBKDF2
    randomGen.generateData(masterKey, 16 bytes)     // Master Key (AES-128)
    
┌─────────────────────────────────────────────────────────────┐
│  2. TẠO HASH CỦA MASTER KEY (ĐỂ VERIFY SAU NÀY)            │
└─────────────────────────────────────────────────────────────┘
    ↓
    SHA1(masterKey) → masterKeyHash (20 bytes)
    
┌─────────────────────────────────────────────────────────────┐
│  3. DERIVE KEK TỪ USER PIN (Key Encryption Key)            │
└─────────────────────────────────────────────────────────────┘
    ↓
    PBKDF2-HMAC-SHA256(
        password: userPIN,
        salt: salt,
        iterations: 500,
        outputLen: 16 bytes
    ) → userKEK
    
┌─────────────────────────────────────────────────────────────┐
│  4. WRAP MASTER KEY BẰNG USER KEK                           │
└─────────────────────────────────────────────────────────────┘
    ↓
    iv_user = random(16 bytes)
    AES-CBC-Encrypt(masterKey, key=userKEK, iv=iv_user)
    wrappedMasterKey = [iv_user(16) | encrypted_key(16)]  // 32 bytes
    
┌─────────────────────────────────────────────────────────────┐
│  5. TẠO ADMIN KEK VÀ WRAP MASTER KEY CHO ADMIN              │
└─────────────────────────────────────────────────────────────┘
    ↓
    adminPIN = "1234567890123456" (mặc định)
    PBKDF2-HMAC-SHA256(adminPIN, salt, 500, 16) → adminKEK
    
    iv_admin = random(16 bytes)
    AES-CBC-Encrypt(masterKey, key=adminKEK, iv=iv_admin)
    adminWrappedMasterKey = [iv_admin(16) | encrypted_key(16)]
    
┌─────────────────────────────────────────────────────────────┐
│  6. SINH RSA KEYPAIR (1024-bit)                             │
└─────────────────────────────────────────────────────────────┘
    ↓
    rsaKeyPair.genKeyPair()
    → rsaPrivateKey (lưu trong thẻ)
    → rsaPublicKey (trả về cho backend)
    
┌─────────────────────────────────────────────────────────────┐
│  7. KHỞI TẠO DỮ LIỆU USER RỖNG VÀ MÃ HÓA                    │
└─────────────────────────────────────────────────────────────┘
    ↓
    plainUserData = TLV{
        TAG_NAME: "",
        TAG_GENDER: 0,
        TAG_COINS: 0,
        TAG_BOUGHT_GAMES: [],
        TAG_AGE: 0
    } (160 bytes fixed)
    
    encryptUserData(plainUserData) → encryptedUserData

┌─────────────────────────────────────────────────────────────┐
│  8. XÓA DỮ LIỆU NHẠY CẢM KHỎI BỘ NHỚ                        │
└─────────────────────────────────────────────────────────────┘
    ↓
    arrayFillNonAtomic(userKEK, 0)
    arrayFillNonAtomic(adminKEK, 0)
    arrayFillNonAtomic(adminPIN, 0)
    arrayFillNonAtomic(userPIN, 0)
    
    // Master Key vẫn ở trong transient memory cho session
```

### Output lưu persistent
- `salt` (16 bytes) - Dùng cho PBKDF2
- `wrappedMasterKey` (32 bytes) - Master key được wrap bởi user KEK
- `adminWrappedMasterKey` (32 bytes) - Master key được wrap bởi admin KEK
- `masterKeyHash` (20 bytes) - SHA1(masterKey) để verify
- `encryptedUserData` (256 bytes) - Dữ liệu user đã mã hóa
- `rsaPrivateKey`, `rsaPublicKey` - RSA keypair

### Tại sao dùng PBKDF2?
✅ **Chống brute-force attack:** 500 iterations làm chậm việc thử PIN
✅ **Derive key từ password:** Chuyển PIN thành AES key hợp lệ
✅ **Salt ngẫu nhiên:** Mỗi thẻ có salt khác nhau → cùng PIN cũng cho KEK khác nhau

---

## LUỒNG 2: Xác thực PIN và Unwrap Master Key

### Mục đích
Mở session authenticated để truy cập dữ liệu đã mã hóa

### Input
- User PIN (từ client)

### Quy trình chi tiết

```
┌─────────────────────────────────────────────────────────────┐
│  1. DERIVE KEK TỪ PIN NHẬP VÀO                              │
└─────────────────────────────────────────────────────────────┘
    ↓
    PBKDF2-HMAC-SHA256(
        password: inputPIN,
        salt: salt (stored),
        iterations: 500,
        outputLen: 16
    ) → candidateKEK
    
┌─────────────────────────────────────────────────────────────┐
│  2. UNWRAP MASTER KEY BẰNG CANDIDATE KEK                    │
└─────────────────────────────────────────────────────────────┘
    ↓
    Extract: iv = wrappedMasterKey[0..15]
             encryptedKey = wrappedMasterKey[16..31]
    
    AES-CBC-Decrypt(
        ciphertext: encryptedKey,
        key: candidateKEK,
        iv: iv
    ) → candidateMasterKey
    
┌─────────────────────────────────────────────────────────────┐
│  3. VERIFY MASTER KEY HASH                                  │
└─────────────────────────────────────────────────────────────┘
    ↓
    computedHash = SHA1(candidateMasterKey)
    
    if (computedHash == masterKeyHash):
        ✅ PIN đúng
        masterKey = candidateMasterKey  // Lưu vào transient memory
        sessionAuth = true
        pinTryCounter = 3
    else:
        ❌ PIN sai
        pinTryCounter--
        if (pinTryCounter == 0):
            lockedFlag = true
            throw SW_AUTHENTICATION_BLOCKED
        else:
            throw 0x63C0 | pinTryCounter
    
┌─────────────────────────────────────────────────────────────┐
│  4. XÓA DỮ LIỆU TẠM                                         │
└─────────────────────────────────────────────────────────────┘
    ↓
    arrayFillNonAtomic(candidateKEK, 0)
    arrayFillNonAtomic(inputPIN, 0)
```

### Tại sao verify hash thay vì chỉ dùng KEK?
✅ **Double verification:** KEK có thể decrypt nhưng sai PIN vẫn cho masterKey sai
✅ **Integrity check:** Đảm bảo masterKey không bị corrupt
✅ **Security:** Ngăn ngừa chosen-ciphertext attack

---

## LUỒNG 3: Đổi PIN (User tự đổi)

### Mục đích
User thay đổi PIN của mình mà không ảnh hưởng đến dữ liệu đã mã hóa

### Input
- Old PIN (để verify)
- New PIN

### Yêu cầu tiên quyết
✅ `sessionAuth == true` (đã xác thực trước đó)

### Quy trình chi tiết

```
┌─────────────────────────────────────────────────────────────┐
│  1. RE-VERIFY OLD PIN (DOUBLE SECURITY CHECK)               │
└─────────────────────────────────────────────────────────────┘
    ↓
    PBKDF2-HMAC-SHA256(oldPIN, salt, 500, 16) → oldKEK
    
    Unwrap: AES-CBC-Decrypt(wrappedMasterKey, oldKEK, iv)
            → tempMasterKey
    
    if (SHA1(tempMasterKey) != masterKeyHash):
        ❌ Old PIN sai
        throw SW_SECURITY_STATUS_NOT_SATISFIED
        
┌─────────────────────────────────────────────────────────────┐
│  2. DERIVE NEW KEK TỪ NEW PIN                               │
└─────────────────────────────────────────────────────────────┘
    ↓
    PBKDF2-HMAC-SHA256(newPIN, salt, 500, 16) → newKEK
    
┌─────────────────────────────────────────────────────────────┐
│  3. RE-WRAP MASTER KEY BẰNG NEW KEK                         │
└─────────────────────────────────────────────────────────────┘
    ↓
    newIV = random(16 bytes)
    encryptedKey = AES-CBC-Encrypt(tempMasterKey, newKEK, newIV)
    wrappedMasterKey = [newIV | encryptedKey]  // Overwrite old one
    
┌─────────────────────────────────────────────────────────────┐
│  4. UPDATE SESSION MASTER KEY                               │
└─────────────────────────────────────────────────────────────┘
    ↓
    masterKey = tempMasterKey  // Session vẫn authenticated
    
┌─────────────────────────────────────────────────────────────┐
│  5. CLEAN UP                                                │
└─────────────────────────────────────────────────────────────┘
    ↓
    arrayFillNonAtomic(oldKEK, 0)
    arrayFillNonAtomic(newKEK, 0)
    arrayFillNonAtomic(oldPIN, 0)
    arrayFillNonAtomic(newPIN, 0)
    arrayFillNonAtomic(tempMasterKey, 0)
```

### Điểm quan trọng
✅ **Không thay đổi masterKey** → Dữ liệu đã mã hóa vẫn giữ nguyên
✅ **Chỉ re-wrap masterKey** với KEK mới từ new PIN
✅ **Session không bị mất** sau khi đổi PIN
✅ **encryptedUserData không đổi** → Không cần decrypt/re-encrypt

---

## LUỒNG 4: Unlock PIN bởi Admin (Emergency Recovery)

### Mục đích
Admin reset user PIN khi user quên PIN hoặc bị lock

### Input
- New user PIN (optional)

### Yêu cầu tiên quyết
✅ `adminSessionAuth == true` (đã xác thực admin PIN)

### Quy trình chi tiết

```
┌─────────────────────────────────────────────────────────────┐
│  1. UNWRAP MASTER KEY BẰNG ADMIN KEK                        │
└─────────────────────────────────────────────────────────────┘
    ↓
    // Admin đã xác thực → adminSessionAuth = true
    // Master key đã unwrap từ adminWrappedMasterKey
    // Có quyền truy cập masterKey trong transient memory
    
┌─────────────────────────────────────────────────────────────┐
│  2. DERIVE NEW USER KEK (NẾU CÓ NEW PIN)                    │
└─────────────────────────────────────────────────────────────┘
    ↓
    if (newPinLength > 0):
        PBKDF2-HMAC-SHA256(newPIN, salt, 500, 16) → newUserKEK
        
        newIV = random(16)
        encryptedKey = AES-CBC-Encrypt(masterKey, newUserKEK, newIV)
        wrappedMasterKey = [newIV | encryptedKey]
        
        arrayFillNonAtomic(newUserKEK, 0)
        arrayFillNonAtomic(newPIN, 0)
        
┌─────────────────────────────────────────────────────────────┐
│  3. RESET USER PIN COUNTER                                  │
└─────────────────────────────────────────────────────────────┘
    ↓
    pinTryCounter = 3
    lockedFlag = false
```

### So sánh với CHANGE_PIN

| Đặc điểm | CHANGE_PIN (0x23) | UNLOCK_BY_ADMIN (0x21) |
|----------|-------------------|------------------------|
| Yêu cầu auth | User PIN | Admin PIN |
| Verify old user PIN | ✅ Bắt buộc | ❌ Không cần |
| Use case | Đổi PIN thường xuyên | Emergency recovery |
| Reset counter | ❌ Không | ✅ Có (reset về 3) |
| Unwrap từ | wrappedMasterKey | adminWrappedMasterKey |

---

## LUỒNG 5: Mã hóa dữ liệu User (TLV format)

### Mục đích
Mã hóa dữ liệu user (name, gender, coins, bought games) bằng Master Key

### Cấu trúc TLV (Tag-Length-Value)

```
Plain User Data (160 bytes fixed):
┌──────┬────────┬───────────────────────┐
│ Tag  │ Length │       Value           │
├──────┼────────┼───────────────────────┤
│ 0x01 │   N    │ Name (N bytes)        │
│ 0x02 │   1    │ Gender (1 byte)       │
│ 0x03 │   2    │ Coins (2 bytes BE)    │
│ 0x04 │   M    │ Bought games (M bytes)│
│ 0x05 │   1    │ Age (1 byte)          │
└──────┴────────┴───────────────────────┘
```

### Quy trình mã hóa

```
┌─────────────────────────────────────────────────────────────┐
│  1. SINH IV NGẪU NHIÊN                                      │
└─────────────────────────────────────────────────────────────┘
    ↓
    randomGen.generateData(iv, 16 bytes)
    
┌─────────────────────────────────────────────────────────────┐
│  2. MÃ HÓA BẰNG AES-CBC                                     │
└─────────────────────────────────────────────────────────────┘
    ↓
    AESKey.setKey(masterKey, 16 bytes)
    aesCipher.init(AESKey, MODE_ENCRYPT, iv, 16)
    
    ciphertext = aesCipher.doFinal(
        plainUserData,  // 160 bytes TLV
        0,
        160
    )
    
┌─────────────────────────────────────────────────────────────┐
│  3. LƯU [IV | CIPHERTEXT]                                   │
└─────────────────────────────────────────────────────────────┘
    ↓
    encryptedUserData[0..15] = iv
    encryptedUserData[16..175] = ciphertext
    
    Total size: 176 bytes (trong MAX_ENCRYPTED_DATA_SIZE = 256)
```

### Quy trình giải mã

```
┌─────────────────────────────────────────────────────────────┐
│  1. EXTRACT IV                                              │
└─────────────────────────────────────────────────────────────┘
    ↓
    iv = encryptedUserData[0..15]
    
┌─────────────────────────────────────────────────────────────┐
│  2. GIẢI MÃ BẰNG MASTER KEY                                 │
└─────────────────────────────────────────────────────────────┘
    ↓
    AESKey.setKey(masterKey, 16)  // masterKey từ session
    aesCipher.init(AESKey, MODE_DECRYPT, iv, 16)
    
    plainUserData = aesCipher.doFinal(
        encryptedUserData,
        16,   // Skip IV
        160   // Decrypt 160 bytes
    )
    
┌─────────────────────────────────────────────────────────────┐
│  3. PARSE TLV                                               │
└─────────────────────────────────────────────────────────────┘
    ↓
    offset = 0
    while (offset < 160):
        tag = plainUserData[offset++]
        length = plainUserData[offset++]
        value = plainUserData[offset .. offset+length]
        offset += length
```

### Tại sao dùng fixed 160 bytes?
✅ **Prevent size-based leakage:** Attacker không biết user có bao nhiêu data
✅ **Simplified encryption:** Luôn là bội số 16 (AES block size)
✅ **Memory efficiency:** Không cần dynamic allocation

---

## LUỒNG 6: Mã hóa Image (AES-CBC Streaming)

### Đặc điểm
- **Max size:** 32KB
- **Streaming:** Nhận từng chunk, mã hóa dần (không đợi toàn bộ image)
- **PKCS#7 Padding:** Tự động thêm padding cho block cuối

### Quy trình mã hóa (Multi-chunk)

#### Bước 1: Write Image Start (INS 0x52)

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT                                                      │
└─────────────────────────────────────────────────────────────┘
    totalImageSize (2 bytes)
    imageType (1 byte)
    firstChunkData (N bytes)

┌─────────────────────────────────────────────────────────────┐
│  1. SINH IV VÀ KHỞI TẠO CIPHER (CHỈ 1 LẦN)                 │
└─────────────────────────────────────────────────────────────┘
    ↓
    randomGen.generateData(imageIV, 16)
    
    AESKey.setKey(masterKey, 16)
    aesCipher.init(AESKey, MODE_ENCRYPT, imageIV, 16)  // ⚠️ Chỉ init 1 lần!
    
    currentWriteOffset = 0
    tempChunkLen = 0
    
┌─────────────────────────────────────────────────────────────┐
│  2. MÃ HÓA CHUNK ĐẦU TIÊN                                   │
└─────────────────────────────────────────────────────────────┘
    ↓
    encryptImageChunk(firstChunkData, chunkLen)
    // → Xem chi tiết function bên dưới
```

#### Bước 2: Write Image Continue (INS 0x53) - Gọi nhiều lần

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT                                                      │
└─────────────────────────────────────────────────────────────┘
    offset (2 bytes) - Vị trí trong image gốc
    chunkData (N bytes)

┌─────────────────────────────────────────────────────────────┐
│  1. MÃ HÓA CHUNK TIẾP THEO (KHÔNG RE-INIT CIPHER!)         │
└─────────────────────────────────────────────────────────────┘
    ↓
    encryptImageChunk(chunkData, chunkLen)
    // ⚠️ Cipher vẫn giữ CBC chain từ lần init đầu tiên
    
┌─────────────────────────────────────────────────────────────┐
│  2. AUTO-FINALIZE KHI ĐỦ DATA                               │
└─────────────────────────────────────────────────────────────┘
    ↓
    if (totalReceived >= totalImageSize):
        // Apply PKCS#7 padding
        paddingLen = 16 - tempChunkLen
        for (i = tempChunkLen; i < 16; i++):
            tempImageChunk[i] = paddingLen
        
        // Encrypt final block
        aesCipher.doFinal(tempImageChunk, 16, imageBuffer, currentWriteOffset)
        currentWriteOffset += 16
        
        encryptedImageSize = currentWriteOffset
        imageSize = encryptedImageSize
```

#### Function: encryptImageChunk (Streaming Encryption Core)

```
┌─────────────────────────────────────────────────────────────┐
│  LOGIC XỬ LÝ PARTIAL BLOCKS                                 │
└─────────────────────────────────────────────────────────────┘

INPUT: sourceData, sourceOffset, dataLength
IMPORTANT: Cipher đã được init sẵn, KHÔNG re-init trong function này!

    ↓
    
┌─────────────────────────────────────────────────────────────┐
│  1. XỬ LÝ REMAINDER TỪ CHUNK TRƯỚC (NẾU CÓ)                │
└─────────────────────────────────────────────────────────────┘
    ↓
    if (tempChunkLen > 0):
        needed = 16 - tempChunkLen
        
        if (dataLength >= needed):
            // Đủ để hoàn thành block 16-byte
            arrayCopy(sourceData, sourceOffset, tempImageChunk, tempChunkLen, needed)
            
            aesCipher.update(tempImageChunk, 0, 16, imageBuffer, currentWriteOffset)
            currentWriteOffset += 16
            
            chunkOffset = needed
            tempChunkLen = 0
        else:
            // Chưa đủ, chỉ accumulate
            arrayCopy(sourceData, sourceOffset, tempImageChunk, tempChunkLen, dataLength)
            tempChunkLen += dataLength
            return  // Đợi chunk tiếp theo
            
┌─────────────────────────────────────────────────────────────┐
│  2. MÃ HÓA TẤT CẢ FULL 16-BYTE BLOCKS                       │
└─────────────────────────────────────────────────────────────┘
    ↓
    while (chunkOffset + 16 <= dataLength):
        aesCipher.update(
            sourceData,
            sourceOffset + chunkOffset,
            16,
            imageBuffer,
            currentWriteOffset
        )
        currentWriteOffset += 16
        chunkOffset += 16
        
┌─────────────────────────────────────────────────────────────┐
│  3. LƯU REMAINDER MỚI (< 16 BYTES)                          │
└─────────────────────────────────────────────────────────────┘
    ↓
    tempChunkLen = dataLength - chunkOffset
    if (tempChunkLen > 0):
        arrayCopy(sourceData, sourceOffset + chunkOffset, tempImageChunk, 0, tempChunkLen)
```

### Ví dụ cụ thể: Mã hóa image 50 bytes qua 3 chunks

```
┌─────────────────────────────────────────────────────────────┐
│  CHUNK 1: 20 bytes                                          │
└─────────────────────────────────────────────────────────────┘
Input: [A0 A1 ... A19]  (20 bytes)
tempChunkLen = 0

→ Encrypt full block: [A0..A15] → imageBuffer[0..15]
  currentWriteOffset = 16
  tempChunkLen = 4
  tempImageChunk = [A16 A17 A18 A19 ?? ?? ...]

┌─────────────────────────────────────────────────────────────┐
│  CHUNK 2: 25 bytes                                          │
└─────────────────────────────────────────────────────────────┘
Input: [B0 B1 ... B24]  (25 bytes)
tempChunkLen = 4 (còn [A16..A19])

→ Hoàn thành block: [A16 A17 A18 A19 B0 B1 ... B11]
  Encrypt → imageBuffer[16..31]
  currentWriteOffset = 32
  
→ Encrypt full block: [B12 B13 ... B27] → imageBuffer[32..47]
  currentWriteOffset = 48
  tempChunkLen = 1
  tempImageChunk = [B28 ?? ?? ...]

┌─────────────────────────────────────────────────────────────┐
│  CHUNK 3: 5 bytes (FINAL)                                   │
└─────────────────────────────────────────────────────────────┘
Input: [C0 C1 C2 C3 C4]  (5 bytes)
tempChunkLen = 1 (còn [B28])
totalReceived = 20 + 25 + 5 = 50 ✅

→ Accumulate: tempImageChunk = [B28 C0 C1 C2 C3 C4 ?? ...]
  tempChunkLen = 6
  
→ Auto-finalize:
  paddingLen = 16 - 6 = 10
  tempImageChunk = [B28 C0 C1 C2 C3 C4 0A 0A 0A 0A 0A 0A 0A 0A 0A 0A]
                                      └─────────────────────┘
                                         PKCS#7 padding
  
  aesCipher.doFinal(tempImageChunk, 16) → imageBuffer[48..63]
  currentWriteOffset = 64
  
  encryptedImageSize = 64
  actualImageSize = 50
  
Final encrypted image:
  imageBuffer = [encrypted_block_1 | encrypted_block_2 | encrypted_block_3 | encrypted_block_4]
                      16 bytes            16 bytes            16 bytes            16 bytes
```

### Quy trình giải mã Image

```
┌─────────────────────────────────────────────────────────────┐
│  INPUT (INS_READ_IMAGE 0x54)                                │
└─────────────────────────────────────────────────────────────┘
    requestedOffset (2 bytes) - Vị trí trong image GỐC (plaintext)
    requestedLength (2 bytes) - Số bytes cần đọc (plaintext)

┌─────────────────────────────────────────────────────────────┐
│  1. XÁC ĐỊNH ENCRYPTED BLOCKS CẦN DECRYPT                   │
└─────────────────────────────────────────────────────────────┘
    ↓
    startBlock = requestedOffset / 16
    endBlock = (requestedOffset + requestedLength - 1) / 16
    
    VD: Đọc bytes 18-35 (18 bytes từ vị trí 18)
        startBlock = 18 / 16 = 1
        endBlock = (18 + 18 - 1) / 16 = 35 / 16 = 2
        → Cần decrypt block 1 và block 2
        
┌─────────────────────────────────────────────────────────────┐
│  2. DECRYPT CÁC BLOCKS CẦN THIẾT                            │
└─────────────────────────────────────────────────────────────┘
    ↓
    AESKey.setKey(masterKey, 16)
    aesCipher.init(AESKey, MODE_DECRYPT, imageIV, 16)
    
    tempOffset = 0
    for (blockIdx = startBlock to endBlock):
        encryptedBlockOffset = blockIdx * 16
        aesCipher.update(
            imageBuffer, encryptedBlockOffset, 16,
            tempBuffer, tempOffset
        )
        tempOffset += 16
    
    // tempBuffer giờ chứa plaintext của các blocks cần thiết
    
┌─────────────────────────────────────────────────────────────┐
│  3. REMOVE PKCS#7 PADDING (CHỈ KHI DECRYPT BLOCK CUỐI)      │
└─────────────────────────────────────────────────────────────┘
    ↓
    lastBlock = (actualImageSize - 1) / 16
    
    if (endBlock == lastBlock):
        // Đang decrypt block cuối của image
        paddingLen = tempBuffer[tempOffset - 1]
        if (paddingLen > 0 && paddingLen <= 16):
            tempOffset -= paddingLen  // Loại bỏ padding
            
┌─────────────────────────────────────────────────────────────┐
│  4. EXTRACT DỮ LIỆU YÊU CẦU                                 │
└─────────────────────────────────────────────────────────────┘
    ↓
    offsetInDecrypted = requestedOffset % 16
    
    arrayCopy(
        tempBuffer,
        offsetInDecrypted,
        outputBuffer,
        0,
        requestedLength
    )
    
    return requestedLength
```

### Ví dụ Read Image cụ thể

```
Image gốc: 50 bytes
Encrypted: 64 bytes (4 blocks)
Request: Đọc 10 bytes từ vị trí 40

┌─────────────────────────────────────────────────────────────┐
│  CALCULATION                                                │
└─────────────────────────────────────────────────────────────┘
    requestedOffset = 40
    requestedLength = 10
    
    startBlock = 40 / 16 = 2
    endBlock = (40 + 10 - 1) / 16 = 49 / 16 = 3
    lastBlock = (50 - 1) / 16 = 3
    
    → Decrypt block 2 và 3 (32 bytes encrypted)
    
┌─────────────────────────────────────────────────────────────┐
│  DECRYPTION                                                 │
└─────────────────────────────────────────────────────────────┘
    Block 2: imageBuffer[32..47] → tempBuffer[0..15]
    Block 3: imageBuffer[48..63] → tempBuffer[16..31]
    
    tempBuffer sau decrypt:
    [B12 B13 ... B27 | B28 C0 C1 C2 C3 C4 0A 0A 0A 0A 0A 0A 0A 0A 0A 0A]
     ← Block 2 (16)      ← Block 3 (16) →
     
┌─────────────────────────────────────────────────────────────┐
│  REMOVE PADDING                                             │
└─────────────────────────────────────────────────────────────┘
    endBlock (3) == lastBlock (3) ✅
    paddingLen = tempBuffer[31] = 0x0A = 10
    tempOffset = 32 - 10 = 22
    
    tempBuffer (sau remove padding):
    [B12 B13 ... B27 | B28 C0 C1 C2 C3 C4]
                       ← 22 bytes valid data →
     
┌─────────────────────────────────────────────────────────────┐
│  EXTRACT                                                    │
└─────────────────────────────────────────────────────────────┘
    offsetInDecrypted = 40 % 16 = 8
    
    // Vị trí 40 trong image gốc = vị trí 8 trong block 2
    // Block 2 bắt đầu từ tempBuffer[0]
    // → Đọc từ tempBuffer[8]
    
    arrayCopy(tempBuffer, 8, output, 0, 10)
    
    Output: [B20 B21 ... B27 B28 C0]
            └─ 8 bytes từ block 2 ┘└ 2 bytes từ block 3
```

---

## LUỒNG 7: Admin Authentication và Dual Key System

### Kiến trúc Dual Master Key Wrapping

```
                    ┌─────────────────┐
                    │   Master Key    │
                    │    (16 bytes)   │
                    └─────────────────┘
                            │
                ┌───────────┴───────────┐
                ▼                       ▼
        ┌───────────────┐       ┌───────────────┐
        │   User KEK    │       │  Admin KEK    │
        │ (từ user PIN) │       │ (từ admin PIN)│
        └───────────────┘       └───────────────┘
                │                       │
                ▼                       ▼
    ┌─────────────────────┐   ┌───────────────────────┐
    │ wrappedMasterKey    │   │ adminWrappedMasterKey │
    │ [IV | Enc(MK)]      │   │ [IV | Enc(MK)]        │
    └─────────────────────┘   └───────────────────────┘
```

### Quy trình Admin Verify PIN

```
┌─────────────────────────────────────────────────────────────┐
│  1. DERIVE ADMIN KEK                                        │
└─────────────────────────────────────────────────────────────┘
    ↓
    PBKDF2-HMAC-SHA256(
        password: adminPIN,
        salt: salt (SAME salt với user),
        iterations: 500,
        outputLen: 16
    ) → adminKEK
    
┌─────────────────────────────────────────────────────────────┐
│  2. UNWRAP MASTER KEY TỪ ADMIN WRAPPED                      │
└─────────────────────────────────────────────────────────────┘
    ↓
    iv = adminWrappedMasterKey[0..15]
    encryptedKey = adminWrappedMasterKey[16..31]
    
    candidateMasterKey = AES-CBC-Decrypt(
        encryptedKey, adminKEK, iv
    )
    
┌─────────────────────────────────────────────────────────────┐
│  3. VERIFY MASTER KEY HASH                                  │
└─────────────────────────────────────────────────────────────┘
    ↓
    if (SHA1(candidateMasterKey) == masterKeyHash):
        ✅ Admin PIN đúng
        masterKey = candidateMasterKey
        adminSessionAuth = true
        adminPinTryCounter = 3
    else:
        ❌ Admin PIN sai
        adminPinTryCounter--
        if (adminPinTryCounter == 0):
            adminLockedFlag = true
```

### Tại sao cần 2 wrapped copies?

✅ **Separation of Privilege:**
- User PIN unlock → Chỉ truy cập data
- Admin PIN unlock → Có quyền reset user PIN

✅ **Emergency Recovery:**
- User quên PIN → Admin unwrap bằng admin KEK → Re-wrap với user KEK mới

✅ **Independent Counter:**
- User bị lock (3 lần sai user PIN) ≠ Admin bị lock
- Admin vẫn có thể unlock user

---

## Bảng tóm tắt các loại Key

| Key Type | Size | Storage | Purpose | Lifetime |
|----------|------|---------|---------|----------|
| **Master Key** | 16 bytes | Transient (RAM) | Mã hóa user data & image | Session |
| **User KEK** | 16 bytes | Derived (not stored) | Wrap/unwrap master key | Per-operation |
| **Admin KEK** | 16 bytes | Derived (not stored) | Wrap/unwrap master key | Per-operation |
| **Salt** | 16 bytes | Persistent (EEPROM) | PBKDF2 input | Permanent |
| **Master Key Hash** | 20 bytes | Persistent | Verify unwrap thành công | Permanent |
| **Wrapped Master Key** | 32 bytes | Persistent | IV + Enc(MK) bằng user KEK | Permanent |
| **Admin Wrapped MK** | 32 bytes | Persistent | IV + Enc(MK) bằng admin KEK | Permanent |
| **RSA Private Key** | 128 bytes | Persistent | Sign challenge | Permanent |
| **RSA Public Key** | 128 bytes | Returned to backend | Verify signature | External |
| **Image IV** | 16 bytes | Persistent | Decrypt image | Per-image |
| **User Data IV** | 16 bytes | Embedded in encryptedUserData | Decrypt user data | Per-update |

---

## Sơ đồ tổng quan Data Flow

```
┌─────────────────────────────────────────────────────────────────────┐
│                         INSTALL CARD                                │
└─────────────────────────────────────────────────────────────────────┘
                                ↓
                    Generate: Salt, MasterKey, RSA Keys
                    Derive: UserKEK, AdminKEK
                    Wrap: MasterKey → 2 copies
                    Hash: SHA1(MasterKey)
                                ↓
                    ┌───────────────────────┐
                    │   PERSISTENT STORAGE  │
                    │ ─────────────────────│
                    │ • wrappedMasterKey    │
                    │ • adminWrappedMK      │
                    │ • masterKeyHash       │
                    │ • salt                │
                    │ • encryptedUserData   │
                    │ • imageBuffer (enc)   │
                    │ • rsaPrivateKey       │
                    └───────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      USER VERIFY PIN                                │
└─────────────────────────────────────────────────────────────────────┘
                    PIN + Salt
                        ↓
                    PBKDF2(500 iter)
                        ↓
                    UserKEK (transient)
                        ↓
                Unwrap(wrappedMasterKey, UserKEK)
                        ↓
                Verify SHA1(MasterKey)
                        ↓
                    ┌───────────────┐
                    │ SESSION OPEN  │
                    │ masterKey (RAM)│
                    └───────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   READ/WRITE USER DATA                              │
└─────────────────────────────────────────────────────────────────────┘
        WRITE:                          READ:
        TLV Data                        encryptedUserData
            ↓                                  ↓
        Generate IV                     Extract IV
            ↓                                  ↓
        AES-CBC-Enc(Data, MasterKey)   AES-CBC-Dec(Data, MasterKey)
            ↓                                  ↓
        [IV|Ciphertext]                   TLV Data
            ↓
        → encryptedUserData

┌─────────────────────────────────────────────────────────────────────┐
│                      WRITE IMAGE (STREAMING)                        │
└─────────────────────────────────────────────────────────────────────┘
        START: Generate imageIV, init cipher ONCE
            ↓
        CHUNK 1 → encryptImageChunk() → imageBuffer[0..N]
            ↓
        CHUNK 2 → encryptImageChunk() → imageBuffer[N..M]
            ↓
        CHUNK N → encryptImageChunk() + PKCS#7 → imageBuffer[M..end]
        
        Final: [encrypted_blocks] stored in imageBuffer
               imageIV stored separately

┌─────────────────────────────────────────────────────────────────────┐
│                      CHANGE PIN                                     │
└─────────────────────────────────────────────────────────────────────┘
        OldPIN + Salt → PBKDF2 → OldKEK
            ↓
        Unwrap(wrappedMasterKey, OldKEK) → Verify
            ↓
        NewPIN + Salt → PBKDF2 → NewKEK
            ↓
        Wrap(MasterKey, NewKEK) → wrappedMasterKey (overwrite)
        
        encryptedUserData KHÔNG ĐỔI ✅

┌─────────────────────────────────────────────────────────────────────┐
│                  ADMIN UNLOCK USER PIN                              │
└─────────────────────────────────────────────────────────────────────┘
        AdminPIN + Salt → PBKDF2 → AdminKEK
            ↓
        Unwrap(adminWrappedMasterKey, AdminKEK) → MasterKey
            ↓
        NewUserPIN + Salt → PBKDF2 → NewUserKEK
            ↓
        Wrap(MasterKey, NewUserKEK) → wrappedMasterKey (overwrite)
            ↓
        Reset pinTryCounter = 3
```