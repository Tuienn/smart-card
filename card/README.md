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

### 3. INS_UNLOCK_BY_ADMIN (0x21) - Mở khóa bởi Admin
**Mục đích:** Reset PIN counter và tùy chọn đổi PIN mới

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

---

### 4. INS_CHECK_ACCESS_FOR_GAME (0x30) - Kiểm tra quyền truy cập game
**Mục đích:** Kiểm tra user có quyền chơi game không (đã mua hoặc đủ coins)

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
- Success: `0x01` (có quyền) hoặc `0x00` (không có quyền) + `SW=0x9000`
- Error:
  - `0x6982`: Chưa xác thực PIN

**Ví dụ:**
```
Request: 00 30 00 00 03 05 00 64  // Game ID=5, cần 100 coins
Response: 01 90 00  // Có quyền truy cập
```

---

### 5. INS_TOPUP_COINS (0x32) - Nạp coins
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

### 6. INS_PURCHASE_COMBO (0x33) - Mua combo game
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

### 7. INS_SIGN_CHALLENGE (0x41) - Ký challenge
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

### 8. INS_READ_USER_DATA_BASIC (0x50) - Đọc dữ liệu user
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

### 9. INS_WRITE_USER_DATA_BASIC (0x51) - Ghi dữ liệu user
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

### 10. INS_WRITE_IMAGE_START (0x52) - Bắt đầu ghi ảnh
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

### 11. INS_WRITE_IMAGE_CONTINUE (0x53) - Tiếp tục ghi ảnh
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

### 12. INS_READ_IMAGE (0x54) - Đọc ảnh
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

### 13. INS_RESET_CARD (0x99) - Reset thẻ
**Mục đích:** Xóa toàn bộ dữ liệu và reset thẻ về trạng thái ban đầu

**Request:**
```
CLA: 0x00
INS: 0x99
P1: 0x00
P2: 0x00
```

**Response:**
- Success: `SW=0x9000`

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

### 3. Mua game
```
1. Xác thực PIN
2. Gọi INS_CHECK_ACCESS_FOR_GAME để kiểm tra quyền
3. Nếu chưa có quyền:
   - Gọi INS_TOPUP_COINS để nạp tiền (nếu cần)
   - Gọi INS_PURCHASE_COMBO để mua game
```

### 4. Quản lý dữ liệu user
```
1. Xác thực PIN
2. Gọi INS_READ_USER_DATA_BASIC để đọc dữ liệu
3. Gọi INS_WRITE_USER_DATA_BASIC để cập nhật
```

### 5. Upload ảnh đại diện
```
1. Xác thực PIN
2. Gọi INS_WRITE_IMAGE_START với chunk đầu tiên
3. Gọi INS_WRITE_IMAGE_CONTINUE cho các chunk tiếp theo
4. Gọi INS_READ_IMAGE để đọc lại ảnh
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
- Giới hạn 3 lần thử sai
- Thẻ bị khóa sau 3 lần sai
- Chỉ Admin có thể unlock

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
3. **Error Handling:** Check SW code để xử lý lỗi phù hợp
4. **Image Upload:** Chia ảnh thành chunks ~200 bytes để tránh overflow
5. **TLV Format:** Khi ghi user data, sử dụng đúng format Tag-Length-Value
6. **Public Key:** Lưu public key sau INSTALL để verify signature sau này
