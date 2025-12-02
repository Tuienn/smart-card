"use client";

import React, { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { OtpView, OtpViewRef } from "@/components/common/otp-view";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  CreditCard,
  FileText,
  PenLine,
  KeyRound,
  ShieldCheck,
} from "lucide-react";
import { verifyPinAction } from "../actions";
import { isAuthVerified, setAuthVerified } from "@/lib/auth";

type ViewState = "verify" | "menu";

// Get initial view based on auth state
const getInitialView = (): ViewState => {
  if (typeof window !== "undefined" && isAuthVerified()) {
    return "menu";
  }
  return "verify";
};

const UserPage: React.FC = () => {
  const router = useRouter();
  const otpRef = useRef<OtpViewRef>(null);
  const [currentView, setCurrentView] = useState<ViewState>(getInitialView);
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const clearMessage = () => setMessage({ type: null, text: "" });

  // Verify PIN
  const handleVerifyPin = async (pin?: string) => {
    const userPin = pin || otpRef.current?.getValue();

    if (!userPin || userPin.length !== 6) {
      setMessage({ type: "error", text: "Vui lòng nhập đủ 6 số PIN" });
      return;
    }

    setMessage({ type: "info", text: "Đang xác thực..." });

    try {
      const result = await verifyPinAction(userPin);
      if (result.success) {
        setMessage({ type: "success", text: result.message });
        setAuthVerified(); // Save to session storage
        setTimeout(() => {
          setCurrentView("menu");
          clearMessage();
        }, 1000);
      } else {
        setMessage({ type: "error", text: result.message });
        otpRef.current?.clear();
      }
    } catch {
      setMessage({ type: "error", text: "Có lỗi xảy ra. Vui lòng thử lại." });
    }
  };

  // Verify PIN View
  if (currentView === "verify") {
    return (
      <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-green-50 via-white to-teal-50 p-6">
        <div className="max-w-2xl w-full">
          <div className="text-center space-y-8">
            <div className="space-y-4">
              <div className="flex justify-center">
                <div className="bg-linear-to-br from-green-500 to-teal-600 p-6 rounded-3xl shadow-2xl">
                  <ShieldCheck className="size-16 text-white" />
                </div>
              </div>
              <h1 className="text-4xl font-bold bg-linear-to-r from-green-600 to-teal-600 bg-clip-text text-transparent">
                Xác thực PIN
              </h1>
              <p className="text-lg text-gray-600">
                Vui lòng nhập mã PIN để truy cập
              </p>
            </div>

            <div className="py-4">
              <OtpView ref={otpRef} onComplete={handleVerifyPin} />
            </div>

            <MessageAlert message={message} className="mt-4" />
          </div>
        </div>
      </div>
    );
  }

  // Function Menu View
  return (
    <div className="min-h-screen bg-linear-to-br from-slate-50 via-white to-blue-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-linear-to-br from-blue-500 to-indigo-600 p-4 rounded-2xl shadow-xl">
              <CreditCard className="size-12 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold bg-linear-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            Quản lý thẻ
          </h1>
          <p className="text-gray-600 mt-2">Chọn chức năng bạn muốn sử dụng</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Read Data Card */}
          <Card
            className="cursor-pointer hover:shadow-xl transition-all duration-300 hover:scale-105 border-2 hover:border-blue-300"
            onClick={() => router.push("/user/read-data")}
          >
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <div className="bg-blue-100 p-4 rounded-2xl">
                  <FileText className="size-10 text-blue-600" />
                </div>
              </div>
              <CardTitle className="text-xl">Đọc dữ liệu thẻ</CardTitle>
              <CardDescription>Xem thông tin được lưu trên thẻ</CardDescription>
            </CardHeader>
          </Card>

          {/* Write Data Card */}
          <Card
            className="cursor-pointer hover:shadow-xl transition-all duration-300 hover:scale-105 border-2 hover:border-green-300"
            onClick={() => router.push("/user/write-data")}
          >
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <div className="bg-green-100 p-4 rounded-2xl">
                  <PenLine className="size-10 text-green-600" />
                </div>
              </div>
              <CardTitle className="text-xl">Ghi dữ liệu thẻ</CardTitle>
              <CardDescription>Lưu thông tin mới vào thẻ</CardDescription>
            </CardHeader>
          </Card>

          {/* Change PIN Card */}
          <Card
            className="cursor-pointer hover:shadow-xl transition-all duration-300 hover:scale-105 border-2 hover:border-orange-300"
            onClick={() => router.push("/user/change-pin")}
          >
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <div className="bg-orange-100 p-4 rounded-2xl">
                  <KeyRound className="size-10 text-orange-600" />
                </div>
              </div>
              <CardTitle className="text-xl">Thay đổi mã PIN</CardTitle>
              <CardDescription>Cập nhật mã PIN bảo mật mới</CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default UserPage;
