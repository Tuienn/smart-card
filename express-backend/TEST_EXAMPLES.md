# Hướng dẫn Test API với Postman

## 📦 Import Postman Collection

1. Mở Postman
2. Click **Import** ở góc trên bên trái
3. Chọn file: `SmartCard_API.postman_collection.json`
4. Click **Import**

## 🧪 Test Flow (Theo thứ tự)

### Bước 1: Kiểm tra server
```
GET http://localhost:4000/
```
Kết quả mong đợi: Thông tin về API endpoints

---

### Bước 2: Tạo Games

**Request 1: Tạo game PUBG**
```http
POST http://localhost:4000/api/games
Content-Type: application/json

{
  "name": "PUBG Mobile",
  "price": 50000,
  "description": "Game battle royale phổ biến nhất hiện nay"
}
```

**Request 2: Tạo game Free Fire**
```http
POST http://localhost:4000/api/games
Content-Type: application/json

{
  "name": "Free Fire",
  "price": 30000,
  "description": "Game sinh tồn 50 người"
}
```

**Request 3: Tạo game Liên Quân**
```http
POST http://localhost:4000/api/games
Content-Type: application/json

{
  "name": "Liên Quân Mobile",
  "price": 40000,
  "description": "Game MOBA 5v5 hàng đầu"
}
```

✅ **Lưu lại các `_id` của games vừa tạo**

---

### Bước 3: Lấy danh sách Games

```http
GET http://localhost:4000/api/games
```

Kết quả: Danh sách tất cả games đã tạo

---

### Bước 4: Tạo Combo

**Thay `GAME_ID_1`, `GAME_ID_2`, `GAME_ID_3` bằng các ID thực tế từ bước 2**

```http
POST http://localhost:4000/api/combos
Content-Type: application/json

{
  "name": "Combo Gaming Pro",
  "price": 100000,
  "description": "Gói combo gồm 3 game hot nhất",
  "game_ids": ["GAME_ID_1", "GAME_ID_2", "GAME_ID_3"]
}
```

✅ **Lưu lại `_id` của combo vừa tạo**

---

### Bước 5: Lấy Combo với thông tin Games

```http
GET http://localhost:4000/api/combos
```

Kết quả: Combo với đầy đủ thông tin games được populate

---

### Bước 6: Tạo Card

**Request 1: Card của Nguyen Van A**
```http
POST http://localhost:4000/api/cards
Content-Type: application/json

{
  "user_name": "Nguyen Van A",
  "user_age": 25,
  "user_gender": true,
  "public_key": "PK_NVA_001_2025"
}
```

**Request 2: Card của Tran Thi B**
```http
POST http://localhost:4000/api/cards
Content-Type: application/json

{
  "user_name": "Tran Thi B",
  "user_age": 22,
  "user_gender": false,
  "public_key": "PK_TTB_002_2025"
}
```

✅ **Lưu lại `_id` của card vừa tạo**

---

### Bước 7: Tạo Transaction

**Thay `CARD_ID` và `COMBO_ID` bằng ID thực tế**

**Transaction 1: Mua combo**
```http
POST http://localhost:4000/api/transactions
Content-Type: application/json

{
  "card_id": "CARD_ID",
  "payment": 100000,
  "combo_id": "COMBO_ID"
}
```

**Transaction 2: Mua lẻ (không có combo)**
```http
POST http://localhost:4000/api/transactions
Content-Type: application/json

{
  "card_id": "CARD_ID",
  "payment": 50000,
  "combo_id": null
}
```

---

### Bước 8: Lấy lịch sử giao dịch theo Card

**Thay `CARD_ID` bằng ID thực tế**
```http
GET http://localhost:4000/api/transactions/card/CARD_ID
```

Kết quả: Tất cả transactions của card đó với thông tin đầy đủ

---

### Bước 9: Update dữ liệu

**Update Game**
```http
PUT http://localhost:4000/api/games/GAME_ID
Content-Type: application/json

{
  "name": "PUBG Mobile VIP",
  "price": 70000,
  "description": "Phiên bản VIP với nhiều tính năng đặc biệt"
}
```

**Update Card**
```http
PUT http://localhost:4000/api/cards/CARD_ID
Content-Type: application/json

{
  "user_name": "Nguyen Van A - VIP",
  "user_age": 26,
  "user_gender": true,
  "public_key": "PK_NVA_001_2025_VIP"
}
```

---

### Bước 10: Delete dữ liệu

⚠️ **Cẩn thận khi xóa! Nên test cuối cùng**

```http
DELETE http://localhost:4000/api/games/GAME_ID
DELETE http://localhost:4000/api/combos/COMBO_ID
DELETE http://localhost:4000/api/cards/CARD_ID
DELETE http://localhost:4000/api/transactions/TRANSACTION_ID
```

---

## 📝 Lưu ý quan trọng

1. **Public Key phải unique**: Không thể tạo 2 card với cùng `public_key`
2. **game_ids trong Combo**: Phải là array các ObjectId hợp lệ của Game
3. **card_id trong Transaction**: Phải là ObjectId hợp lệ của Card
4. **user_gender**: `true` = Nam, `false` = Nữ
5. **time_stamp**: Tự động được tạo, không cần truyền vào

## 🔍 Các trường hợp lỗi để test

### Test lỗi validation:
```http
POST http://localhost:4000/api/games
Content-Type: application/json

{
  "name": "Game Test"
  // Thiếu price và description
}
```
Kết quả: Error 400 với message lỗi

### Test lỗi Not Found:
```http
GET http://localhost:4000/api/games/123456789012345678901234
```
Kết quả: Error 404 - Game not found

### Test lỗi Duplicate Key:
```http
POST http://localhost:4000/api/cards
Content-Type: application/json

{
  "user_name": "Test User",
  "user_age": 20,
  "user_gender": true,
  "public_key": "PK_NVA_001_2025"  // Key đã tồn tại
}
```
Kết quả: Error 400 - Duplicate key error

---

## 🎯 Checklist Test

- [ ] GET all resources (Games, Combos, Cards, Transactions)
- [ ] GET by ID
- [ ] POST create new resources
- [ ] PUT update resources
- [ ] DELETE resources
- [ ] Test populate (Combo với Games, Transaction với Card & Combo)
- [ ] Test validation errors
- [ ] Test 404 errors
- [ ] Test duplicate key error
- [ ] Test GET transactions by card ID

---

## 💡 Tips

1. Sử dụng Postman **Variables** để lưu các ID:
   - Vào Settings → Add variable: `game_id`, `card_id`, `combo_id`, etc.
   - Dùng `{{game_id}}` trong các request

2. Sử dụng Postman **Tests** để tự động lưu ID:
   ```javascript
   var jsonData = pm.response.json();
   pm.environment.set("game_id", jsonData.data._id);
   ```

3. Sử dụng **Collection Runner** để chạy tất cả tests tự động
