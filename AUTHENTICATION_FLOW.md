# Smart Card Authentication Flow

## Tổng quan
Hệ thống sử dụng xác thực RSA challenge-response để đảm bảo thẻ là chính hãng và được đăng ký trong hệ thống.

## Flow đăng ký thẻ mới (Card Registration)

### Bước 1: Thu thập thông tin người dùng
- Họ tên
- Tuổi
- Giới tính
- Ảnh đại diện (tùy chọn)
- Mã PIN (6 số)
- Số tiền nạp

### Bước 2: Khởi tạo thẻ
1. **Connect to card**: Kết nối với jCIDE simulator qua PC/SC
2. **Install card** (INS_INSTALL = 0x10):
   - Generate User ID (16 bytes random)
   - Gửi PIN và User ID đến thẻ
   - Thẻ generate RSA key pair (1024 bits)
   - Thẻ trả về RSA Public Key

### Bước 3: Đăng ký vào Backend
- Gọi API `POST /api/cards` với thông tin:
  ```json
  {
    "_id": "user_id_hex_string",
    "user_name": "Tên người dùng",
    "user_age": 25,
    "user_gender": true,
    "public_key": "base64_encoded_public_key"
  }
  ```

### Bước 4: Ghi thông tin vào thẻ
1. **Verify PIN** (INS_VERIFY_PIN = 0x20)
2. **Write user data** (INS_WRITE_USER_DATA_BASIC = 0x51):
   - Name, Gender, Age (TLV format)
3. **Top up coins** (INS_TOPUP_COINS = 0x32)
4. **Write avatar** (INS_WRITE_IMAGE_START = 0x52, INS_WRITE_IMAGE_CONTINUE = 0x53)

---

## Flow xác thực thẻ (Card Authentication)

### Khi nào xác thực diễn ra?
- **Tự động** khi connect vào thẻ đã được khởi tạo
- Sau khi applet được select thành công

### Chi tiết các bước:

#### Bước 1: Đọc User ID từ thẻ
```java
CommandAPDU: INS_READ_USER_ID (0x55)
Response: 16 bytes User ID
```

#### Bước 2: Lấy Public Key từ Backend
```java
GET http://localhost:4000/api/cards/{user_id_hex}
Response: {
  "success": true,
  "data": {
    "_id": "...",
    "user_name": "...",
    "public_key": "base64_encoded_key",
    ...
  }
}
```

#### Bước 3: Challenge-Response Authentication
1. **Tạo random challenge** (32 bytes)
2. **Gửi challenge đến thẻ** (INS_SIGN_CHALLENGE = 0x41):
   ```
   CommandAPDU: [CLA=0x00, INS=0x41, P1=0x00, P2=0x00, Data=challenge]
   Response: RSA signature (128 bytes for 1024-bit key)
   ```
3. **Verify signature** bằng public key:
   ```java
   Signature verifier = Signature.getInstance("SHA1withRSA");
   verifier.initVerify(publicKey);
   verifier.update(challenge);
   boolean verified = verifier.verify(signature);
   ```

#### Bước 4: Xử lý kết quả
- **Thành công**: Giữ kết nối, cho phép truy cập các chức năng
- **Thất bại**: Ngắt kết nối, báo lỗi "Thẻ không hợp lệ"

---

## Xử lý các trường hợp đặc biệt

### 1. Thẻ chưa được khởi tạo (New Card)
- `readUserId()` trả về lỗi `SW_CONDITIONS_NOT_SATISFIED (0x6985)`
- **Xử lý**: Skip authentication, cho phép card registration

### 2. Thẻ không tồn tại trong Backend
- Backend API trả về HTTP 404
- **Xử lý**: Ngắt kết nối, báo lỗi "Không tìm thấy thẻ trong hệ thống"

### 3. Signature không khớp
- `verifier.verify()` trả về `false`
- **Xử lý**: Ngắt kết nối, báo lỗi "Xác thực thẻ thất bại"

### 4. Backend không khả dụng
- Connection timeout hoặc network error
- **Xử lý**: Báo lỗi "Lỗi kết nối đến hệ thống backend"

---

## Security Considerations

### 1. RSA Key Size
- Sử dụng 1024 bits (balance giữa security và compatibility với JavaCard)
- Signature algorithm: SHA1withRSA

### 2. Challenge Randomness
- Sử dụng `SecureRandom` để generate challenge
- Challenge size: 32 bytes (256 bits)
- Mỗi lần connect tạo challenge mới

### 3. Public Key Storage
- Public key được lưu ở backend (MongoDB)
- Encoded dạng Base64 trong database
- X.509 DER format khi parse

### 4. User ID as Identifier
- 16 bytes random, generated trên client
- Sử dụng làm unique identifier (_id) trong database
- Hex string format khi gọi API

---

## API Endpoints

### 1. Register Card
```
POST /api/cards
Content-Type: application/json

{
  "_id": "user_id_hex",
  "user_name": "string",
  "user_age": number,
  "user_gender": boolean,
  "public_key": "base64_string"
}

Response:
{
  "success": true,
  "data": { card_object }
}
```

### 2. Get Card by User ID
```
GET /api/cards/{user_id_hex}

Response:
{
  "success": true,
  "data": {
    "_id": "...",
    "user_name": "...",
    "user_age": ...,
    "user_gender": ...,
    "public_key": "..."
  }
}
```

---

## Code Flow Diagram

```
┌─────────────────┐
│  Desktop App    │
│   (JavaFX)      │
└────────┬────────┘
         │
         ├─(1)─ connect() ──────────────────┐
         │                                   │
         ├─(2)─ Select Applet               │
         │                                   │
         ├─(3)─ readUserId()                 │
         │        │                          │
         │        └─ INS_READ_USER_ID ───────┼───► ┌──────────────┐
         │              (0x55)               │     │  Smart Card  │
         │                                   │     │   (JavaCard) │
         ├─(4)─ getPublicKeyFromBackend() ───┼─┐   └──────────────┘
         │        │                          │ │
         │        └─ GET /api/cards/{id} ────┼─┼──► ┌──────────────┐
         │                                   │ │    │   Backend    │
         │                                   │ │    │  (Express +  │
         ├─(5)─ Generate Challenge (32B)    │ │    │   MongoDB)   │
         │                                   │ │    └──────────────┘
         ├─(6)─ signChallenge() ─────────────┼─┘
         │        │                          │
         │        └─ INS_SIGN_CHALLENGE ─────┤
         │              (0x41)               │
         │                                   │
         ├─(7)─ Verify Signature             │
         │                                   │
         └─(8)─ Result: Success/Failed       │
                                             │
                                             ▼
```

---

## Testing

### 1. Test Connection
```bash
# Trong CardService.java có sẵn test method:
java -cp ... com.example.desktopapp.service.CardService
```

### 2. Test Authentication Flow
1. Start jCIDE simulator với PC/SC enabled
2. Power on card
3. Load Entertainment applet
4. Start backend: `cd express-backend && npm start`
5. Run desktop app
6. Connect to card → Authentication tự động chạy

### 3. Debug Mode
```java
cardService.setDebugMode(true);
```
Sẽ in ra console:
- APDU commands/responses
- User ID
- Public key (truncated)
- Challenge và signature
- Verification result

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| "Không tìm thấy thẻ trong hệ thống" | Card chưa được đăng ký vào backend | Thực hiện Card Registration |
| "Xác thực thẻ thất bại" | Public key không khớp hoặc thẻ fake | Kiểm tra database, reset và đăng ký lại |
| "Lỗi kết nối đến backend" | Backend không chạy | Start backend: `npm start` |
| Authentication skipped | Card chưa initialized | Normal behavior for new cards |
