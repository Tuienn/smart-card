"use client";

import React, { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { OtpView, OtpViewRef } from "@/components/common/otp-view";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import { CreditCard } from "lucide-react";
import { initializeCardAction } from "../actions";

const InitializePage: React.FC = () => {
  const router = useRouter();
  const otpRef = useRef<OtpViewRef>(null);
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const handleComplete = async (pin: string) => {
    // Auto submit when PIN is complete
    await handleInitialize(pin);
  };

  const handleInitialize = async (pin?: string) => {
    const userPin = pin || otpRef.current?.getValue();

    if (!userPin || userPin.length !== 6) {
      setMessage({
        type: "error",
        text: "Vui lòng nhập đủ 6 số PIN",
      });
      return;
    }

    setMessage({ type: "info", text: "Đang khởi tạo thẻ..." });

    try {
      const result = await initializeCardAction(userPin);

      if (result.success) {
        setMessage({
          type: "success",
          text: result.message,
        });

        // Redirect to user page after 2 seconds
        setTimeout(() => {
          router.push("/user");
        }, 2000);
      } else {
        setMessage({
          type: "error",
          text: result.message,
        });
        otpRef.current?.clear();
      }
    } catch {
      setMessage({
        type: "error",
        text: "Có lỗi xảy ra. Vui lòng thử lại.",
      });
      otpRef.current?.clear();
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-purple-50 via-white to-blue-50 p-6">
      <div className="max-w-2xl w-full">
        <div className="text-center space-y-8">
          {/* Title */}
          <div className="space-y-4">
            <div className="flex justify-center">
              <div className="bg-linear-to-br from-purple-500 to-blue-600 p-6 rounded-3xl shadow-2xl">
                <CreditCard className="size-16 text-white" />
              </div>
            </div>
            <h1 className="text-4xl font-bold bg-linear-to-r from-purple-600 to-blue-600 bg-clip-text text-transparent">
              Khởi tạo thẻ
            </h1>
            <p className="text-lg text-gray-600">
              Vui lòng nhập mã PIN (6 chữ số) để khởi tạo thẻ
            </p>
          </div>

          {/* OTP Input */}
          <div className="py-4">
            <OtpView ref={otpRef} onComplete={handleComplete} />
          </div>

          {/* Message Display */}
          <MessageAlert message={message} />

          {/* Instructions */}
          <div className="pt-4 space-y-2 text-gray-500 text-sm">
            <p>🔒 Bạn có thể thay đổi PIN sau khi khởi tạo</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default InitializePage;
