# Entertainment JavaCard Applet - API Documentation

## Tổng quan
Applet Entertainment cung cấp các chức năng quản lý game, xác thực người dùng, và bảo mật dữ liệu trên JavaCard.

## Cấu trúc APDU
```
CLA | INS | P1 | P2 | Lc | Data | Le
```

## Danh sách INS Commands

### 1. INS_INSTALL (0x10) - Khởi tạo thẻ
**Mục đích:** Khởi tạo thẻ lần đầu với PIN và User ID, không yêu cầu xác thực lại khi khởi tạo thành công

**Request:**
```
CLA: 0x00
INS: 0x10
P1: 0x00
P2: 0x00
Lc: Length
Data: [PIN_LENGTH(1)] [PIN(4-16 bytes)] [USER_ID(16 bytes)]
```

**Response:**
- Success: `SW=0x9000` + RSA Public Key (Modulus + Exponent)
- Error: 
  - `0x6985`: Thẻ đã được khởi tạo
  - `0x6A80`: Dữ liệu không hợp lệ

**Ví dụ:**
```
Request: 00 10 00 00 15 04 31 32 33 34 [16 bytes User ID]
Response: [RSA Public Key] 90 00
```

---

### 2. INS_VERIFY_PIN (0x20) - Xác thực PIN
**Mục đích:** Xác thực PIN để bắt đầu session

**Request:**
```
CLA: 0x00
INS: 0x20
P1: 0x00
P2: 0x00
Lc: PIN_LENGTH (4-16)
Data: [PIN]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x63CX`: PIN sai, còn X lần thử
  - `0x6983`: Thẻ bị khóa
  - `0x6985`: Chưa khởi tạo thẻ

**Ví dụ:**
```
Request: 00 20 00 00 04 31 32 33 34
Response: 90 00
```

---

### 3. INS_CHANGE_PIN (0x23) - Đổi PIN của User
**Mục đích:** User tự đổi PIN của mình khi đã xác thực

**Yêu cầu tiên quyết:** Phải xác thực User PIN (INS 0x20) trước

**Request:**
```
CLA: 0x00
INS: 0x23
P1: 0x00
P2: 0x00
Lc: OLD_PIN_LENGTH + NEW_PIN_LENGTH + 2
Data: [OLD_PIN_LENGTH(1)] [OLD_PIN(4-16)] [NEW_PIN_LENGTH(1)] [NEW_PIN(4-16)]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực User PIN
  - `0x6983`: Thẻ bị khóa
  - `0x6985`: Old PIN không đúng
  - `0x6A80`: Dữ liệu không hợp lệ (PIN length sai)

**Ví dụ:**
```
// Bước 1: Xác thực User PIN hiện tại
Request: 00 20 00 00 04 31 32 33 34  // PIN="1234"
Response: 90 00

// Bước 2: Đổi PIN từ "1234" sang "567890"
Request: 00 23 00 00 0C 04 31 32 33 34 06 35 36 37 38 39 30
// Data: [0x04][1234][0x06][567890]
Response: 90 00
```

**Lưu ý bảo mật:**
- Yêu cầu verify lại old PIN để tăng cường bảo mật
- Chỉ re-wrap master key, không ảnh hưởng đến encrypted user data
- Session vẫn được giữ nguyên sau khi đổi PIN thành công
- Master key hash được verify để đảm bảo tính toàn vẹn

---

### 4. INS_VERIFY_ADMIN_PIN (0x22) - Xác thực Admin PIN
**Mục đích:** Xác thực Admin PIN để có quyền unlock user PIN

**Request:**
```
CLA: 0x00
INS: 0x22
P1: 0x00
P2: 0x00
Lc: ADMIN_PIN_LENGTH (4-16)
Data: [ADMIN_PIN]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x63CX`: Admin PIN sai, còn X lần thử
  - `0x6983`: Admin bị khóa
  - `0x6985`: Chưa khởi tạo thẻ

**Admin PIN mặc định:** `1234567890123456` (16 ký tự)

**Ví dụ:**
```
Request: 00 22 00 00 10 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36
Response: 90 00
```

---

### 5. INS_UNLOCK_BY_ADMIN (0x21) - Mở khóa bởi Admin (Emergency Recovery)
**Mục đích:** Reset user PIN counter và tùy chọn đổi user PIN mới (dành cho trường hợp user quên PIN)

**Yêu cầu tiên quyết:** Phải xác thực Admin PIN (INS 0x22) trước

**So sánh với INS_CHANGE_PIN:**
| Đặc điểm | CHANGE_PIN (0x23) | UNLOCK_BY_ADMIN (0x21) |
|----------|-------------------|------------------------|
| Yêu cầu auth | User PIN | Admin PIN |
| Verify old PIN | ✅ Bắt buộc | ❌ Không cần |
| Use case | Đổi PIN thường xuyên | Emergency recovery |
| Reset counter | ❌ Không | ✅ Có |

**Request:**
```
CLA: 0x00
INS: 0x21
P1: 0x00
P2: 0x00
Lc: NEW_PIN_LENGTH (0 hoặc 4-16)
Data: [NEW_PIN] (optional)
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực Admin PIN

**Ví dụ:**
```
// Bước 1: Xác thực Admin PIN
Request: 00 22 00 00 10 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36
Response: 90 00

// Bước 2: Unlock và đổi user PIN mới
Request: 00 21 00 00 04 35 36 37 38  // Đổi PIN thành "5678"
Response: 90 00
```

---

### 5. INS_TRY_PLAY_GAME (0x30) - Thử chơi game
**Mục đích:** Kiểm tra quyền chơi game và tự động trừ coins nếu chưa mua

**Logic:**
- Nếu game đã được mua (trong danh sách BOUGHT_GAMES): Cho chơi miễn phí
- Nếu game chưa mua: Trừ coins theo giá (pay-per-play)

**Request:**
```
CLA: 0x00
INS: 0x30
P1: 0x00
P2: 0x00
Lc: 0x03
Data: [GAME_ID(1)] [REQUIRED_COINS(2 bytes - Big Endian)]
```

**Response:**
- Success: `0x01` + `SW=0x9000` (Được phép chơi, coins đã bị trừ nếu cần)
- Error:
  - `0x6982`: Chưa xác thực PIN
  - `0x6985`: Không đủ coins để chơi

**Ví dụ:**
```
// Trường hợp 1: Game chưa mua, có đủ coins
Request: 00 30 00 00 03 05 00 64  // Game ID=5, cần 100 coins
Response: 01 90 00  // Thành công, đã trừ 100 coins

// Trường hợp 2: Game đã mua
Request: 00 30 00 00 03 05 00 64  // Game ID=5
Response: 01 90 00  // Thành công, không trừ tiền

// Trường hợp 3: Không đủ coins
Request: 00 30 00 00 03 05 00 64  // Chỉ còn 50 coins
Response: 69 85  // Lỗi: Không đủ coins
```

---

### 6. INS_TOPUP_COINS (0x32) - Nạp coins
**Mục đích:** Thêm coins vào tài khoản user

**Request:**
```
CLA: 0x00
INS: 0x32
P1: 0x00
P2: 0x00
Lc: 0x04
Data: [AMOUNT(4 bytes - Big Endian)]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN

**Ví dụ:**
```
Request: 00 32 00 00 04 00 00 03 E8  // Nạp 1000 coins
Response: 90 00
```

---

### 7. INS_PURCHASE_COMBO (0x33) - Mua combo game
**Mục đích:** Mua nhiều game cùng lúc

**Request:**
```
CLA: 0x00
INS: 0x33
P1: 0x00
P2: 0x00
Lc: NUM_GAMES + NUM_GAMES + 4
Data: [NUM_GAMES(1)] [GAME_ID_1] [GAME_ID_2] ... [TOTAL_PRICE(4 bytes)]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN
  - `0x6985`: Không đủ coins
  - `0x6A80`: Dữ liệu không hợp lệ

**Ví dụ:**
```
Request: 00 33 00 00 08 03 01 02 03 00 00 01 2C  // Mua 3 game (ID: 1,2,3), giá 300
Response: 90 00
```

---

### 8. INS_SIGN_CHALLENGE (0x41) - Ký challenge
**Mục đích:** Ký một challenge bằng RSA private key để xác thực

**Request:**
```
CLA: 0x00
INS: 0x41
P1: 0x00
P2: 0x00
Lc: CHALLENGE_LENGTH
Data: [CHALLENGE]
```

**Response:**
- Success: [RSA_SIGNATURE] + `SW=0x9000`

**Ví dụ:**
```
Request: 00 41 00 00 20 [32 bytes challenge]
Response: [256 bytes signature] 90 00
```

---

### 9. INS_READ_USER_DATA_BASIC (0x50) - Đọc dữ liệu user
**Mục đích:** Đọc một trường dữ liệu cụ thể của user

**Request:**
```
CLA: 0x00
INS: 0x50
P1: 0x00
P2: 0x00
Lc: 0x01
Data: [TAG]
```

**Tags:**
- `0x01`: NAME (Tên)
- `0x02`: GENDER (Giới tính)
- `0x03`: COINS (Số coins)
- `0x04`: BOUGHT_GAMES (Danh sách game đã mua)

**Response:**
- Success: [FIELD_DATA] + `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN
  - `0x6A80`: Tag không tồn tại

**Ví dụ:**
```
Request: 00 50 00 00 01 03  // Đọc số coins
Response: 00 00 03 E8 90 00  // 1000 coins
```

---

### 10. INS_WRITE_USER_DATA_BASIC (0x51) - Ghi dữ liệu user
**Mục đích:** Cập nhật dữ liệu user theo định dạng TLV

**Request:**
```
CLA: 0x00
INS: 0x51
P1: 0x00
P2: 0x00
Lc: Length
Data: [TAG_1][LENGTH_1][VALUE_1] [TAG_2][LENGTH_2][VALUE_2] ...
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN

**Ví dụ:**
```
Request: 00 51 00 00 08 01 04 4A 6F 68 6E 02 01 01
// Ghi NAME="John" (01 04 4A6F686E), GENDER=Male (02 01 01)
Response: 90 00
```

---

### 11. INS_WRITE_IMAGE_START (0x52) - Bắt đầu ghi ảnh
**Mục đích:** Bắt đầu ghi ảnh đại diện (chunk đầu tiên)

**Request:**
```
CLA: 0x00
INS: 0x52
P1: 0x00
P2: 0x00
Lc: 3 + CHUNK_LENGTH
Data: [TOTAL_SIZE(2)] [IMAGE_TYPE(1)] [FIRST_CHUNK_DATA]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN
  - `0x6A84`: Không đủ bộ nhớ (>32KB)

**Ví dụ:**
```
Request: 00 52 00 00 xx 10 00 01 [image data...]
// Total size=4096 bytes, Type=1 (JPG), + first chunk
Response: 90 00
```

---

### 12. INS_WRITE_IMAGE_CONTINUE (0x53) - Tiếp tục ghi ảnh
**Mục đích:** Ghi các chunk tiếp theo của ảnh

**Request:**
```
CLA: 0x00
INS: 0x53
P1: 0x00
P2: 0x00
Lc: 2 + CHUNK_LENGTH
Data: [OFFSET(2)] [CHUNK_DATA]
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN
  - `0x6A84`: Vượt quá kích thước

**Ví dụ:**
```
Request: 00 53 00 00 xx 04 00 [image data...]
// Write from offset 1024
Response: 90 00
```

---

### 13. INS_READ_IMAGE (0x54) - Đọc ảnh
**Mục đích:** Đọc một phần ảnh

**Request:**
```
CLA: 0x00
INS: 0x54
P1: 0x00
P2: 0x00
Lc: 0x04
Data: [OFFSET(2)] [LENGTH(2)]
```

**Response:**
- Success: [IMAGE_DATA] + `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN

**Ví dụ:**
```
Request: 00 54 00 00 04 00 00 01 00
// Đọc 256 bytes từ offset 0
Response: [256 bytes image data] 90 00
```

---

### 14. INS_RESET_CARD (0x99) - Reset thẻ
**Mục đích:** Xóa toàn bộ dữ liệu và reset thẻ về trạng thái ban đầu

**Yêu cầu tiên quyết:** Phải xác thực Admin PIN (INS 0x22) trước

**Request:**
```
CLA: 0x00
INS: 0x99
P1: 0x00
P2: 0x00
```

**Response:**
- Success: `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực Admin PIN

**Ví dụ:**
```
// Bước 1: Xác thực Admin PIN
Request: 00 22 00 00 10 31 32 33 34 35 36 37 38 39 30 31 32 33 34 35 36
Response: 90 00

// Bước 2: Reset thẻ
Request: 00 99 00 00
Response: 90 00
```

---

## Status Words (SW)

| SW Code | Ý nghĩa |
|---------|---------|
| `0x9000` | Success |
| `0x6982` | Chưa xác thực PIN (PIN verification required) |
| `0x6983` | Thẻ bị khóa (Authentication blocked) |
| `0x6985` | Không đủ coins (Insufficient funds) |
| `0x6A80` | Dữ liệu sai (Wrong data) |
| `0x6A84` | Không đủ bộ nhớ (Not enough memory) |
| `0x63CX` | PIN sai, còn X lần thử |

---

## Luồng hoạt động cơ bản

### 1. Khởi tạo thẻ mới
```
1. Gọi INS_INSTALL (0x10) với PIN và User ID
2. Nhận RSA Public Key
3. Lưu Public Key để xác thực sau này
```

### 2. Xác thực và sử dụng
```
1. Gọi INS_VERIFY_PIN (0x20) để xác thực
2. Session được mở, có thể gọi các lệnh khác
3. Khi deselect applet, session tự động đóng
```

### 3. Đổi PIN
**Cách 1: User tự đổi PIN (khuyên dùng)**
```
1. Xác thực User PIN (INS 0x20)
2. Gọi INS_CHANGE_PIN (0x23) với old PIN + new PIN
3. Hệ thống verify lại old PIN để đảm bảo bảo mật
4. Session vẫn được giữ sau khi đổi PIN
```

**Cách 2: Admin reset PIN (emergency)**
```
1. Xác thực Admin PIN (INS 0x22)
2. Gọi INS_UNLOCK_BY_ADMIN (0x21) với new PIN
3. User PIN counter được reset về 3 lần thử
4. Không cần biết old user PIN
```

### 4. Chơi game
```
1. Xác thực PIN
2. Gọi INS_TRY_PLAY_GAME (0x30) để chơi game
   - Nếu đã mua game: Chơi miễn phí
   - Nếu chưa mua: Tự động trừ coins (pay-per-play)
   - Nếu không đủ coins: Trả về lỗi 0x6985
3. Hoặc mua game vĩnh viễn:
   - Gọi INS_TOPUP_COINS để nạp tiền (nếu cần)
   - Gọi INS_PURCHASE_COMBO để mua game (chỉ trả 1 lần)
   - Sau đó INS_TRY_PLAY_GAME sẽ miễn phí
```

### 5. Quản lý dữ liệu user
```
1. Xác thực PIN
2. Gọi INS_READ_USER_DATA_BASIC để đọc dữ liệu
3. Gọi INS_WRITE_USER_DATA_BASIC để cập nhật
```

### 6. Upload ảnh đại diện
```
1. Xác thực PIN
2. Gọi INS_WRITE_IMAGE_START với chunk đầu tiên
3. Gọi INS_WRITE_IMAGE_CONTINUE cho các chunk tiếp theo
4. Gọi INS_READ_IMAGE để đọc lại ảnh
```

### 7. Unlock user PIN bằng Admin (Emergency)
```
1. Gọi INS_VERIFY_ADMIN_PIN (0x22) với admin PIN (mặc định: "1234567890123456")
2. Sau khi xác thực admin thành công, gọi INS_UNLOCK_BY_ADMIN (0x21)
3. Tùy chọn gửi kèm user PIN mới để đổi PIN
4. User PIN counter được reset về 3 lần thử
```

---

## Bảo mật

### Encryption
- **Master Key:** AES-256, được wrap bởi KEK
- **KEK (Key Encryption Key):** Derive từ PIN + Salt bằng PBKDF2 (10,000 iterations)
- **User Data:** Được mã hóa bằng Master Key với AES-CBC
- **RSA:** 2048-bit keypair để sign challenge

### Session Management
- Session chỉ valid sau khi xác thực PIN thành công
- Session tự động clear khi deselect applet
- Master key chỉ tồn tại trong transient memory

### PIN Protection
- **User PIN:** Giới hạn 3 lần thử sai, thẻ bị khóa sau 3 lần sai
- **Admin PIN:** 
  - Mặc định: `1234567890123456` (được khởi tạo tự động khi install)
  - Giới hạn 3 lần thử sai, admin bị khóa sau 3 lần sai
  - Admin Master Key được wrap riêng với Admin KEK
  - Chỉ admin đã xác thực mới có thể unlock user PIN
- **Dual Authentication:** User session và Admin session độc lập

---

## Giới hạn

- **PIN Length:** 4-16 bytes
- **User ID:** 16 bytes (fixed)
- **Max Games:** 50 games
- **Max Image Size:** 32KB
- **Max Name Length:** 64 bytes
- **Encrypted Data Size:** 256 bytes
- **Salt Size:** 16 bytes

---

## Notes cho Backend Developer

1. **Byte Order:** Sử dụng Big Endian cho tất cả số nguyên nhiều byte
2. **Session:** Luôn gọi VERIFY_PIN trước khi gọi các lệnh cần xác thực
3. **Đổi PIN:**
   - **User tự đổi:** Dùng INS_CHANGE_PIN (0x23) - yêu cầu verify old PIN, bảo mật cao hơn
   - **Admin reset:** Dùng INS_UNLOCK_BY_ADMIN (0x21) - cho trường hợp emergency, không cần old PIN
4. **Admin Operations:** Để unlock user PIN, phải gọi VERIFY_ADMIN_PIN (0x22) trước, sau đó mới gọi UNLOCK_BY_ADMIN (0x21)
5. **Admin PIN Default:** Admin PIN mặc định là `1234567890123456`, nên đổi ngay sau lần đầu sử dụng
6. **Error Handling:** Check SW code để xử lý lỗi phù hợp
7. **Image Upload:** Chia ảnh thành chunks ~200 bytes để tránh overflow
8. **TLV Format:** Khi ghi user data, sử dụng đúng format Tag-Length-Value
9. **Public Key:** Lưu public key sau INSTALL để verify signature sau này
10. **Session Independence:** Admin session và user session độc lập - cần xác thực riêng
11. **Master Key Persistence:** Khi đổi PIN (cả 2 cách), encrypted user data không bị ảnh hưởng vì chỉ re-wrap master key
