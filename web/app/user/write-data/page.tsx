"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Loader2, PenLine } from "lucide-react";
import { writeCardDataAction } from "../../actions";

const WriteDataPage: React.FC = () => {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [writeDataInput, setWriteDataInput] = useState("");
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const handleWriteData = async () => {
    if (!writeDataInput.trim()) {
      setMessage({ type: "error", text: "Vui lòng nhập dữ liệu" });
      return;
    }

    setIsLoading(true);
    setMessage({ type: "info", text: "Đang ghi dữ liệu..." });

    try {
      const result = await writeCardDataAction(writeDataInput);
      if (result.success) {
        setMessage({ type: "success", text: result.message });
        setWriteDataInput("");
      } else {
        setMessage({ type: "error", text: result.message });
      }
    } catch {
      setMessage({ type: "error", text: "Có lỗi xảy ra." });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-linear-to-br from-green-50 via-white to-emerald-50 p-6">
      <div className="max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-linear-to-br from-green-500 to-emerald-600 p-4 rounded-2xl shadow-xl">
              <PenLine className="size-12 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold bg-linear-to-r from-green-600 to-emerald-600 bg-clip-text text-transparent">
            Ghi dữ liệu
          </h1>
        </div>

        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Nhập dữ liệu cần ghi</CardTitle>
            <CardDescription>Dữ liệu sẽ được lưu trữ trên thẻ</CardDescription>
          </CardHeader>
          <CardContent>
            <textarea
              value={writeDataInput}
              onChange={(e) => setWriteDataInput(e.target.value)}
              placeholder="Nhập dữ liệu tại đây..."
              className="w-full min-h-32 p-4 border rounded-lg resize-none focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
            />
          </CardContent>
        </Card>

        <MessageAlert message={message} className="mt-4" />

        <div className="flex gap-4 mt-6">
          <Button
            variant="outline"
            onClick={() => router.push("/user")}
            className="flex-1 h-12"
          >
            Quay lại
          </Button>
          <Button
            onClick={handleWriteData}
            disabled={isLoading || !writeDataInput.trim()}
            className="flex-1 h-12 bg-linear-to-r from-green-600 to-emerald-600"
          >
            {isLoading ? (
              <Loader2 className="size-5 animate-spin" />
            ) : (
              "Ghi dữ liệu"
            )}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default WriteDataPage;
