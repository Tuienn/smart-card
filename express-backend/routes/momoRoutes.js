const express = require("express");
const crypto = require("crypto");
const axios = require("axios");
const Transaction = require("../models/Transaction");
const Card = require("../models/Card");
const router = express.Router();

// ===== CONFIG MOMO =====
const MOMO_CONFIG = {
  partnerCode: process.env.MOMO_PARTNER_CODE,
  accessKey: process.env.MOMO_ACCESS_KEY,
  secretKey: process.env.MOMO_SECRET_KEY,
  endpoint: process.env.MOMO_ENDPOINT,
};

// Ngrok URL for IPN callback
const NGROK_URL = process.env.MOMO_REDIRECT_URL;

// In-memory storage cho demo (production nên dùng database)
const paymentStorage = new Map();

// ===== HELPER FUNCTIONS =====

/**
 * Create HMAC SHA256 signature
 */
function hmacSHA256(data, key) {
  return crypto.createHmac("sha256", key).update(data, "utf-8").digest("hex");
}

// ===== API ENDPOINTS =====

/**
 * POST /api/momo/pay
 * Thanh toán bằng app MoMo (redirect)
 */
router.post("/pay", async (req, res) => {
  try {
    const { amount, orderInfo, redirectUrl, ipnUrl } = req.body;

    const requestId = MOMO_CONFIG.partnerCode + Date.now();
    const orderId = requestId;
    const extraData = "";
    const requestType = "captureWallet";

    const rawSignature = `accessKey=${MOMO_CONFIG.accessKey}&amount=${amount}&extraData=${extraData}&ipnUrl=${ipnUrl}&orderId=${orderId}&orderInfo=${orderInfo}&partnerCode=${MOMO_CONFIG.partnerCode}&redirectUrl=${redirectUrl}&requestId=${requestId}&requestType=${requestType}`;

    const signature = hmacSHA256(rawSignature, MOMO_CONFIG.secretKey);

    const requestBody = {
      partnerCode: MOMO_CONFIG.partnerCode,
      accessKey: MOMO_CONFIG.accessKey,
      requestId,
      amount,
      orderId,
      orderInfo,
      redirectUrl,
      ipnUrl,
      extraData,
      requestType,
      signature,
      lang: "vi",
    };

    const momoRes = await axios.post(MOMO_CONFIG.endpoint, requestBody, {
      headers: { "Content-Type": "application/json" },
    });

    const { payUrl, resultCode, message } = momoRes.data;
    res.json({ payUrl, resultCode, message });
  } catch (error) {
    console.error("Error in /pay:", error.message);
    res.json({ message: error.message });
  }
});

/**
 * POST /api/momo/qr
 * Tạo QR code MoMo - Gọi API MoMo thật
 */
router.post("/qr", async (req, res) => {
  try {
    const { amount, orderInfo, description, paymentMethod } = req.body;

    console.log("=== Creating MOMO QR Payment ===");
    console.log("Amount:", amount);
    console.log("Description:", description);

    // Tạo orderId và requestId unique
    const orderId = `QR_${Date.now()}`;
    const requestId = `REQ_${Date.now()}`;
    const amountNum = parseInt(amount) || 0;

    // Sử dụng description từ user làm orderInfo (nội dung chuyển khoản)
    const finalOrderInfo = description?.trim() || orderInfo;

    const redirectUrl = "momoapp://callback";
    const ipnUrl = `${NGROK_URL}/api/momo/ipn`;

    // captureWallet = Thanh toán qua ví MoMo (QR nhảy thẳng vào app)
    const requestType = "captureWallet";
    const extraData = "";

    console.log("OrderInfo (nội dung CK):", finalOrderInfo);
    console.log("PaymentMethod:", paymentMethod);
    console.log("Using requestType:", requestType);

    // Tạo raw signature giống Java/Kotlin
    const rawSignature = `accessKey=${MOMO_CONFIG.accessKey}&amount=${amountNum}&extraData=${extraData}&ipnUrl=${ipnUrl}&orderId=${orderId}&orderInfo=${finalOrderInfo}&partnerCode=${MOMO_CONFIG.partnerCode}&redirectUrl=${redirectUrl}&requestId=${requestId}&requestType=${requestType}`;

    console.log("Raw signature:", rawSignature);

    const signature = hmacSHA256(rawSignature, MOMO_CONFIG.secretKey);
    console.log("Signature:", signature);

    const requestBody = {
      partnerCode: MOMO_CONFIG.partnerCode,
      accessKey: MOMO_CONFIG.accessKey,
      requestId,
      amount: amountNum,
      orderId,
      orderInfo: finalOrderInfo,
      redirectUrl,
      ipnUrl,
      requestType,
      extraData,
      lang: "vi",
      signature,
    };

    console.log("Request body:", JSON.stringify(requestBody));

    // Lưu vào storage trước khi gọi MoMo
    paymentStorage.set(orderId, {
      orderId,
      status: "pending",
      amount: amount,
      description: finalOrderInfo,
      qrData: "",
      transId: null,
      completedAt: null,
      createdAt: Date.now(),
    });

    // ===== MOCK MODE for Development =====
    if (process.env.MOMO_MOCK_MODE === 'true') {
      console.log("🔧 MOCK MODE: Bypassing real MoMo API");
      
      // Tạo mock QR code URL (sẽ không hoạt động thật, chỉ để demo UI)
      const mockPayUrl = `https://test-payment.momo.vn/pay?t=${orderId}`;
      const qrCodeImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${encodeURIComponent(mockPayUrl)}`;
      
      // Cập nhật storage
      const payment = paymentStorage.get(orderId);
      if (payment) {
        payment.qrData = mockPayUrl;
        paymentStorage.set(orderId, payment);
      }
      
      console.log("✓ Mock QR created successfully");
      
      return res.json({
        qrCodeUrl: qrCodeImageUrl,
        orderId,
        qrData: mockPayUrl,
        resultCode: 0,
        message: `[MOCK] QR được tạo với số tiền ${amountNum} VND. Sử dụng endpoint /api/momo/confirm/${orderId} để giả lập thanh toán thành công.`,
      });
    }

    // ===== REAL MOMO API =====
    // Gọi API MOMO
    const momoRes = await axios.post(MOMO_CONFIG.endpoint, requestBody, {
      headers: { "Content-Type": "application/json" },
    });

    console.log("MOMO response:", JSON.stringify(momoRes.data));

    const { resultCode, message, payUrl, deeplink } = momoRes.data;

    if (resultCode !== 0 || !payUrl) {
      return res.status(400).json({
        resultCode,
        message: message || "Lỗi tạo QR",
      });
    }

    // Tạo QR code URL từ payUrl
    const qrCodeImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=${encodeURIComponent(
      payUrl
    )}`;

    // Cập nhật storage với response từ MoMo
    const payment = paymentStorage.get(orderId);
    if (payment) {
      payment.qrData = payUrl;
      paymentStorage.set(orderId, payment);
    }

    // Trả về response
    res.json({
      qrCodeUrl: qrCodeImageUrl,
      orderId,
      qrData: payUrl,
      resultCode,
      message: `QR được tạo thành công với số tiền ${amountNum} VND và nội dung: ${finalOrderInfo}. Quét QR hoặc mở link để thanh toán.`,
    });
  } catch (error) {
    console.error("Error in /qr:", error.message);
    if (error.response) {
      console.error("MOMO API Error:", error.response.data);
    }
    res.status(500).json({
      resultCode: 99,
      message: `Lỗi kết nối MoMo: ${error.message}`,
    });
  }
});

/**
 * GET /api/momo/status/:orderId
 * Kiểm tra trạng thái thanh toán
 */
router.get("/status/:orderId", (req, res) => {
  try {
    const { orderId } = req.params;

    if (!orderId) {
      return res.status(400).json({
        orderId: "",
        status: "error",
        message: "Thiếu orderId",
      });
    }

    const payment = paymentStorage.get(orderId);

    if (!payment) {
      return res.status(404).json({
        orderId,
        status: "not_found",
        message: "Không tìm thấy giao dịch",
      });
    }

    const statusMessages = {
      success: "Thanh toán thành công",
      pending: "Đang chờ thanh toán",
      failed: "Thanh toán thất bại",
    };

    res.json({
      orderId: payment.orderId,
      status: payment.status,
      amount: payment.amount,
      description: payment.description,
      transId: payment.transId,
      completedAt: payment.completedAt,
      message: statusMessages[payment.status] || null,
    });
  } catch (error) {
    console.error("Error checking status:", error.message);
    res.status(500).json({
      orderId: "",
      status: "error",
      message: `Lỗi: ${error.message}`,
    });
  }
});

/**
 * POST /api/momo/ipn
 * IPN callback endpoint - MoMo sẽ gọi khi thanh toán thành công
 */
router.post("/ipn", async (req, res) => {
  try {
    const ipnData = req.body;
    console.log("=== MoMo IPN received ===");
    console.log(JSON.stringify(ipnData));

    const { orderId, resultCode, transId, amount } = ipnData;

    console.log(
      `OrderId: ${orderId}, ResultCode: ${resultCode}, TransId: ${transId}, Amount: ${amount}`
    );

    if (orderId) {
      const payment = paymentStorage.get(orderId);

      if (payment) {
        if (resultCode === 0) {
          payment.status = "success";
          payment.transId = transId;
          payment.completedAt = Date.now().toString();
          console.log(`✓ Payment ${orderId} marked as SUCCESS`);

          // Extract userId từ description
          const description = payment.description;
          console.log("Description from payment:", description);

          let userId = null;
          if (description.startsWith("USER")) {
            userId = parseInt(description.substring(4));
          } else {
            userId = parseInt(description);
          }

          console.log("Extracted userId:", userId);

          if (userId && !isNaN(userId) && amount) {
            try {
              // TODO: Implement UserRepository for database update
              // const userRepository = new UserRepository();
              // const currentUser = await userRepository.getUserByAccountId(userId);
              // if (currentUser) {
              //     const newBalance = currentUser.balance + parseInt(amount);
              //     await userRepository.updateBalanceByUserId(currentUser.Id, newBalance);
              // }
              console.log(
                `✓ Would update balance for userId ${userId}: +${amount} VND`
              );

              // Extract cardId from description (format: TOPUP<timestamp> or cardId)
              // For now, we'll try to find the card by checking recent payments
              // In production, you should pass cardId in the description
              const cardId = description.startsWith("TOPUP") 
                ? description.replace("TOPUP", "").substring(0, 32) // Extract potential cardId
                : description.toUpperCase();

              // Try to find card and create transaction
              const card = await Card.findById(cardId).catch(() => null);
              
              if (card) {
                const newTransaction = new Transaction({
                  card_id: cardId,
                  user_age: card.user_age,
                  payment: parseInt(amount),
                  time_stamp: new Date(),
                  game_id: null,
                  combo_id: null
                });

                await newTransaction.save();
                console.log(`✓ Transaction created for card ${cardId}: ${amount} VND`);
              } else {
                console.log(`✗ Card not found: ${cardId}, transaction not saved`);
              }

            } catch (e) {
              console.error("✗ Error saving transaction:", e.message);
            }
          } else {
            console.log(`✗ Invalid userId (${userId}) or amount (${amount})`);
          }
        } else {
          payment.status = "failed";
          console.log(
            `✗ Payment ${orderId} marked as FAILED (resultCode: ${resultCode})`
          );
        }
        paymentStorage.set(orderId, payment);
      } else {
        console.log(`✗ Payment not found in storage: ${orderId}`);
      }
    } else {
      console.log("✗ OrderId is null in IPN data");
    }

    res.json({ message: "OK", resultCode: 0 });
  } catch (error) {
    console.error("✗ Error in IPN:", error.message);
    res.json({ message: "Error", resultCode: 99 });
  }
});

/**
 * POST /api/momo/confirm/:orderId
 * Xác nhận thanh toán thủ công (cho demo/testing)
 */
router.post("/confirm/:orderId", (req, res) => {
  try {
    const { orderId } = req.params;

    if (!orderId) {
      return res.status(400).json({ message: "Thiếu orderId" });
    }

    const payment = paymentStorage.get(orderId);

    if (!payment) {
      return res.status(404).json({ message: "Không tìm thấy giao dịch" });
    }

    payment.status = "success";
    payment.transId = `MANUAL_${Date.now()}`;
    payment.completedAt = Date.now().toString();
    paymentStorage.set(orderId, payment);

    res.json({
      message: "Đã xác nhận thanh toán thành công",
      orderId,
      status: "success",
    });
  } catch (error) {
    res.status(500).json({ message: error.message || "Lỗi không xác định" });
  }
});

module.exports = router;
