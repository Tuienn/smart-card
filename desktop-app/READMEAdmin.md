# Admin App - Hệ thống Quản trị Thẻ

## Tổng quan
**AdminApp** là ứng dụng quản trị dành cho Admin để quản lý và hỗ trợ thẻ người dùng trong hệ thống Smart Card Entertainment.

## Tính năng

### 1. 🔓 Mở khóa thẻ (Unlock Card)
- **Mục đích:** Mở khóa thẻ người dùng bị khóa do nhập sai PIN 3 lần
- **Quy trình:**
  1. Đặt thẻ vào đầu đọc → Kiểm tra thẻ
  2. Nhập Admin PIN để xác thực
  3. Tùy chọn: Đổi PIN mới cho user
  4. Thực hiện mở khóa
- **Kết quả:** Reset PIN counter về 3, bỏ cờ khóa, user có thể dùng lại thẻ

### 2. 🔑 Đổi mật khẩu thẻ (Change PIN)
- **Mục đích:** Thay đổi PIN cho thẻ người dùng
- **Quy trình:**
  1. Đặt thẻ → Kiểm tra thẻ
  2. Nhập Admin PIN để xác thực
  3. Nhập PIN mới và xác nhận PIN mới
  4. Thực hiện đổi mật khẩu
- **Yêu cầu:** PIN mới phải có độ dài 4-16 ký tự

### 3. 🔄 Reset thẻ (Reset Card)
- **Mục đích:** Reset thẻ về trạng thái ban đầu (Xóa toàn bộ dữ liệu)
- **Quy trình:**
  1. Đặt thẻ → Kiểm tra thẻ
  2. Nhập Admin PIN để xác thực
  3. **Xác nhận cẩn thận** (hành động không thể hoàn tác)
  4. Thực hiện reset
- **Cảnh báo:** 
  - ⚠️ Xóa toàn bộ dữ liệu người dùng
  - ⚠️ Xóa số dư coins
  - ⚠️ Xóa danh sách games đã mua
  - ⚠️ Reset master key và RSA keys
  - ⚠️ Thẻ về trạng thái chưa khởi tạo
  - ❌ **KHÔNG THỂ KHÔI PHỤC!**

### 4. 📜 Lịch sử giao dịch (Transaction History)
- **Mục đích:** Xem lịch sử giao dịch của thẻ người dùng
- **Quy trình:**
  1. Nhập Card ID (hoặc đọc từ thẻ)
  2. Nhập Admin PIN để xác thực
  3. Xem danh sách giao dịch từ backend
- **Hiển thị:** Ngày giờ, số tiền, loại giao dịch (nạp tiền/mua combo)

### 5. ℹ️ Thông tin thẻ (Card Info)
- **Trạng thái:** 🚧 Đang phát triển
- **Mục đích:** Xem thông tin chi tiết của thẻ người dùng

### 6. 💰 Quản lý số dư (Manage Balance)
- **Trạng thái:** 🚧 Đang phát triển
- **Mục đích:** Nạp tiền hoặc điều chỉnh số dư cho thẻ

## Khởi chạy AdminApp

### Cách 1: Maven
```bash
cd desktop-app
mvn clean javafx:run -Padmin
```

### Cách 2: IDE (IntelliJ/Eclipse)
1. Mở project `desktop-app`
2. Run class `com.example.desktopapp.AdminApp`
3. Đảm bảo JavaFX SDK đã được cấu hình

### Cách 3: JAR file
```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml \
     -jar AdminApp.jar
```

## Yêu cầu hệ thống
- **Java:** JDK 11 hoặc cao hơn
- **JavaFX:** 17 hoặc cao hơn
- **Card Reader:** jCIDE Simulator với PC/SC enabled
- **Backend:** Express server chạy tại http://localhost:4000

## Admin PIN mặc định
```
Default Admin PIN: 1234567890123456
```
⚠️ **Lưu ý:** Admin PIN được hard-coded trong JavaCard applet và không thể thay đổi từ desktop app.

## Kiến trúc

### Structure
```
AdminApp.java                          # Main application entry
admin-menu.fxml                        # Main menu với 6 function cards
AdminMenuController.java               # Controller cho main menu

# Admin functions
admin-unlock-card.fxml                 # Unlock card UI
AdminUnlockCardController.java         # Unlock card logic

admin-change-pin.fxml                  # Change PIN UI
AdminChangePinController.java          # Change PIN logic

admin-reset-card.fxml                  # Reset card UI
AdminResetCardController.java          # Reset card logic

admin-transaction-history.fxml         # Transaction history UI
AdminTransactionHistoryController.java # Transaction history logic

admin-card-info.fxml                   # Card info UI (placeholder)
AdminCardInfoController.java           # Card info logic (placeholder)

admin-manage-balance.fxml              # Manage balance UI (placeholder)
AdminManageBalanceController.java      # Manage balance logic (placeholder)
```

### Design Pattern
- **3-step wizard:** Tất cả chức năng đều theo mẫu
  1. Step 1: Đặt thẻ và kiểm tra
  2. Step 2: Xác thực Admin PIN
  3. Step 3: Thực hiện chức năng

### APDU Commands Used
- `INS_VERIFY_ADMIN_PIN (0x22)` - Xác thực Admin PIN
- `INS_UNLOCK_BY_ADMIN (0x21)` - Mở khóa thẻ (có thể đổi PIN)
- `INS_RESET_CARD (0x99)` - Reset thẻ về trạng thái ban đầu
- `INS_READ_USER_ID (0x55)` - Đọc User ID từ thẻ

### Backend APIs Used
- `GET /api/transactions/card/:cardId` - Lấy lịch sử giao dịch theo Card ID

## Best Practices

### Bảo mật
1. **Luôn xác thực Admin PIN** trước khi thực hiện bất kỳ thao tác nào
2. **Disconnect card** sau khi hoàn thành để giải phóng tài nguyên
3. **Clear sensitive data** (PIN fields) sau khi sử dụng
4. **Double confirmation** cho các thao tác nguy hiểm (Reset card)

### UX
1. **Step indicator** rõ ràng để user biết đang ở bước nào
2. **Loading state** khi thực hiện APDU commands
3. **Error handling** với message thân thiện
4. **Success feedback** với auto-redirect về menu

### Code
1. **Background threads** cho tất cả card operations để avoid blocking UI
2. **Platform.runLater()** khi update UI từ background thread
3. **Try-catch** đầy đủ cho CardException
4. **Reusable patterns** giữa các controllers

## Troubleshooting

### Card không kết nối được
- Kiểm tra jCIDE simulator đã chạy
- Kiểm tra PC/SC đã được enable trong jCIDE
- Thẻ đã được power on

### Admin PIN sai
- Default PIN: `1234567890123456`
- Nếu nhập sai 3 lần, Admin account cũng bị khóa
- Cần reset applet trong jCIDE để unlock Admin

### Reset card không hoạt động
- Đảm bảo đã verify Admin PIN trước
- Kiểm tra thẻ đã được khởi tạo
- Kiểm tra applet có hỗ trợ INS_RESET_CARD

### Transaction history không hiển thị
- Kiểm tra backend server đang chạy
- Kiểm tra Card ID đúng format (32 hex characters)
- Kiểm tra card đã có giao dịch trong database

## Khác biệt với MainApp và ClientApp

| Feature | MainApp | ClientApp | AdminApp |
|---------|---------|-----------|----------|
| **Mục đích** | Đăng ký thẻ mới | Thanh toán game | Quản trị thẻ |
| **User** | Người chơi mới | Người chơi | Admin |
| **PIN** | User PIN | User PIN | Admin PIN |
| **Chức năng chính** | Install card, Write data | Select game, Pay | Unlock, Reset, View history |
| **Tương tác Backend** | POST /cards, POST /transactions | GET /games | GET /transactions |

## License
MIT License - Entertainment Smart Card System
