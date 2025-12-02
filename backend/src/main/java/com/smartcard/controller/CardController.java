package com.smartcard.controller;

import com.smartcard.dto.*;
import com.smartcard.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/card")
@CrossOrigin(origins = "*") // Cho phép Frontend gọi thoải mái
public class CardController {

    @Autowired
    private CardService cardService;

    /**
     * Kết nối tới thẻ
     */
    @GetMapping("/connect")
    public ResponseEntity<ApiResponse> connect() {
        try {
            String result = cardService.connectToCard();
            return ResponseEntity.ok(new ApiResponse(true, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi kết nối: " + e.getMessage()));
        }
    }

    /**
     * Ngắt kết nối thẻ
     */
    @GetMapping("/disconnect")
    public ResponseEntity<ApiResponse> disconnect() {
        try {
            cardService.disconnect();
            return ResponseEntity.ok(new ApiResponse(true, "Đã ngắt kết nối"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Khởi tạo thẻ lần đầu
     * Body: { "userPin": "123456", "adminPin": "admin123" }
     */
    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse> initialize(@RequestBody InitializeRequest request) {
        try {
            String result = cardService.initialize(request.getUserPin(), request.getAdminPin());
            boolean success = result.contains("thành công");
            return ResponseEntity.ok(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Lấy SALT từ thẻ
     * Response: { "success": true, "message": "...", "data": { "userSalt": "...", "adminSalt": "..." } }
     */
    @GetMapping("/salt")
    public ResponseEntity<ApiResponse> getSalt() {
        try {
            Map<String, String> salts = cardService.getSalt();
            SaltResponse saltResponse = new SaltResponse(
                salts.get("userSalt"),
                salts.get("adminSalt")
            );
            return ResponseEntity.ok(new ApiResponse(true, "Lấy SALT thành công", saltResponse));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Xác thực user PIN
     * Body: { "pin": "123456" }
     */
    @PostMapping("/verify-user-pin")
    public ResponseEntity<ApiResponse> verifyUserPin(@RequestBody VerifyPinRequest request) {
        try {
            String result = cardService.verifyUserPin(request.getPin());
            boolean success = result.contains("thành công");
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Xác thực admin PIN
     * Body: { "pin": "admin123" }
     */
    @PostMapping("/verify-admin-pin")
    public ResponseEntity<ApiResponse> verifyAdminPin(@RequestBody VerifyPinRequest request) {
        try {
            String result = cardService.verifyAdminPin(request.getPin());
            boolean success = result.contains("thành công");
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Đổi PIN user (cần verify user trước)
     * Body: { "newPin": "newpass123" }
     */
    @PostMapping("/change-user-pin")
    public ResponseEntity<ApiResponse> changeUserPin(@RequestBody ChangePinRequest request) {
        try {
            String result = cardService.changeUserPin(request.getNewPin());
            boolean success = result.contains("thành công");
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Admin reset PIN user (cần verify admin trước)
     * Body: { "newPin": "resetpass123" }
     */
    @PostMapping("/reset-user-pin")
    public ResponseEntity<ApiResponse> resetUserPin(@RequestBody ChangePinRequest request) {
        try {
            String result = cardService.resetUserPin(request.getNewPin());
            boolean success = result.contains("thành công");
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }

    /**
     * Đọc dữ liệu từ thẻ (cần verify trước)
     * Response: { "success": true, "message": "...", "data": "plain text data" }
     */
    @GetMapping("/data")
    public ResponseEntity<ApiResponse> getData() {
        try {
            String data = cardService.getData();
            return ResponseEntity.ok(new ApiResponse(true, "Đọc dữ liệu thành công", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /**
     * Ghi dữ liệu vào thẻ (cần verify trước)
     * Body: { "data": "Hello World!" }
     */
    @PostMapping("/data")
    public ResponseEntity<ApiResponse> setData(@RequestBody DataRequest request) {
        try {
            String result = cardService.setData(request.getData());
            boolean success = result.contains("thành công");
            HttpStatus status = success ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
            return ResponseEntity.status(status).body(new ApiResponse(success, result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(false, "Lỗi: " + e.getMessage()));
        }
    }
}