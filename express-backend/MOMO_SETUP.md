# MoMo Payment Integration - Setup Guide

## 🔧 Development Mode (Mock Payment)

Để test chức năng thanh toán mà không cần MoMo credentials thật, set trong `.env`:

```env
MOMO_MOCK_MODE=true
```

### Mock Flow:
1. **Tạo QR**: POST `/api/momo/qr` → Trả về mock QR code
2. **Xác nhận thanh toán**: POST `/api/momo/confirm/:orderId` → Giả lập thanh toán thành công
3. **Kiểm tra status**: GET `/api/momo/status/:orderId` → Xem trạng thái

### Ví dụ test với Mock Mode:

```bash
# 1. Tạo QR code (mock)
curl -X POST http://localhost:4000/api/momo/qr \
  -H "Content-Type: application/json" \
  -d '{"amount": "100000", "description": "TEST123"}'

# Response sẽ có orderId, ví dụ: QR_1234567890

# 2. Giả lập thanh toán thành công
curl -X POST http://localhost:4000/api/momo/confirm/QR_1234567890

# 3. Kiểm tra trạng thái
curl http://localhost:4000/api/momo/status/QR_1234567890
```

---

## 🏦 Production Mode (Real MoMo)

### Bước 1: Đăng ký tài khoản MoMo Business

1. Truy cập: https://business.momo.vn
2. Đăng ký tài khoản doanh nghiệp
3. Hoàn tất xác minh KYC

### Bước 2: Lấy API Credentials

1. Đăng nhập MoMo Business Portal
2. Vào **Cài đặt** > **API Configuration**
3. Tạo App mới hoặc xem credentials của app hiện tại
4. Copy các thông tin:
   - Partner Code
   - Access Key
   - Secret Key

### Bước 3: Cấu hình `.env`

```env
# Tắt mock mode
MOMO_MOCK_MODE=false

# Điền credentials thật từ MoMo Portal
MOMO_PARTNER_CODE=YOUR_PARTNER_CODE
MOMO_ACCESS_KEY=YOUR_ACCESS_KEY
MOMO_SECRET_KEY=YOUR_SECRET_KEY

# Endpoint production
MOMO_ENDPOINT=https://payment.momo.vn/v2/gateway/api/create

# URL callback (cần public URL, dùng ngrok cho dev)
MOMO_REDIRECT_URL=https://your-domain.com
```

### Bước 4: Setup IPN Callback với ngrok (Development)

IPN (Instant Payment Notification) là webhook MoMo gọi về khi thanh toán thành công.

```bash
# 1. Cài ngrok
npm install -g ngrok

# 2. Chạy ngrok
ngrok http 4000

# 3. Copy HTTPS URL (ví dụ: https://abc123.ngrok.io)

# 4. Update .env
MOMO_REDIRECT_URL=https://abc123.ngrok.io
```

### Bước 5: Cấu hình IPN URL trong MoMo Portal

1. Vào MoMo Business Portal
2. **App Settings** > **IPN URL**
3. Nhập: `https://abc123.ngrok.io/api/momo/ipn`
4. Verify URL

---

## 📊 Test Credentials (Sandbox)

⚠️ **LƯU Ý**: Credentials test công khai thường bị disable hoặc hết hạn.

Nếu bạn muốn test trên sandbox của MoMo:
1. Đăng ký tài khoản test tại: https://developers.momo.vn
2. Lấy credentials test riêng của bạn
3. Sử dụng endpoint sandbox: `https://test-payment.momo.vn/v2/gateway/api/create`

---

## 🔍 Troubleshooting

### Error 11007: "Chữ ký không hợp lệ"

**Nguyên nhân:**
- Secret Key sai
- Credentials test công khai đã hết hạn
- Thứ tự tham số trong raw signature sai

**Giải pháp:**
1. Dùng Mock Mode: `MOMO_MOCK_MODE=true`
2. HOẶC đăng ký credentials thật từ MoMo Business

### QR Code không hiển thị

- Kiểm tra `qrCodeUrl` trong response
- Đảm bảo có internet để load QR từ api.qrserver.com

### IPN không được gọi

- Đảm bảo ngrok đang chạy
- Kiểm tra `MOMO_REDIRECT_URL` trong `.env`
- Verify IPN URL trong MoMo Portal

---

## 📱 Flow hoàn chỉnh

```
[Desktop App] 
    ↓ POST /api/momo/qr
[Backend] → Tạo QR code
    ↓ Trả về qrCodeUrl
[Desktop App] → Hiển thị QR
    ↓ 
[User] → Quét QR bằng MoMo app
    ↓
[MoMo] → Xử lý thanh toán
    ↓ POST /api/momo/ipn (callback)
[Backend] → Cập nhật status = "success"
    ↓
[Desktop App] → Polling /api/momo/status/:orderId
    ↓ status = "success"
[Desktop App] → Ghi dữ liệu vào thẻ
```

---

## 🎯 Khuyến nghị

- **Development**: Dùng Mock Mode
- **Staging**: Dùng MoMo Sandbox với credentials test riêng
- **Production**: Dùng MoMo Production với credentials thật
