import { ADMIN_PIN } from "@/constants/env.config";
import javaService from ".";

export default class SmartCardService {
  static async connect() {
    const res = await javaService("/card/connect");
    return res;
  }

  static async disconnect() {
    const res = await javaService("/card/disconnect");
    return res;
  }

  static async initializeCard(userPin: string) {
    const res = await javaService("/card/initialize", {
      method: "POST",
      body: JSON.stringify({
        userPin,
        adminPin: ADMIN_PIN,
      }),
    });
    return res;
  }
}
