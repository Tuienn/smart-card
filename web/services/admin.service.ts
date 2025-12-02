import javaService from ".";

export default class AdminService {
  static async resetUserPin(pin: string) {
    const res = await javaService("/card/reset-user-pin", {
      method: "POST",
      body: JSON.stringify({
        newPin: pin,
      }),
    });
    return res;
  }

  static async verifyAdminPin(pin: string) {
    const res = await javaService("/card/verify-admin-pin", {
      method: "POST",
      body: JSON.stringify({
        pin,
      }),
    });
    return res;
  }
}
