package com.smartcard.service;

import com.smartcard.constant.AppletConsts;
import com.smartcard.utils.HexUtils;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Service;
import javax.smartcardio.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.List;
import java.util.Arrays;

@Service
public class CardService {

    private Card card;
    private CardChannel channel;
    private final SecureRandom secureRandom;

    public CardService() {
        this.secureRandom = new SecureRandom();
    }

    public String connectToCard() {
        try {
            // 1. Tìm thiết bị
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            if (terminals.isEmpty()) return "Không tìm thấy đầu đọc thẻ!";

            CardTerminal terminal = terminals.get(0); // Lấy cái đầu tiên (Simulator)

            // 2. Kết nối
            card = terminal.connect("*");
            channel = card.getBasicChannel();

            // 3. Gửi lệnh SELECT Applet
            byte[] aid = AppletConsts.AID;
            ResponseAPDU response = sendApdu(0x00, 0xA4, 0x04, 0x00, aid);

            if (response.getSW() == 0x9000) {
                return "Kết nối thành công tới Applet!";
            } else {
                return "Kết nối thất bại. Mã lỗi: " + Integer.toHexString(response.getSW());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }

    // Hàm disconnect
    public void disconnect() {
        try {
            if (card != null) card.disconnect(false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // Hàm gửi APDU helper (để các hàm khác gọi lại)
    public ResponseAPDU sendApdu(int cla, int ins, int p1, int p2, byte[] data) throws CardException {
        if (channel == null) throw new CardException("Chưa kết nối thẻ");

        CommandAPDU command;
        if (data != null) {
            command = new CommandAPDU(cla, ins, p1, p2, data);
        } else {
            command = new CommandAPDU(cla, ins, p1, p2);
        }

        System.out.println(">> Gửi: " + HexUtils.bytesToHex(command.getBytes()));
        ResponseAPDU response = channel.transmit(command);
        System.out.println("<< Nhận: " + HexUtils.bytesToHex(response.getBytes()));

        return response;
    }

    /**
     * Tính toán KEK từ PIN sử dụng Argon2
     * @param pin PIN dạng plain text
     * @param salt Salt 16 bytes
     * @return KEK 32 bytes
     */
    private byte[] computeKEK(String pin, byte[] salt) {
        // Argon2 parameters theo khuyến nghị trong APPLET_DOCUMENTATION
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(3)
            .withMemoryAsKB(65536)  // 64MB
            .withParallelism(4)
            .withSalt(salt);
        
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());
        
        byte[] hash = new byte[32]; // KEK 32 bytes
        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
        
        try {
            generator.generateBytes(pinBytes, hash);
            return hash;
        } finally {
            // Clear sensitive data
            Arrays.fill(pinBytes, (byte) 0);
        }
    }

    /**
     * INITIALIZE (INS=0x60): Khởi tạo thẻ lần đầu
     * Input: userPin (plaintext), adminPin (plaintext)
     * Host tạo SALT và tính KEK, sau đó gửi xuống thẻ
     */
    public String initialize(String userPin, String adminPin) {
        try {
            // 1. Tạo SALT ngẫu nhiên
            byte[] saltUser = new byte[16];
            byte[] saltAdmin = new byte[16];
            secureRandom.nextBytes(saltUser);
            secureRandom.nextBytes(saltAdmin);

            // 2. Tính KEK từ PIN
            byte[] kekUser = computeKEK(userPin, saltUser);
            byte[] kekAdmin = computeKEK(adminPin, saltAdmin);

            // 3. Tạo payload: KEK_user (32) + Salt_user (16) + KEK_admin (32) + Salt_admin (16) = 96 bytes
            byte[] payload = new byte[96];
            System.arraycopy(kekUser, 0, payload, 0, 32);
            System.arraycopy(saltUser, 0, payload, 32, 16);
            System.arraycopy(kekAdmin, 0, payload, 48, 32);
            System.arraycopy(saltAdmin, 0, payload, 80, 16);

            // 4. Gửi lệnh INITIALIZE
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_INITIALIZE,
                0x00,
                0x00,
                payload
            );

            if (response.getSW() == AppletConsts.SW_SUCCESS) {
                return "Khởi tạo thẻ thành công!";
            } else {
                return "Khởi tạo thất bại. Mã lỗi: " + String.format("0x%04X", response.getSW());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * GET_SALT (INS=0x10): Lấy SALT từ thẻ
     * @return Map chứa userSalt và adminSalt dạng hex string
     */
    public java.util.Map<String, String> getSalt() {
        try {
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_GET_SALT,
                0x00,
                0x00,
                null
            );

            if (response.getSW() == AppletConsts.SW_SUCCESS) {
                byte[] data = response.getData();
                if (data.length >= 32) {
                    byte[] saltUser = new byte[16];
                    byte[] saltAdmin = new byte[16];
                    System.arraycopy(data, 0, saltUser, 0, 16);
                    System.arraycopy(data, 16, saltAdmin, 0, 16);

                    java.util.Map<String, String> result = new java.util.HashMap<>();
                    result.put("userSalt", HexUtils.bytesToHex(saltUser));
                    result.put("adminSalt", HexUtils.bytesToHex(saltAdmin));
                    return result;
                }
            }
            throw new RuntimeException("Không thể lấy SALT. Mã lỗi: " + String.format("0x%04X", response.getSW()));

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy SALT: " + e.getMessage(), e);
        }
    }

    /**
     * VERIFY_USER_PIN (INS=0x20): Xác thực user PIN
     * @param pin PIN dạng plain text
     * @return true nếu xác thực thành công
     */
    public String verifyUserPin(String pin) {
        try {
            // 1. Lấy SALT từ thẻ
            java.util.Map<String, String> salts = getSalt();
            byte[] saltUser = HexUtils.hexToBytes(salts.get("userSalt"));

            // 2. Tính KEK từ PIN
            byte[] kekUser = computeKEK(pin, saltUser);

            // 3. Gửi KEK xuống thẻ để verify
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_VERIFY_USER_PIN,
                0x00,
                0x00,
                kekUser
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                return "Xác thực user thành công!";
            } else if (sw == AppletConsts.SW_AUTHENTICATION_METHOD_BLOCKED) {
                return "Tài khoản user đã bị khóa sau 5 lần nhập sai!";
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                return "PIN user không đúng!";
            } else {
                return "Lỗi xác thực. Mã lỗi: " + String.format("0x%04X", sw);
            }

        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * VERIFY_ADMIN_PIN (INS=0x21): Xác thực admin PIN
     * @param pin PIN dạng plain text
     * @return thông báo kết quả
     */
    public String verifyAdminPin(String pin) {
        try {
            // 1. Lấy SALT từ thẻ
            java.util.Map<String, String> salts = getSalt();
            byte[] saltAdmin = HexUtils.hexToBytes(salts.get("adminSalt"));

            // 2. Tính KEK từ PIN
            byte[] kekAdmin = computeKEK(pin, saltAdmin);

            // 3. Gửi KEK xuống thẻ để verify
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_VERIFY_ADMIN_PIN,
                0x00,
                0x00,
                kekAdmin
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                return "Xác thực admin thành công!";
            } else if (sw == AppletConsts.SW_AUTHENTICATION_METHOD_BLOCKED) {
                return "Tài khoản admin đã bị khóa sau 5 lần nhập sai!";
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                return "PIN admin không đúng!";
            } else {
                return "Lỗi xác thực. Mã lỗi: " + String.format("0x%04X", sw);
            }

        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * CHANGE_USER_PIN (INS=0x30): Đổi PIN user (cần xác thực user trước)
     * @param newPin PIN mới dạng plain text
     * @return thông báo kết quả
     */
    public String changeUserPin(String newPin) {
        try {
            // 1. Tạo SALT mới
            byte[] newSalt = new byte[16];
            secureRandom.nextBytes(newSalt);

            // 2. Tính KEK mới
            byte[] newKek = computeKEK(newPin, newSalt);

            // 3. Tạo payload: KEK_new (32) + Salt_new (16) = 48 bytes
            byte[] payload = new byte[48];
            System.arraycopy(newKek, 0, payload, 0, 32);
            System.arraycopy(newSalt, 0, payload, 32, 16);

            // 4. Gửi lệnh
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_CHANGE_USER_PIN,
                0x00,
                0x00,
                payload
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                return "Đổi PIN user thành công!";
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                return "Chưa xác thực user. Vui lòng verify PIN trước!";
            } else {
                return "Đổi PIN thất bại. Mã lỗi: " + String.format("0x%04X", sw);
            }

        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * RESET_USER_PIN (INS=0x31): Admin reset PIN user
     * @param newPin PIN mới cho user
     * @return thông báo kết quả
     */
    public String resetUserPin(String newPin) {
        try {
            // 1. Tạo SALT mới
            byte[] newSalt = new byte[16];
            secureRandom.nextBytes(newSalt);

            // 2. Tính KEK mới
            byte[] newKek = computeKEK(newPin, newSalt);

            // 3. Tạo payload: KEK_new (32) + Salt_new (16) = 48 bytes
            byte[] payload = new byte[48];
            System.arraycopy(newKek, 0, payload, 0, 32);
            System.arraycopy(newSalt, 0, payload, 32, 16);

            // 4. Gửi lệnh
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_RESET_USER_PIN,
                0x00,
                0x00,
                payload
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                return "Reset PIN user thành công! Counter đã được đặt lại.";
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                return "Chưa xác thực admin. Vui lòng verify admin PIN trước!";
            } else {
                return "Reset PIN thất bại. Mã lỗi: " + String.format("0x%04X", sw);
            }

        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    /**
     * GET_DATA (INS=0x40): Đọc dữ liệu đã mã hóa (cần xác thực trước)
     * @return dữ liệu plain text
     */
    public String getData() {
        try {
            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_GET_DATA,
                0x00,
                0x00,
                null
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                byte[] data = response.getData();
                // Loại bỏ padding (0x00 ở cuối)
                int actualLength = data.length;
                for (int i = data.length - 1; i >= 0; i--) {
                    if (data[i] != 0) {
                        actualLength = i + 1;
                        break;
                    }
                }
                byte[] actualData = new byte[actualLength];
                System.arraycopy(data, 0, actualData, 0, actualLength);
                
                return new String(actualData, StandardCharsets.UTF_8);
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                throw new RuntimeException("Chưa xác thực. Vui lòng verify PIN trước!");
            } else if (sw == AppletConsts.SW_FILE_NOT_FOUND) {
                throw new RuntimeException("Chưa có dữ liệu được lưu trên thẻ!");
            } else {
                throw new RuntimeException("Đọc dữ liệu thất bại. Mã lỗi: " + String.format("0x%04X", sw));
            }

        } catch (Exception e) {
            throw new RuntimeException("Lỗi: " + e.getMessage(), e);
        }
    }

    /**
     * SET_DATA (INS=0x50): Ghi dữ liệu và mã hóa (cần xác thực trước)
     * @param data dữ liệu plain text
     * @return thông báo kết quả
     */
    public String setData(String data) {
        try {
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            
            if (dataBytes.length > 512) {
                return "Dữ liệu quá lớn! Tối đa 512 bytes.";
            }

            // Padding đến bội số của 16 bytes (AES block size)
            int paddedLength = ((dataBytes.length + 15) / 16) * 16;
            byte[] paddedData = new byte[paddedLength];
            System.arraycopy(dataBytes, 0, paddedData, 0, dataBytes.length);
            // Phần còn lại tự động là 0x00

            ResponseAPDU response = sendApdu(
                AppletConsts.CLA,
                AppletConsts.INS_SET_DATA,
                0x00,
                0x00,
                paddedData
            );

            int sw = response.getSW();
            if (sw == AppletConsts.SW_SUCCESS) {
                return "Ghi dữ liệu thành công!";
            } else if (sw == AppletConsts.SW_SECURITY_STATUS_NOT_SATISFIED) {
                return "Chưa xác thực. Vui lòng verify PIN trước!";
            } else if (sw == AppletConsts.SW_WRONG_LENGTH) {
                return "Dữ liệu quá lớn!";
            } else {
                return "Ghi dữ liệu thất bại. Mã lỗi: " + String.format("0x%04X", sw);
            }

        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }
}