1. Mục tiêu hệ thống

Cho phép người dùng tự đăng ký thông tin cá nhân.

Chọn gói chơi / nạp tiền.

Thanh toán thử (fake) để mô phỏng giao dịch.

Sinh thẻ vào chơi (QR/Barcode) dùng để quẹt tại cổng & máy trò chơi.

Backend lưu lại dữ liệu người dùng + lịch sử giao dịch.

Tương thích với hệ thống trò chơi trừ tiền theo lượt.

2. Luồng người chơi
   1️⃣ Nhập thông tin người dùng

Người chơi nhập:

Họ tên

Tuổi

Giới tính

Tạo mã pin

Ảnh đại diện

Nhập số tiền thanh toán (quy đôi 100k = 10 coin)

2️⃣ Thanh toán thử

desktop app fake data api lên server và lưu giao dịch lại

3️⃣ Sinh thẻ vào chơi

desktop nhận dữ liệu từ server, gọi api để lấy ra QR và spam 1s 1 lần api lấy trạng thái thanh toán, nếu hợp lệ sẽ ghi dữ liệu vào thẻ (số coin, thông tin người dùng, mã để xác thực mã pin)

4️⃣ Sử dụng thẻ tại khu vui chơi

Nhập mã pin để xác minh -> Trừ coin trong thẻ (không gọi API trừ tiền)
