# Rules - Desktop App

## 1. Icon Usage (ikonli FontAwesome5)

### Thư viện icon

- Sử dụng thư viện **ikonli** với **FontAwesome5** cho tất cả các icon
- Import trong FXML: `<?import org.kordamp.ikonli.javafx.FontIcon?>`
- Import trong Java:
  ```java
  import org.kordamp.ikonli.javafx.FontIcon;
  import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
  ```

### Cú pháp sử dụng trong FXML

```xml
<FontIcon iconLiteral="fas-arrow-right" iconSize="24" styleClass="icon-white"/>
```

### Cú pháp sử dụng trong Java

```java
FontIcon icon = new FontIcon(FontAwesomeSolid.CHECK);
icon.setIconSize(14);
icon.setIconColor(javafx.scene.paint.Color.web("#22c55e"));
```

### Icon thường dùng

| Icon | Literal            | Mô tả                 |
| ---- | ------------------ | --------------------- |
| ➡️   | `fas-arrow-right`  | Mũi tên phải          |
| ⬅️   | `fas-arrow-left`   | Mũi tên trái          |
| ✓    | `fas-check`        | Dấu check             |
| ✓    | `fas-check-circle` | Check trong vòng tròn |
| ✗    | `fas-times-circle` | X trong vòng tròn     |

---

## 2. Icon Color Classes

### Quy tắc màu icon

- **KHÔNG** sử dụng inline style `style="-fx-icon-color: ..."` vì có thể gây lỗi hiển thị icon
- **SỬ DỤNG** CSS class để thiết lập màu icon

### Các class màu có sẵn (trong styles.css)

| Class             | Màu       | Sử dụng cho               |
| ----------------- | --------- | ------------------------- |
| `.icon-white`     | `white`   | Icon trong button primary |
| `.icon-primary`   | `#6366f1` | Icon chính, accent        |
| `.icon-secondary` | `#9ca3af` | Icon phụ, mờ              |
| `.icon-success`   | `#22c55e` | Icon thành công           |
| `.icon-danger`    | `#ef4444` | Icon lỗi, xóa             |
| `.icon-warning`   | `#fbbf24` | Icon cảnh báo, coins      |

### Ví dụ sử dụng

```xml
<!-- Đúng ✓ -->
<FontIcon iconLiteral="fas-check" iconSize="16" styleClass="icon-success"/>

<!-- Sai ✗ -->
<FontIcon iconLiteral="fas-check" iconSize="16" style="-fx-icon-color: #22c55e;"/>
```

---

## 3. Text Case Convention (Sentence case)

### Quy tắc

- Sử dụng **Sentence case** cho tất cả các text hiển thị
- **Sentence case**: Chỉ viết hoa chữ cái đầu tiên của câu/cụm từ
- **KHÔNG** sử dụng Title Case (Viết Hoa Mỗi Từ)

### Ví dụ

| ❌ Sai (Title Case) | ✓ Đúng (Sentence case) |
| ------------------- | ---------------------- |
| Đăng Ký Thông Tin   | Đăng ký thông tin      |
| Họ Và Tên           | Họ và tên              |

### Ngoại lệ

- **Tên riêng** vẫn viết hoa: "Nguyễn Văn A"
- **Từ viết tắt** vẫn viết hoa: "PIN", "VNĐ", "COINS"
- **Button action** có thể ALL CAPS: "TIẾP TỤC", "BẮT ĐẦU GHI THẺ"

---

## 4. Module Requirements

Trong `module-info.java`, cần có:

```java
requires org.kordamp.ikonli.javafx;
requires org.kordamp.ikonli.fontawesome5;
```

---

## 5. Maven Dependencies

Trong `pom.xml`, cần có:

```xml
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-javafx</artifactId>
    <version>12.3.1</version>
</dependency>
<dependency>
    <groupId>org.kordamp.ikonli</groupId>
    <artifactId>ikonli-fontawesome5-pack</artifactId>
    <version>12.3.1</version>
</dependency>
```

## 6. Không được build trong chat command
