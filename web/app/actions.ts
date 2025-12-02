"use server";

import SmartCardService from "@/services/smart-card.service";
import UserService from "@/services/user.service";
import AdminService from "@/services/admin.service";

export async function connectCardAction() {
  try {
    const response = await SmartCardService.connect();
    return {
      success: true,
      message: response?.message || "Kết nối thẻ thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể kết nối với thẻ. Vui lòng thử lại.",
    };
  }
}

export async function initializeCardAction(userPin: string) {
  try {
    const response = await SmartCardService.initializeCard(userPin);
    return {
      success: true,
      message: response?.message || "Khởi tạo thẻ thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể khởi tạo thẻ. Vui lòng thử lại.",
    };
  }
}

export async function verifyPinAction(pin: string) {
  try {
    const response = await UserService.verifyPin(pin);
    return {
      success: true,
      message: response?.message || "Xác thực PIN thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Mã PIN không đúng. Vui lòng thử lại.",
    };
  }
}

export async function readCardDataAction() {
  try {
    const response = await UserService.readData();
    return {
      success: true,
      message: "Đọc dữ liệu thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể đọc dữ liệu thẻ.",
    };
  }
}

export async function writeCardDataAction(data: string) {
  try {
    const response = await UserService.writeData(data);
    return {
      success: true,
      message: response?.message || "Ghi dữ liệu thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể ghi dữ liệu vào thẻ.",
    };
  }
}

export async function changePinAction(newPin: string) {
  try {
    const response = await UserService.changePin(newPin);
    return {
      success: true,
      message: response?.message || "Đổi mã PIN thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể đổi mã PIN.",
    };
  }
}

export async function disconnectCardAction() {
  try {
    const response = await SmartCardService.disconnect();
    return {
      success: true,
      message: response?.message || "Ngắt kết nối thẻ thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể ngắt kết nối thẻ.",
    };
  }
}

export async function resetUserPinAction(newPin: string) {
  try {
    const response = await AdminService.resetUserPin(newPin);

    return {
      success: true,
      message: response?.message || "Reset mã PIN thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Không thể reset mã PIN.",
    };
  }
}

export async function verifyAdminPinAction(pin: string) {
  try {
    const response = await AdminService.verifyAdminPin(pin);
    return {
      success: true,
      message: response?.message || "Xác thực Admin PIN thành công!",
      data: response,
    };
  } catch (error: any) {
    return {
      success: false,
      message: error?.message || "Mã Admin PIN không đúng.",
    };
  }
}
