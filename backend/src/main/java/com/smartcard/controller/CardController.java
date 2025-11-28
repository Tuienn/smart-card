package com.smartcard.controller;

import com.smartcard.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/card")
@CrossOrigin(origins = "*") // Cho phép Frontend gọi thoải mái
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping("/connect")
    public String connect() {
        return cardService.connectToCard();
    }

    @GetMapping("/disconnect")
    public String disconnect() {
        cardService.disconnect();
        return "Đã ngắt kết nối";
    }

    // Ví dụ API gửi PIN
    /*
    @PostMapping("/verify-pin")
    public ResponseEntity<?> verifyPin(@RequestBody PinRequest request) {
        // Gọi service xử lý
    }
    */
}