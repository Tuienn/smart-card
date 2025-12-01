# JavaCard Applet - Hệ thống Xác thực PIN & Quản lý Master Key

## Tổng quan

Applet này triển khai hệ thống bảo mật dựa trên PIN và mã hóa dữ liệu sử dụng Master Key (M). Master Key được bảo vệ bởi KEK (Key Encryption Key) được sinh ra từ Argon2 trên host.

### Kiến trúc bảo mật

```
Host (Argon2)                    JavaCard
─────────────                    ─────────
PIN_user ──────> Argon2 ──────> KEK_user ──────> AES Decrypt ──────> Master Key (M)
                 + SALT_user                      Enc_M_user

PIN_admin ─────> Argon2 ──────> KEK_admin ─────> AES Decrypt ──────> Master Key (M)
                 + SALT_admin                     Enc_M_admin

Master Key (M) ────────────────────────────────> AES Encrypt/Decrypt ──> Data
```

## Cấu trúc dữ liệu

### Lưu trữ EEPROM (Persistent)
| Tên | Kích thước | Mô tả |
|-----|-----------|-------|
| `SALT_user` | 16 bytes | Salt cho Argon2 user PIN |
| `SALT_admin` | 16 bytes | Salt cho Argon2 admin PIN |
| `Enc_M_user` | 32 bytes | Master Key được mã hóa bằng KEK_user |
| `Enc_M_admin` | 32 bytes | Master Key được mã hóa bằng KEK_admin |
| `SHA256_M` | 32 bytes | Hash của Master Key để verify |
| `Enc_DATA` | ≤512 bytes | Dữ liệu người dùng được mã hóa |
| `encDataLength` | 2 bytes | Độ dài thực tế của Enc_DATA |
| `userPinTries` | 1 byte | Số lần thử PIN user còn lại (max: 5) |
| `adminPinTries` | 1 byte | Số lần thử PIN admin còn lại (max: 5) |

### Lưu trữ RAM (Volatile - xóa khi deselect)
| Tên | Kích thước | Mô tả |
|-----|-----------|-------|
| `masterKeyRAM` | 32 bytes | Master Key M (AES-256) |
| `userAuthenticated` | boolean | Trạng thái xác thực user |
| `adminAuthenticated` | boolean | Trạng thái xác thực admin |
| `tempBuffer` | 64 bytes | Buffer tạm cho xử lý |
| `ivBuffer` | 16 bytes | IV buffer cho AES |

## Các lệnh APDU (INS codes)

### 1. INITIALIZE (0x08)
**Mục đích:** Khởi tạo thẻ lần đầu, tạo Master Key ngẫu nhiên và mã hóa với KEK.

**Input:**
```
KEK_user (32 bytes) + Salt_user (16 bytes) + KEK_admin (32 bytes) + Salt_admin (16 bytes)
Tổng: 96 bytes
```

**Xử lý:**
1. Tạo Master Key M ngẫu nhiên (32 bytes AES-256)
2. Tính SHA256(M) và lưu vào EEPROM
3. Mã hóa M với KEK_user → Enc_M_user
4. Mã hóa M với KEK_admin → Enc_M_admin
5. Lưu SALT_user, SALT_admin vào EEPROM
6. Giữ M trong RAM cho session hiện tại

**Output:** 9000 (Success)

**Lưu ý:** 
- Chỉ gọi một lần khi setup thẻ
- KEK_user và KEK_admin phải được tính từ Argon2 trên host

---

### 2. GET_SALT (0x01)
**Mục đích:** Lấy SALT để host tính toán Argon2.

**Input:** Không

**Output:**
```
Salt_user (16 bytes) + Salt_admin (16 bytes)
Tổng: 32 bytes
```

**Lưu ý:** 
- Lệnh public, không cần xác thực
- Gọi trước khi verify PIN

---

### 3. VERIFY_USER_PIN (0x02)
**Mục đích:** Xác thực user bằng KEK_user từ Argon2(PIN_user).

**Input:**
```
KEK_user (32 bytes)
```

**Xử lý:**
1. Kiểm tra userPinTries > 0 (nếu = 0 → SW_AUTHENTICATION_METHOD_BLOCKED)
2. Dùng KEK_user để AES decrypt Enc_M_user → M
3. Tính SHA256(M) và so sánh với SHA256_M đã lưu
4. Nếu khớp:
   - Set userAuthenticated = true
   - Reset userPinTries = 5
   - Giữ M trong RAM
5. Nếu sai:
   - Giảm userPinTries--
   - Wipe M khỏi RAM
   - Throw SW_SECURITY_STATUS_NOT_SATISFIED

**Output:** 9000 (Success) hoặc error code

**Error codes:**
- `6983`: SW_AUTHENTICATION_METHOD_BLOCKED (đã khóa sau 5 lần sai)
- `6982`: SW_SECURITY_STATUS_NOT_SATISFIED (PIN sai)

---

### 4. VERIFY_ADMIN_PIN (0x03)
**Mục đích:** Xác thực admin bằng KEK_admin từ Argon2(PIN_admin).

**Input:**
```
KEK_admin (32 bytes)
```

**Xử lý:** Tương tự VERIFY_USER_PIN nhưng dùng:
- adminPinTries
- Enc_M_admin
- adminAuthenticated flag

**Output:** 9000 (Success) hoặc error code

---

### 5. CHANGE_USER_PIN (0x04)
**Mục đích:** Đổi PIN user (cần xác thực user trước).

**Điều kiện:** userAuthenticated = true

**Input:**
```
KEK_new_user (32 bytes) + Salt_new_user (16 bytes)
Tổng: 48 bytes
```

**Xử lý:**
1. Kiểm tra userAuthenticated
2. Dùng M trong RAM để mã hóa lại với KEK_new_user
3. Cập nhật Enc_M_user và SALT_user vào EEPROM

**Output:** 9000 (Success)

**Error codes:**
- `6982`: Chưa xác thực user

---

### 6. RESET_USER_PIN (0x05)
**Mục đích:** Admin reset PIN user (cần xác thực admin).

**Điều kiện:** adminAuthenticated = true

**Input:**
```
KEK_new_user (32 bytes) + Salt_new_user (16 bytes)
Tổng: 48 bytes
```

**Xử lý:**
1. Kiểm tra adminAuthenticated
2. Dùng M trong RAM để mã hóa lại với KEK_new_user
3. Cập nhật Enc_M_user và SALT_user vào EEPROM
4. Reset userPinTries = 5

**Output:** 9000 (Success)

**Error codes:**
- `6982`: Chưa xác thực admin

**Lưu ý:** Khác với CHANGE_USER_PIN:
- Cần quyền admin
- Reset counter PIN user về 5

---

### 7. GET_DATA (0x06)
**Mục đích:** Đọc dữ liệu đã mã hóa.

**Điều kiện:** userAuthenticated = true HOẶC adminAuthenticated = true

**Input:** Không

**Xử lý:**
1. Kiểm tra authentication
2. Dùng M trong RAM để AES decrypt Enc_DATA
3. Trả về plaintext data

**Output:** Plaintext data (≤512 bytes)

**Error codes:**
- `6982`: Chưa xác thực
- `6A82`: Không có data (encDataLength = 0)

---

### 8. SET_DATA (0x07)
**Mục đích:** Ghi dữ liệu và mã hóa.

**Điều kiện:** userAuthenticated = true HOẶC adminAuthenticated = true

**Input:** Plaintext data (≤512 bytes)

**Xử lý:**
1. Kiểm tra authentication
2. Pad data đến bội số của 16 bytes (AES block size)
3. Dùng M trong RAM để AES encrypt data
4. Lưu Enc_DATA vào EEPROM
5. Cập nhật encDataLength

**Output:** 9000 (Success)

**Error codes:**
- `6982`: Chưa xác thực
- `6700`: Data quá lớn (>512 bytes)

---

## Quy trình sử dụng

### Khởi tạo thẻ lần đầu

```python
# 1. Host tạo PIN và salt
PIN_user = "123456"
PIN_admin = "admin123"
SALT_user = random_bytes(16)
SALT_admin = random_bytes(16)

# 2. Host tính KEK từ Argon2
KEK_user = Argon2(PIN_user, SALT_user)  # 32 bytes
KEK_admin = Argon2(PIN_admin, SALT_admin)  # 32 bytes

# 3. Gửi lệnh INITIALIZE đến thẻ
APDU: 00 60 00 00 60 [KEK_user][SALT_user][KEK_admin][SALT_admin]
```

### Xác thực User

```python
# 1. Lấy SALT từ thẻ
APDU: 00 10 00 00 00
Response: [SALT_user(16)][SALT_admin(16)]

# 2. Host nhập PIN và tính KEK
PIN_user = input("Enter PIN: ")
KEK_user = Argon2(PIN_user, SALT_user)

# 3. Gửi KEK đến thẻ để verify
APDU: 00 20 00 00 20 [KEK_user]
Response: 9000 (Success) hoặc 6982 (Failed)
```

### Đổi PIN User

```python
# 1. Xác thực user trước (VERIFY_USER_PIN)
# 2. Host tạo PIN mới và salt mới
NEW_PIN_user = "654321"
NEW_SALT_user = random_bytes(16)
KEK_new_user = Argon2(NEW_PIN_user, NEW_SALT_user)

# 3. Gửi lệnh CHANGE_USER_PIN
APDU: 00 30 00 00 30 [KEK_new_user][NEW_SALT_user]
```

### Ghi/Đọc dữ liệu

```python
# 1. Xác thực (VERIFY_USER_PIN hoặc VERIFY_ADMIN_PIN)

# 2. Ghi dữ liệu
data = b"Hello World!"
APDU: 00 50 00 00 [len] [data]

# 3. Đọc dữ liệu
APDU: 00 40 00 00 00
Response: [decrypted_data]
```

### Reset PIN User bởi Admin

```python
# 1. Xác thực admin (VERIFY_ADMIN_PIN)
# 2. Tạo PIN mới cho user
NEW_PIN_user = "newpass"
NEW_SALT_user = random_bytes(16)
KEK_new_user = Argon2(NEW_PIN_user, NEW_SALT_user)

# 3. Gửi lệnh RESET_USER_PIN
APDU: 00 31 00 00 30 [KEK_new_user][NEW_SALT_user]
```

---

## Đặc điểm bảo mật

### 1. **Master Key Management**
- M được tạo ngẫu nhiên khi khởi tạo (32 bytes AES-256)
- M chỉ tồn tại trong RAM volatile
- M tự động bị xóa khi:
  - Deselect applet
  - Verify PIN sai
  - Power off

### 2. **PIN Protection**
- Counter: tối đa 5 lần thử sai
- Sau 5 lần → khóa (SW_AUTHENTICATION_METHOD_BLOCKED)
- User và admin có counter riêng biệt
- Admin có thể reset counter user

### 3. **Key Encryption Key (KEK)**
- KEK không bao giờ lưu trên thẻ
- KEK chỉ tồn tại tạm thời trong buffer
- KEK được overwrite bằng 0x00 sau sử dụng
- KEK được tính từ Argon2 trên host (KDF mạnh)

### 4. **Data Encryption**
- Sử dụng AES-256 CBC mode
- NoPadding: data phải là bội số của 16 bytes
- Padding tự động bằng 0x00
- Enc_DATA lưu trong EEPROM

### 5. **Authentication States**
- userAuthenticated: cho phép GET_DATA, SET_DATA, CHANGE_USER_PIN
- adminAuthenticated: cho phép GET_DATA, SET_DATA, RESET_USER_PIN
- Cả hai flag đều reset khi deselect

---

## Giới hạn kỹ thuật

| Thông số | Giá trị |
|----------|---------|
| Master Key length | 32 bytes (AES-256) |
| SALT length | 16 bytes |
| KEK length | 32 bytes (AES-256) |
| Max data length | 512 bytes |
| AES block size | 16 bytes |
| Max PIN tries | 5 lần |
| Hash algorithm | SHA-256 |
| Cipher mode | AES/CBC/NoPadding |

---

## Response Codes

| Code | Mô tả |
|------|-------|
| `9000` | Success |
| `6700` | Wrong length (LC sai) |
| `6982` | Security status not satisfied (chưa xác thực) |
| `6983` | Authentication method blocked (khóa sau 5 lần sai) |
| `6A82` | File not found (chưa có data) |
| `6D00` | Instruction not supported (INS không hợp lệ) |

---

## Lưu ý quan trọng cho Host Developer

### ✅ BẮT BUỘC
1. **Argon2 trên Host:** Thẻ KHÔNG thực hiện Argon2, host phải tính KEK
2. **KEK = 32 bytes:** Luôn đảm bảo KEK từ Argon2 có độ dài 32 bytes
3. **SALT management:** Lưu SALT hoặc lấy từ GET_SALT mỗi lần verify
4. **Data padding:** Data ghi vào phải pad đến bội số 16 bytes
5. **Session management:** Sau deselect, phải verify lại PIN

### ⚠️ KHUYẾN CÁO
1. **Argon2 parameters:** Sử dụng tham số phù hợp (time=3, memory=65536, parallelism=4)
2. **SALT random:** SALT phải được tạo bằng CSPRNG
3. **Secure input:** Che PIN khi nhập trên host
4. **Error handling:** Kiểm tra response code và xử lý lỗi đúng cách
5. **Counter monitoring:** Cảnh báo user khi còn ít lần thử PIN

### 🚫 CẤM
1. Không gửi PIN trực tiếp đến thẻ (chỉ gửi KEK)
2. Không lưu KEK persistent trên host
3. Không gửi data > 512 bytes
4. Không gọi INITIALIZE nhiều lần (sẽ ghi đè M cũ)

---

## Ví dụ code Python (Host)

```python
from smartcard.System import readers
from smartcard.util import toHexString, toBytes
import hashlib
from argon2.low_level import hash_secret_raw, Type

# Kết nối thẻ
r = readers()[0]
connection = r.createConnection()
connection.connect()

# SELECT applet
AID = [0xA0, 0x00, 0x00, 0x00, 0x62, 0x03, 0x01, 0x0C, 0x06, 0x01]
SELECT = [0x00, 0xA4, 0x04, 0x00, len(AID)] + AID
data, sw1, sw2 = connection.transmit(SELECT)

# Hàm Argon2
def compute_kek(pin, salt):
    kek = hash_secret_raw(
        secret=pin.encode('utf-8'),
        salt=salt,
        time_cost=3,
        memory_cost=65536,
        parallelism=4,
        hash_len=32,
        type=Type.I
    )
    return kek

# 1. Khởi tạo thẻ
import os
SALT_user = os.urandom(16)
SALT_admin = os.urandom(16)
KEK_user = compute_kek("123456", SALT_user)
KEK_admin = compute_kek("admin123", SALT_admin)

INITIALIZE = [0x00, 0x08, 0x00, 0x00, 0x08]
INITIALIZE += list(KEK_user) + list(SALT_user)
INITIALIZE += list(KEK_admin) + list(SALT_admin)
data, sw1, sw2 = connection.transmit(INITIALIZE)
print(f"Initialize: {sw1:02X}{sw2:02X}")

# 2. Lấy SALT
GET_SALT = [0x00, 0x01, 0x00, 0x00, 0x00]
data, sw1, sw2 = connection.transmit(GET_SALT)
SALT_user_card = bytes(data[0:16])
SALT_admin_card = bytes(data[16:32])

# 3. Verify user PIN
pin = input("Enter user PIN: ")
KEK_user = compute_kek(pin, SALT_user_card)
VERIFY = [0x00, 0x02, 0x00, 0x00, 0x02] + list(KEK_user)
data, sw1, sw2 = connection.transmit(VERIFY)
if sw1 == 0x90 and sw2 == 0x00:
    print("✓ User authenticated")
else:
    print(f"✗ Authentication failed: {sw1:02X}{sw2:02X}")

# 4. Ghi dữ liệu
plaintext = b"Secret message"
SET_DATA = [0x00, 0x07, 0x00, 0x00, len(plaintext)] + list(plaintext)
data, sw1, sw2 = connection.transmit(SET_DATA)

# 5. Đọc dữ liệu
GET_DATA = [0x00, 0x06, 0x00, 0x00, 0x00]
data, sw1, sw2 = connection.transmit(GET_DATA)
print(f"Data: {bytes(data).rstrip(b'\\x00')}")
```

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-11-28 | Initial release |

---

## Liên hệ & Hỗ trợ

- **Developer:** Entertainment Package
- **JavaCard Version:** 3.0.4+
- **Applet Class:** `Entertainment.test2`

**Lưu ý:** Đây là tài liệu kỹ thuật cho host developer. Đọc kỹ phần bảo mật và giới hạn kỹ thuật trước khi triển khai.
