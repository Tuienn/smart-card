# SmartCard API Testing với Postman

## Mô tả
Collection này chứa tất cả các API endpoints để test ứng dụng SmartCard, bao gồm:
- Kết nối/ngắt kết nối thẻ
- Khởi tạo thẻ
- Xác thực PIN (user/admin)
- Đọc/ghi dữ liệu
- Đổi PIN và reset PIN

## Cách import vào Postman

### 1. Import Collection
1. Mở Postman
2. Click **Import** 
3. Chọn file `SmartCard_API_Collection.postman_collection.json`
4. Click **Import**

### 2. Import Environment
1. Click **Import** tiếp
2. Chọn file `SmartCard_Local.postman_environment.json`
3. Click **Import**
4. Chọn environment **SmartCard Local Environment** ở góc trên bên phải

## Thứ tự test được khuyến nghị

### Luồng cơ bản:
1. **Kết nối thẻ** - Thiết lập kết nối với smart card
2. **Khởi tạo thẻ** - Setup PIN ban đầu (userPin: 123456, adminPin: admin123)
3. **Lấy SALT** - Lấy giá trị SALT từ thẻ (tự động lưu vào variables)
4. **Xác thực User PIN** - Verify user để có quyền đọc/ghi data
5. **Đọc dữ liệu** - Đọc dữ liệu hiện tại từ thẻ
6. **Ghi dữ liệu** - Ghi dữ liệu mới vào thẻ
7. **Ngắt kết nối** - Kết thúc phiên làm việc

### Luồng quản lý PIN:
1. **Kết nối thẻ**
2. **Xác thực User PIN** 
3. **Đổi User PIN** - Đổi từ 123456 → newpass123
4. **Xác thực Admin PIN**
5. **Reset User PIN** - Admin reset PIN về 123456
6. **Ngắt kết nối**

## Variables sử dụng

### Collection Variables:
- `baseUrl`: http://localhost:8080
- `userPin`: 123456 (sẽ tự động update khi đổi PIN)
- `adminPin`: admin123
- `newUserPin`: newpass123

### Tự động lưu:
- `userSalt`: SALT của user (tự động lưu sau khi gọi /salt)
- `adminSalt`: SALT của admin (tự động lưu sau khi gọi /salt)

## Test Scripts

Mỗi request đều có built-in test scripts để:
- Kiểm tra status code
- Validate cấu trúc response
- Test logic nghiệp vụ
- Tự động update variables khi cần

## Lưu ý quan trọng

1. **Thứ tự quan trọng**: Một số API yêu cầu verify PIN trước (như đọc/ghi dữ liệu)
2. **State management**: Application có thể lưu trạng thái verify PIN trong session
3. **Error handling**: Nếu gặp lỗi 401, hãy verify PIN trước
4. **Environment**: Đảm bảo đã chọn đúng environment và Spring Boot app đang chạy trên port 8080

## Chạy toàn bộ collection

1. Click vào collection name **SmartCard API Collection**
2. Click **Run**
3. Chọn các request muốn chạy
4. Click **Run SmartCard API Collection**

## Troubleshooting

### Lỗi kết nối (Connection refused)
- Kiểm tra Spring Boot app có đang chạy không
- Kiểm tra port 8080 có bị occupied không
- Kiểm tra `baseUrl` trong environment

### Lỗi 401 (Unauthorized)
- Chạy "Xác thực User PIN" hoặc "Xác thực Admin PIN" trước
- Kiểm tra PIN có đúng không

### Lỗi smart card
- Kiểm tra smart card reader có kết nối không
- Kiểm tra smart card có được cắm đúng không
- Chạy "Kết nối thẻ" trước các API khác