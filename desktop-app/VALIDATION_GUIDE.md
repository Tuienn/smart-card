# Input Validation Guide

## Tổng quan

Hệ thống validation được thiết kế để đảm bảo dữ liệu đầu vào hợp lệ với các tính năng:

- ✅ **Real-time validation**: Kiểm tra ngay khi người dùng nhập
- ✅ **Visual feedback**: Hiển thị màu đỏ/xanh và tooltip
- ✅ **Auto-filtering**: Tự động lọc ký tự không hợp lệ
- ✅ **Length limiting**: Giới hạn độ dài tự động

## Các trường được validate

### 1. Tên (Name Field)
**Quy tắc:**
- ✓ Độ dài: 2-64 ký tự
- ✓ Chỉ chấp nhận: chữ cái (bao gồm tiếng Việt) và khoảng trắng
- ✓ Không chứa khoảng trắng liên tiếp
- ✓ Phải có ít nhất 1 chữ cái
- ✗ Không chấp nhận: số, ký tự đặc biệt

**Ví dụ hợp lệ:**
```
Nguyễn Văn A
Trần Thị Bích Ngọc
Lê Hoàng
```

**Ví dụ không hợp lệ:**
```
A               (quá ngắn)
Nguyễn  Văn     (khoảng trắng liên tiếp)
123             (không có chữ cái)
Nguyễn@123      (chứa ký tự đặc biệt)
```

### 2. Tuổi (Age Field)
**Quy tắc:**
- ✓ Chỉ chấp nhận số
- ✓ Phạm vi: 1-150
- ✓ Tối đa 3 chữ số
- ✗ Không chấp nhận: chữ cái, ký tự đặc biệt

**Ví dụ hợp lệ:**
```
18
25
100
```

**Ví dụ không hợp lệ:**
```
0               (nhỏ hơn 1)
151             (lớn hơn 150)
abc             (không phải số)
-5              (số âm)
```

### 3. Số tiền (Amount Field)
**Quy tắc:**
- ✓ Chỉ chấp nhận số
- ✓ Phạm vi: 10,000 - 50,000,000 VNĐ
- ✓ Phải là bội số của 10,000
- ✓ Tối đa 8 chữ số

**Ví dụ hợp lệ:**
```
10000
100000
1000000
50000000
```

**Ví dụ không hợp lệ:**
```
5000            (nhỏ hơn 10,000)
15000           (không phải bội số của 10,000)
100000000       (vượt quá giới hạn)
abc123          (chứa chữ cái)
```

## Cách sử dụng trong code

### Setup validation trong Controller

```java
import com.example.desktopapp.util.InputValidator;

@Override
public void initialize(URL url, ResourceBundle resourceBundle) {
    // Setup validation cho các trường
    InputValidator.setupNameValidation(nameField);
    InputValidator.setupAgeValidation(ageField);
    InputValidator.setupAmountValidation(customAmountField);
}
```

### Validate trước khi submit

```java
@FXML
private void onSubmit() {
    // Validate tất cả các trường
    if (!InputValidator.validateName(nameField)) {
        nameField.requestFocus();
        return;
    }
    
    if (!InputValidator.validateAge(ageField)) {
        ageField.requestFocus();
        return;
    }
    
    // Tiếp tục xử lý nếu tất cả hợp lệ
    processData();
}
```

### Check validation không áp dụng style

```java
String name = "Nguyễn Văn A";
if (InputValidator.isValidName(name)) {
    // Tên hợp lệ
}

int age = 25;
if (InputValidator.isValidAge(age)) {
    // Tuổi hợp lệ
}

int amount = 100000;
if (InputValidator.isValidAmount(amount)) {
    // Số tiền hợp lệ
}
```

## Visual Feedback

### Trạng thái hợp lệ (Valid)
- **Border màu xanh lá** (#22c55e)
- Không có tooltip lỗi

### Trạng thái không hợp lệ (Invalid)
- **Border màu đỏ** (#ef4444)
- **Background màu đỏ nhạt** (rgba(239, 68, 68, 0.1))
- **Tooltip hiển thị lỗi** khi hover

### Trạng thái mặc định
- Border trong suốt
- Background xám (#334155)

## Custom Validation

Nếu cần thêm validation rule mới, thêm vào `InputValidator.java`:

```java
public static void setupCustomValidation(TextField field) {
    field.textProperty().addListener((obs, oldVal, newVal) -> {
        // Custom filtering logic
        if (!newVal.matches("your-regex")) {
            field.setText(oldVal);
        }
    });
    
    field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
        if (!isNowFocused) {
            validateCustom(field);
        }
    });
}

public static boolean validateCustom(TextField field) {
    String value = field.getText().trim();
    
    if (!isValidCustom(value)) {
        setInvalidStyle(field, "Custom error message");
        return false;
    }
    
    setValidStyle(field);
    return true;
}
```

## Constants

Các hằng số validation được định nghĩa trong `InputValidator`:

```java
public static final int MIN_NAME_LENGTH = 2;
public static final int MAX_NAME_LENGTH = 64;
public static final int MIN_AGE = 1;
public static final int MAX_AGE = 150;
public static final int MIN_AMOUNT = 10000;
public static final int MAX_AMOUNT = 50000000;
```

## Lưu ý

1. **Real-time filtering**: Các ký tự không hợp lệ sẽ được tự động loại bỏ ngay khi nhập
2. **Validation on blur**: Kiểm tra đầy đủ khi người dùng rời khỏi field
3. **Auto-focus**: Tự động focus vào field đầu tiên bị lỗi
4. **Tooltip position**: Tooltip hiển thị khi hover vào field có lỗi
5. **Thread-safe**: Validation chạy trên JavaFX Application Thread

## Xử lý lỗi

Khi validation thất bại:
1. Field sẽ có border màu đỏ
2. Tooltip hiển thị lý do lỗi
3. Method trả về `false`
4. Focus được set vào field lỗi

Khi validation thành công:
1. Field có border màu xanh
2. Tooltip được xóa
3. Method trả về `true`
4. Có thể tiếp tục xử lý
