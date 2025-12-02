import javaService from ".";

export default class UserService {
  static async verifyPin(pin: string) {
    const res = await javaService("/card/verify-user-pin", {
      method: "POST",
      body: JSON.stringify({
        pin,
      }),
    });
    return res;
  }

  static async readData() {
    const res = await javaService("/card/data");

    return res;
  }

  static async writeData(data: string) {
    const res = await javaService("/card/data", {
      method: "POST",
      body: JSON.stringify({
        data,
      }),
    });
    return res;
  }

  static async changePin(newPin: string) {
    const res = await javaService("/card/change-user-pin", {
      method: "POST",
      body: JSON.stringify({
        newPin,
      }),
    });
    return res;
  }
}
