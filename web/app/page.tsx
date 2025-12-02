"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import { CreditCard, Loader2, User, ShieldUser } from "lucide-react";
import { connectCardAction } from "./actions";

const HomePage: React.FC = () => {
  const router = useRouter();
  const [isConnecting, setIsConnecting] = useState(false);
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const handleConnect = async () => {
    setIsConnecting(true);
    setMessage({ type: "info", text: "Đang kết nối với thẻ..." });

    try {
      const result = await connectCardAction();

      if (result.success) {
        setMessage({
          type: "success",
          text: result.message,
        });

        // Redirect to initialize page after 1.5 seconds
        setTimeout(() => {
          router.push("/initialize");
        }, 1500);
      } else {
        setMessage({
          type: "error",
          text: result.message,
        });
      }
    } catch {
      setMessage({
        type: "error",
        text: "Có lỗi xảy ra. Vui lòng thử lại.",
      });
    } finally {
      setIsConnecting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-linear-to-br from-blue-50 via-white to-purple-50 p-6">
      <div className="max-w-2xl w-full">
        <div className="text-center space-y-8">
          {/* Logo/Title */}
          <div className="space-y-4">
            <div className="flex justify-center">
              <div className="bg-linear-to-br from-blue-500 to-purple-600 p-6 rounded-3xl shadow-2xl">
                <CreditCard className="size-20 text-white" />
              </div>
            </div>
            <h1 className="text-5xl font-bold bg-linear-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
              JoyZone Smart Card
            </h1>
            <p className="text-xl text-gray-600">
              Kiosk tự phục vụ cho khu trò chơi gia đình
            </p>
          </div>

          {/* Connect Button */}
          <div className="py-8">
            <Button
              onClick={handleConnect}
              disabled={isConnecting}
              size="lg"
              className="w-full max-w-md h-24 text-2xl font-bold shadow-2xl hover:shadow-3xl transition-all duration-300 bg-linear-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700"
            >
              {isConnecting ? (
                <>
                  <Loader2 className="size-8 animate-spin" />
                  Đang kết nối...
                </>
              ) : (
                <>
                  <CreditCard className="size-8" />
                  Kết nối thẻ
                </>
              )}
            </Button>
          </div>

          {/* Message Display */}
          <MessageAlert message={message} />

          {/* Instructions */}
          <div className="pt-8 space-y-3 text-gray-500">
            <p className="text-sm">📍 Vui lòng đặt thẻ lên đầu đọc</p>
            <p className="text-sm">
              🔒 Giữ thẻ ổn định trong quá trình kết nối
            </p>
          </div>

          {/* Auth Mode Buttons */}
          <div className="pt-6 border-t border-gray-200">
            <p className="text-sm text-gray-500 mb-4">
              Hoặc chọn phương thức xác thực
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Button
                onClick={() => router.push("/user")}
                variant="outline"
                className="flex items-center gap-2 px-6 py-3 border-2 border-green-300 text-green-700 hover:bg-green-50 hover:border-green-400 rounded-xl transition-all duration-300"
              >
                <User className="size-5" />
                Xác minh người dùng
              </Button>

              <Button
                onClick={() => router.push("/admin")}
                variant="outline"
                className="flex items-center gap-2 px-6 py-3 border-2 border-red-300 text-red-700 hover:bg-red-50 hover:border-red-400 rounded-xl transition-all duration-300"
              >
                <ShieldUser className="size-5" />
                Xác minh Admin
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
