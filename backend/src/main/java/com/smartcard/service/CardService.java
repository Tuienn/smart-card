package com.smartcard.service;

import com.smartcard.constant.AppletConsts;
import com.smartcard.utils.HexUtils;
import org.springframework.stereotype.Service;
import javax.smartcardio.*;
import java.util.List;

@Service
public class CardService {

    private Card card;
    private CardChannel channel;

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
}