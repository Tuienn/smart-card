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
import { ShieldAlert, KeyRound } from "lucide-react";
import { verifyAdminPinAction } from "../actions";
import { isAdminAuthVerified, setAdminAuthVerified } from "@/lib/auth";

type ViewState = "verify" | "menu";

const getInitialView = (): ViewState => {
  if (typeof window !== "undefined" && isAdminAuthVerified()) {
    return "menu";
  }
  return "verify";
};

const AdminPage: React.FC = () => {
  const router = useRouter();
  const otpRef = useRef<OtpViewRef>(null);
  const [currentView, setCurrentView] = useState<ViewState>(getInitialView);
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const clearMessage = () => setMessage({ type: null, text: "" });

  const handleVerifyAdminPin = async (pin?: string) => {
    const adminPin = pin || otpRef.current?.getValue();

    if (!adminPin || adminPin.length !== 6) {
      setMessage({ type: "error", text: "Vui lòng nhập đủ 6 số Admin PIN" });
      return;
    }

    setMessage({ type: "info", text: "Đang xác thực..." });

    try {
      const result = await verifyAdminPinAction(adminPin);
      if (result.success) {
        setMessage({ type: "success", text: result.message });
        setAdminAuthVerified();
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

  // Verify Admin PIN View
  if (currentView === "verify") {
    return (
      <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-red-50 via-white to-rose-50 p-6">
        <div className="max-w-2xl w-full">
          <div className="text-center space-y-8">
            <div className="space-y-4">
              <div className="flex justify-center">
                <div className="bg-linear-to-br from-red-500 to-rose-600 p-6 rounded-3xl shadow-2xl">
                  <ShieldAlert className="size-16 text-white" />
                </div>
              </div>
              <h1 className="text-4xl font-bold bg-linear-to-r from-red-600 to-rose-600 bg-clip-text text-transparent">
                Admin Panel
              </h1>
              <p className="text-lg text-gray-600">
                Vui lòng nhập Admin PIN để truy cập
              </p>
            </div>

            <div className="py-4">
              <OtpView ref={otpRef} onComplete={handleVerifyAdminPin} />
            </div>

            <MessageAlert message={message} className="mt-4" />

            <div className="pt-4 space-y-2 text-gray-500 text-sm">
              <p className="text-red-500 font-medium">
                ⚠️ Chức năng này chỉ dành cho quản trị viên
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Admin Menu View
  return (
    <div className="min-h-screen bg-linear-to-br from-red-50 via-white to-rose-50 p-6">
      <div className="max-w-4xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-linear-to-br from-red-500 to-rose-600 p-4 rounded-2xl shadow-xl">
              <ShieldAlert className="size-12 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold bg-linear-to-r from-red-600 to-rose-600 bg-clip-text text-transparent">
            Quản trị hệ thống
          </h1>
          <p className="text-gray-600 mt-2">Chọn chức năng quản trị</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-2xl mx-auto">
          {/* Reset User PIN Card */}
          <Card
            className="cursor-pointer hover:shadow-xl transition-all duration-300 hover:scale-105 border-2 hover:border-red-300"
            onClick={() => router.push("/admin/reset-user-pin")}
          >
            <CardHeader className="text-center">
              <div className="flex justify-center mb-2">
                <div className="bg-red-100 p-4 rounded-2xl">
                  <KeyRound className="size-10 text-red-600" />
                </div>
              </div>
              <CardTitle className="text-xl">Đặt lại mật khẩu</CardTitle>
              <CardDescription>
                Reset mã PIN người dùng về giá trị mới
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default AdminPage;
