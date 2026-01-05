package com.example.desktopapp.util;

import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

/**
 * Utility class for input validation with visual feedback
 */
public class InputValidator {
    
    // Validation styles
    private static final String VALID_STYLE = "-fx-border-color: #22c55e; -fx-border-width: 2px; -fx-border-radius: 4px;";
    private static final String INVALID_STYLE = "-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 4px;";
    private static final String DEFAULT_STYLE = "";
    
    // Validation constants
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 64;
    public static final int MIN_AGE = 1;
    public static final int MAX_AGE = 150;
    public static final int MIN_AMOUNT = 10000;
    public static final int MAX_AMOUNT = 50000000;
    
    /**
     * Setup name field validation
     */
    public static void setupNameValidation(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            // Remove leading spaces
            if (newVal.startsWith(" ")) {
                field.setText(newVal.trim());
                return;
            }
            
            // Limit length
            if (newVal.length() > MAX_NAME_LENGTH) {
                field.setText(oldVal);
                return;
            }
            
            // Allow only letters, spaces, and Vietnamese characters
            String filtered = newVal.replaceAll("[^a-zA-ZÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚĂĐĨŨƠàáâãèéêìíòóôõùúăđĩũơƯĂẠẢẤẦẨẪẬẮẰẲẴẶẸẺẼỀỀỂưăạảấầẩẫậắằẳẵặẹẻẽềềểỄỆỈỊỌỎỐỒỔỖỘỚỜỞỠỢỤỦỨỪễệỉịọỏốồổỗộớờởỡợụủứừỬỮỰỲỴÝỶỸửữựỳỵýỷỹ\\s]", "");
            if (!filtered.equals(newVal)) {
                field.setText(filtered);
            }
        });
        
        // Validate on focus lost
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                validateName(field);
            }
        });
    }
    
    /**
     * Setup age field validation - only allow numbers
     */
    public static void setupAgeValidation(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            // Only allow digits
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            
            // Limit to 3 digits (max 150)
            if (newVal.length() > 3) {
                field.setText(oldVal);
                return;
            }
            
            // Check range if not empty
            if (!newVal.isEmpty()) {
                try {
                    int age = Integer.parseInt(newVal);
                    if (age > MAX_AGE) {
                        field.setText(oldVal);
                    }
                } catch (NumberFormatException e) {
                    field.setText(oldVal);
                }
            }
        });
        
        // Validate on focus lost
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                validateAge(field);
            }
        });
    }
    
    /**
     * Setup amount field validation - only allow numbers
     */
    public static void setupAmountValidation(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            // Only allow digits
            if (!newVal.matches("\\d*")) {
                field.setText(newVal.replaceAll("[^\\d]", ""));
                return;
            }
            
            // Limit to reasonable length (50 million max = 8 digits)
            if (newVal.length() > 8) {
                field.setText(oldVal);
            }
        });
        
        // Validate on focus lost
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                validateAmount(field);
            }
        });
    }
    
    /**
     * Validate name field
     */
    public static boolean validateName(TextField field) {
        String name = field.getText().trim();
        
        if (name.isEmpty()) {
            setInvalidStyle(field, "Tên không được để trống");
            return false;
        }
        
        if (name.length() < MIN_NAME_LENGTH) {
            setInvalidStyle(field, "Tên phải có ít nhất " + MIN_NAME_LENGTH + " ký tự");
            return false;
        }
        
        if (name.length() > MAX_NAME_LENGTH) {
            setInvalidStyle(field, "Tên không được vượt quá " + MAX_NAME_LENGTH + " ký tự");
            return false;
        }
        
        // Check for consecutive spaces
        if (name.contains("  ")) {
            setInvalidStyle(field, "Tên không được chứa khoảng trắng liên tiếp");
            return false;
        }
        
        // Must contain at least one letter
        if (!name.matches(".*[a-zA-ZÀ-ỹ].*")) {
            setInvalidStyle(field, "Tên phải chứa ít nhất một chữ cái");
            return false;
        }
        
        setValidStyle(field);
        return true;
    }
    
    /**
     * Validate age field
     */
    public static boolean validateAge(TextField field) {
        String ageStr = field.getText().trim();
        
        if (ageStr.isEmpty()) {
            setInvalidStyle(field, "Tuổi không được để trống");
            return false;
        }
        
        try {
            int age = Integer.parseInt(ageStr);
            
            if (age < MIN_AGE) {
                setInvalidStyle(field, "Tuổi phải từ " + MIN_AGE + " trở lên");
                return false;
            }
            
            if (age > MAX_AGE) {
                setInvalidStyle(field, "Tuổi không được vượt quá " + MAX_AGE);
                return false;
            }
            
            setValidStyle(field);
            return true;
            
        } catch (NumberFormatException e) {
            setInvalidStyle(field, "Tuổi phải là số");
            return false;
        }
    }
    
    /**
     * Validate amount field
     */
    public static boolean validateAmount(TextField field) {
        String amountStr = field.getText().trim();
        
        if (amountStr.isEmpty()) {
            setInvalidStyle(field, "Số tiền không được để trống");
            return false;
        }
        
        try {
            int amount = Integer.parseInt(amountStr);
            
            if (amount < MIN_AMOUNT) {
                setInvalidStyle(field, "Số tiền tối thiểu là " + formatCurrency(MIN_AMOUNT));
                return false;
            }
            
            if (amount > MAX_AMOUNT) {
                setInvalidStyle(field, "Số tiền tối đa là " + formatCurrency(MAX_AMOUNT));
                return false;
            }
            
            // Amount must be multiple of 10,000
            if (amount % 10000 != 0) {
                setInvalidStyle(field, "Số tiền phải là bội số của 10,000đ");
                return false;
            }
            
            setValidStyle(field);
            return true;
            
        } catch (NumberFormatException e) {
            setInvalidStyle(field, "Số tiền phải là số");
            return false;
        }
    }
    
    /**
     * Set valid style
     */
    private static void setValidStyle(TextField field) {
        field.setStyle(VALID_STYLE);
        field.setTooltip(null);
    }
    
    /**
     * Set invalid style with error message
     */
    private static void setInvalidStyle(TextField field, String message) {
        field.setStyle(INVALID_STYLE);
        Tooltip tooltip = new Tooltip(message);
        tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #ef4444; -fx-text-fill: white;");
        field.setTooltip(tooltip);
    }
    
    /**
     * Reset field style to default
     */
    public static void resetStyle(TextField field) {
        field.setStyle(DEFAULT_STYLE);
        field.setTooltip(null);
    }
    
    /**
     * Format currency
     */
    private static String formatCurrency(int amount) {
        return String.format("%,dđ", amount);
    }
    
    /**
     * Check if name is valid without applying styles
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        name = name.trim();
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) return false;
        if (name.contains("  ")) return false;
        if (!name.matches(".*[a-zA-ZÀ-ỹ].*")) return false;
        return true;
    }
    
    /**
     * Check if age is valid without applying styles
     */
    public static boolean isValidAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }
    
    /**
     * Check if amount is valid without applying styles
     */
    public static boolean isValidAmount(int amount) {
        return amount >= MIN_AMOUNT && amount <= MAX_AMOUNT && amount % 10000 == 0;
    }
}
