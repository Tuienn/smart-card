# API Documentation - SmartCard Host Backend

## Tổng quan
Backend này cung cấp các REST API để tương tác với JavaCard Applet. Frontend gửi dữ liệu plain text (PIN, data) xuống API, backend sẽ xử lý mã hóa Argon2 và giao tiếp với thẻ.

## Base URL
```
http://localhost:8080/api/v1/card
```

---

## 1. Kết nối thẻ

### `GET /connect`
Kết nối tới JavaCard thông qua đầu đọc thẻ.

**Request:**
```bash
GET /api/v1/card/connect
```

**Response:**
```json
{
  "success": true,
  "message": "Kết nối thành công tới Applet!"
}
```

---

## 2. Ngắt kết nối thẻ

### `GET /disconnect`
Ngắt kết nối với thẻ.

**Request:**
```bash
GET /api/v1/card/disconnect
```

**Response:**
```json
{
  "success": true,
  "message": "Đã ngắt kết nối"
}
```

---

## 3. Khởi tạo thẻ lần đầu

### `POST /initialize`
Khởi tạo thẻ với user PIN và admin PIN. Chỉ gọi một lần khi setup thẻ.

**Request:**
```bash
POST /api/v1/card/initialize
Content-Type: application/json

{
  "userPin": "123456",
  "adminPin": "admin123"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Khởi tạo thẻ thành công!"
}
```

**Lưu ý:**
- Backend sẽ tự động tạo SALT ngẫu nhiên
- Backend tính KEK từ PIN bằng Argon2
- Master Key được tạo ngẫu nhiên trên thẻ

---

## 4. Lấy SALT từ thẻ

### `GET /salt`
Lấy SALT của user và admin từ thẻ (thường không cần gọi trực tiếp, backend tự động xử lý).

**Request:**
```bash
GET /api/v1/card/salt
```

**Response:**
```json
{
  "success": true,
  "message": "Lấy SALT thành công",
  "data": {
    "userSalt": "A1B2C3D4E5F6...",
    "adminSalt": "F6E5D4C3B2A1..."
  }
}
```

---

## 5. Xác thực User PIN

### `POST /verify-user-pin`
Xác thực user bằng PIN. Cần gọi trước khi thực hiện các thao tác đọc/ghi dữ liệu hoặc đổi PIN.

**Request:**
```bash
POST /api/v1/card/verify-user-pin
Content-Type: application/json

{
  "pin": "123456"
}
```

**Response Success:**
```json
{
  "success": true,
  "message": "Xác thực user thành công!"
}
```

**Response Failed:**
```json
{
  "success": false,
  "message": "PIN user không đúng!"
}
```

**Response Blocked:**
```json
{
  "success": false,
  "message": "Tài khoản user đã bị khóa sau 5 lần nhập sai!"
}
```

**Lưu ý:**
- Có tối đa 5 lần thử
- Session xác thực sẽ mất khi ngắt kết nối thẻ

---

## 6. Xác thực Admin PIN

### `POST /verify-admin-pin`
Xác thực admin. Cần gọi trước khi reset PIN user.

**Request:**
```bash
POST /api/v1/card/verify-admin-pin
Content-Type: application/json

{
  "pin": "admin123"
}
```

**Response:** Tương tự như verify user PIN

---

## 7. Đổi PIN User

### `POST /change-user-pin`
User đổi PIN của chính mình. **Cần verify user PIN trước**.

**Request:**
```bash
POST /api/v1/card/change-user-pin
Content-Type: application/json

{
  "newPin": "newpass123"
}
```

**Response Success:**
```json
{
  "success": true,
  "message": "Đổi PIN user thành công!"
}
```

**Response Not Authenticated:**
```json
{
  "success": false,
  "message": "Chưa xác thực user. Vui lòng verify PIN trước!"
}
```

**Quy trình:**
1. Gọi `POST /verify-user-pin` với PIN cũ
2. Gọi `POST /change-user-pin` với PIN mới

---

## 8. Admin Reset PIN User

### `POST /reset-user-pin`
Admin reset PIN cho user (khi user quên PIN). **Cần verify admin PIN trước**.

**Request:**
```bash
POST /api/v1/card/reset-user-pin
Content-Type: application/json

{
  "newPin": "resetpass123"
}
```

**Response Success:**
```json
{
  "success": true,
  "message": "Reset PIN user thành công! Counter đã được đặt lại."
}
```

**Quy trình:**
1. Gọi `POST /verify-admin-pin` với admin PIN
2. Gọi `POST /reset-user-pin` với PIN mới cho user

**Khác biệt với change-user-pin:**
- Cần quyền admin
- Reset counter về 5 (mở khóa nếu user bị khóa)

---

## 9. Đọc dữ liệu từ thẻ

### `GET /data`
Đọc dữ liệu đã được mã hóa trên thẻ. **Cần verify PIN trước** (user hoặc admin).

**Request:**
```bash
GET /api/v1/card/data
```

**Response Success:**
```json
{
  "success": true,
  "message": "Đọc dữ liệu thành công",
  "data": "Hello World!"
}
```

**Response Not Authenticated:**
```json
{
  "success": false,
  "message": "Chưa xác thực. Vui lòng verify PIN trước!"
}
```

**Response No Data:**
```json
{
  "success": false,
  "message": "Chưa có dữ liệu được lưu trên thẻ!"
}
```

**Quy trình:**
1. Gọi `POST /verify-user-pin` hoặc `POST /verify-admin-pin`
2. Gọi `GET /data`

---

## 10. Ghi dữ liệu vào thẻ

### `POST /data`
Ghi dữ liệu và mã hóa trên thẻ. **Cần verify PIN trước** (user hoặc admin).

**Request:**
```bash
POST /api/v1/card/data
Content-Type: application/json

{
  "data": "Secret message"
}
```

**Response Success:**
```json
{
  "success": true,
  "message": "Ghi dữ liệu thành công!"
}
```

**Response Not Authenticated:**
```json
{
  "success": false,
  "message": "Chưa xác thực. Vui lòng verify PIN trước!"
}
```

**Response Data Too Large:**
```json
{
  "success": false,
  "message": "Dữ liệu quá lớn! Tối đa 512 bytes."
}
```

**Quy trình:**
1. Gọi `POST /verify-user-pin` hoặc `POST /verify-admin-pin`
2. Gọi `POST /data`

**Giới hạn:**
- Dữ liệu tối đa: 512 bytes
- Backend tự động padding đến bội số 16 bytes

---

## Quy trình sử dụng đầy đủ

### A. Khởi tạo thẻ lần đầu
```javascript
// 1. Kết nối thẻ
GET /api/v1/card/connect

// 2. Khởi tạo với PIN
POST /api/v1/card/initialize
{
  "userPin": "123456",
  "adminPin": "admin123"
}
```

### B. User đăng nhập và ghi/đọc dữ liệu
```javascript
// 1. Kết nối (nếu chưa)
GET /api/v1/card/connect

// 2. Xác thực
POST /api/v1/card/verify-user-pin
{
  "pin": "123456"
}

// 3. Ghi dữ liệu
POST /api/v1/card/data
{
  "data": "My secret data"
}

// 4. Đọc dữ liệu
GET /api/v1/card/data
```

### C. User đổi PIN
```javascript
// 1. Xác thực với PIN cũ
POST /api/v1/card/verify-user-pin
{
  "pin": "123456"
}

// 2. Đổi PIN
POST /api/v1/card/change-user-pin
{
  "newPin": "newpass123"
}
```

### D. Admin reset PIN user (khi user quên)
```javascript
// 1. Xác thực admin
POST /api/v1/card/verify-admin-pin
{
  "pin": "admin123"
}

// 2. Reset PIN user
POST /api/v1/card/reset-user-pin
{
  "newPin": "resetpass123"
}
```

---

## Error Handling

### HTTP Status Codes
- `200 OK`: Thành công
- `401 UNAUTHORIZED`: Chưa xác thực hoặc xác thực thất bại
- `500 INTERNAL_SERVER_ERROR`: Lỗi server hoặc lỗi giao tiếp thẻ

### Common Error Messages
- `"Không tìm thấy đầu đọc thẻ!"` - Chưa cắm đầu đọc hoặc simulator chưa chạy
- `"Chưa kết nối thẻ"` - Chưa gọi `/connect`
- `"PIN không đúng!"` - PIN sai (còn lần thử)
- `"Tài khoản đã bị khóa sau 5 lần nhập sai!"` - Cần admin reset
- `"Chưa xác thực"` - Cần gọi verify PIN trước
- `"Dữ liệu quá lớn"` - Data > 512 bytes

---

## Bảo mật

### ✅ Điểm mạnh
1. **PIN không bao giờ được lưu trữ**: Chỉ tồn tại trong request
2. **Argon2 KDF**: Tính toán KEK mạnh mẽ từ PIN
3. **SALT ngẫu nhiên**: Mỗi PIN có SALT riêng biệt
4. **AES-256 encryption**: Dữ liệu được mã hóa trên thẻ
5. **PIN retry counter**: Khóa sau 5 lần sai
6. **Session-based auth**: Xác thực mất khi deselect thẻ

### ⚠️ Lưu ý
1. Sử dụng HTTPS trong production
2. Frontend không nên lưu PIN trong localStorage
3. Timeout session authentication sau khoảng thời gian không hoạt động
4. Log các lần xác thực thất bại để phát hiện tấn công

---

## Ví dụ Frontend (JavaScript)

```javascript
const API_BASE = 'http://localhost:8080/api/v1/card';

// Kết nối thẻ
async function connectCard() {
  const response = await fetch(`${API_BASE}/connect`);
  const data = await response.json();
  console.log(data.message);
}

// Khởi tạo thẻ
async function initializeCard(userPin, adminPin) {
  const response = await fetch(`${API_BASE}/initialize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userPin, adminPin })
  });
  return await response.json();
}

// Xác thực user
async function verifyUserPin(pin) {
  const response = await fetch(`${API_BASE}/verify-user-pin`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pin })
  });
  return await response.json();
}

// Ghi dữ liệu
async function saveData(data) {
  const response = await fetch(`${API_BASE}/data`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ data })
  });
  return await response.json();
}

// Đọc dữ liệu
async function loadData() {
  const response = await fetch(`${API_BASE}/data`);
  const result = await response.json();
  return result.data; // Plain text data
}

// Ví dụ sử dụng
async function main() {
  await connectCard();
  
  // Login
  const verifyResult = await verifyUserPin('123456');
  if (verifyResult.success) {
    // Ghi dữ liệu
    await saveData('Hello from Frontend!');
    
    // Đọc dữ liệu
    const data = await loadData();
    console.log('Data:', data);
  }
}
```

---

## Testing với Postman/cURL

### 1. Khởi tạo thẻ
```bash
curl -X POST http://localhost:8080/api/v1/card/initialize \
  -H "Content-Type: application/json" \
  -d '{"userPin":"123456","adminPin":"admin123"}'
```

### 2. Xác thực user
```bash
curl -X POST http://localhost:8080/api/v1/card/verify-user-pin \
  -H "Content-Type: application/json" \
  -d '{"pin":"123456"}'
```

### 3. Ghi dữ liệu
```bash
curl -X POST http://localhost:8080/api/v1/card/data \
  -H "Content-Type: application/json" \
  -d '{"data":"Secret message"}'
```

### 4. Đọc dữ liệu
```bash
curl http://localhost:8080/api/v1/card/data
```

---

## Cấu trúc Project

```
src/main/java/com/smartcard/
├── SmartCardApplication.java       # Main Spring Boot app
├── constant/
│   └── AppletConsts.java          # INS codes, SW codes, AID
├── controller/
│   └── CardController.java        # REST API endpoints
├── service/
│   └── CardService.java           # Business logic, Argon2, APDU
├── dto/
│   ├── ApiResponse.java           # Response wrapper
│   ├── InitializeRequest.java     # DTO cho /initialize
│   ├── VerifyPinRequest.java      # DTO cho verify PIN
│   ├── ChangePinRequest.java      # DTO cho change/reset PIN
│   ├── DataRequest.java           # DTO cho set data
│   └── SaltResponse.java          # DTO cho get salt
└── utils/
    └── HexUtils.java              # Hex conversion utilities
```

---

## Dependencies

```xml
<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>

<!-- Bouncy Castle for Argon2 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.78</version>
</dependency>
```

---

## Chạy ứng dụng

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Server sẽ chạy tại: http://localhost:8080
```

---

## Troubleshooting

### Lỗi: "Không tìm thấy đầu đọc thẻ"
- Kiểm tra JavaCard simulator đã chạy chưa
- Kiểm tra driver đầu đọc thẻ

### Lỗi: "Kết nối thất bại"
- Kiểm tra AID trong `AppletConsts.java` khớp với applet
- Kiểm tra applet đã được cài đặt trên thẻ/simulator chưa

### Lỗi: "Chưa xác thực"
- Phải gọi `/verify-user-pin` hoặc `/verify-admin-pin` trước
- Session authentication mất khi deselect applet

### Lỗi: "Tài khoản đã bị khóa"
- User đã nhập sai PIN 5 lần
- Cần admin gọi `/reset-user-pin` để mở khóa
