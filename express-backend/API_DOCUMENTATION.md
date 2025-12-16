# SmartCard Backend API Documentation

Server đang chạy tại: `http://localhost:4000`

## Collections

### 1. Games API
**Base URL:** `/api/games`

#### GET - Lấy tất cả games
```
GET /api/games
Response: { success: true, data: [...] }
```

#### GET - Lấy game theo ID
```
GET /api/games/:id
Response: { success: true, data: {...} }
```

#### POST - Tạo game mới
```
POST /api/games
Body: {
  "name": "Game Name",
  "price": 100000,
  "description": "Game description"
}
Response: { success: true, data: {...} }
```

#### PUT - Cập nhật game
```
PUT /api/games/:id
Body: {
  "name": "Updated Name",
  "price": 150000,
  "description": "Updated description"
}
Response: { success: true, data: {...} }
```

#### DELETE - Xóa game
```
DELETE /api/games/:id
Response: { success: true, message: "Game deleted successfully" }
```

---

### 2. Combos API
**Base URL:** `/api/combos`

#### GET - Lấy tất cả combos
```
GET /api/combos
Response: { success: true, data: [...] }
```

#### GET - Lấy combo theo ID
```
GET /api/combos/:id
Response: { success: true, data: {...} }
```

#### POST - Tạo combo mới
```
POST /api/combos
Body: {
  "name": "Combo Name",
  "price": 250000,
  "description": "Combo description",
  "game_ids": ["gameId1", "gameId2"]
}
Response: { success: true, data: {...} }
```

#### PUT - Cập nhật combo
```
PUT /api/combos/:id
Body: {
  "name": "Updated Combo",
  "price": 300000,
  "description": "Updated description",
  "game_ids": ["gameId1", "gameId3"]
}
Response: { success: true, data: {...} }
```

#### DELETE - Xóa combo
```
DELETE /api/combos/:id
Response: { success: true, message: "Combo deleted successfully" }
```

---

### 3. Cards API
**Base URL:** `/api/cards`

#### GET - Lấy tất cả cards
```
GET /api/cards
Response: { success: true, data: [...] }
```

#### GET - Lấy card theo ID
```
GET /api/cards/:id
Response: { success: true, data: {...} }
```

#### POST - Tạo card mới
```
POST /api/cards
Body: {
  "user_name": "Nguyen Van A",
  "user_age": 25,
  "user_gender": true,
  "public_key": "unique_public_key_123"
}
Response: { success: true, data: {...} }
```

#### PUT - Cập nhật card
```
PUT /api/cards/:id
Body: {
  "user_name": "Nguyen Van B",
  "user_age": 26,
  "user_gender": false,
  "public_key": "updated_public_key_456"
}
Response: { success: true, data: {...} }
```

#### DELETE - Xóa card
```
DELETE /api/cards/:id
Response: { success: true, message: "Card deleted successfully" }
```

---

### 4. Transactions API
**Base URL:** `/api/transactions`

#### GET - Lấy tất cả transactions
```
GET /api/transactions
Response: { success: true, data: [...] }
```

#### GET - Lấy transaction theo ID
```
GET /api/transactions/:id
Response: { success: true, data: {...} }
```

#### GET - Lấy transactions theo card ID
```
GET /api/transactions/card/:cardId
Response: { success: true, data: [...] }
```

#### POST - Tạo transaction mới
```
POST /api/transactions
Body: {
  "card_id": "cardId123",
  "payment": 150000,
  "combo_id": "comboId456" // Optional
}
Response: { success: true, data: {...} }
```

#### PUT - Cập nhật transaction
```
PUT /api/transactions/:id
Body: {
  "payment": 200000,
  "combo_id": "comboId789"
}
Response: { success: true, data: {...} }
```

#### DELETE - Xóa transaction
```
DELETE /api/transactions/:id
Response: { success: true, message: "Transaction deleted successfully" }
```

---

## Lưu ý

- Tất cả response đều có format: `{ success: boolean, data?: any, message?: string }`
- Combo populate thông tin games khi query
- Transaction populate thông tin card và combo khi query
- user_gender: `true` = Nam, `false` = Nữ
- time_stamp được tự động thêm khi tạo transaction
- public_key phải unique cho mỗi card

## Test API

Bạn có thể test API bằng Postman, Thunder Client, hoặc curl:

```bash
# Test endpoint root
curl http://localhost:4000/

# Test get all games
curl http://localhost:4000/api/games
```
