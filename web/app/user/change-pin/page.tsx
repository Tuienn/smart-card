"use client";

import React, { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { OtpView, OtpViewRef } from "@/components/common/otp-view";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import { Button } from "@/components/ui/button";
import { KeyRound } from "lucide-react";
import { changePinAction } from "../../actions";

const ChangePinPage: React.FC = () => {
  const router = useRouter();
  const otpRef = useRef<OtpViewRef>(null);
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const handleChangePin = async (pin?: string) => {
    const newPin = pin || otpRef.current?.getValue();

    if (!newPin || newPin.length !== 6) {
      setMessage({ type: "error", text: "Vui lòng nhập đủ 6 số PIN mới" });
      return;
    }

    setMessage({ type: "info", text: "Đang đổi mã PIN..." });

    try {
      const result = await changePinAction(newPin);
      if (result.success) {
        setMessage({ type: "success", text: result.message });
        setTimeout(() => {
          router.push("/user");
        }, 2000);
      } else {
        setMessage({ type: "error", text: result.message });
        otpRef.current?.clear();
      }
    } catch {
      setMessage({ type: "error", text: "Có lỗi xảy ra." });
      otpRef.current?.clear();
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-orange-50 via-white to-amber-50 p-6">
      <div className="max-w-2xl w-full">
        <div className="text-center space-y-8">
          <div className="space-y-4">
            <div className="flex justify-center">
              <div className="bg-linear-to-br from-orange-500 to-amber-600 p-6 rounded-3xl shadow-2xl">
                <KeyRound className="size-16 text-white" />
              </div>
            </div>
            <h1 className="text-4xl font-bold bg-linear-to-r from-orange-600 to-amber-600 bg-clip-text text-transparent">
              Đổi mã PIN
            </h1>
            <p className="text-lg text-gray-600">Nhập mã PIN mới (6 chữ số)</p>
          </div>

          <div className="py-4">
            <OtpView ref={otpRef} onComplete={handleChangePin} />
          </div>

          <MessageAlert message={message} className="mt-4" />

          <div className="pt-4">
            <Button
              variant="outline"
              onClick={() => router.push("/user")}
              className="w-full max-w-md h-14 text-lg"
            >
              Quay lại
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChangePinPage;
