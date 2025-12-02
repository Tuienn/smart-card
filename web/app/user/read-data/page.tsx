"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { MessageAlert, MessageState } from "@/components/common/message-alert";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Loader2, FileText } from "lucide-react";
import { readCardDataAction } from "../../actions";

const ReadDataPage: React.FC = () => {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [cardData, setCardData] = useState<string>("");
  const [message, setMessage] = useState<MessageState>({
    type: null,
    text: "",
  });

  const handleReadData = async () => {
    setIsLoading(true);
    setMessage({ type: "info", text: "Đang đọc dữ liệu..." });

    try {
      const result = await readCardDataAction();
      if (result.success) {
        setCardData(result.data?.data || "Không có dữ liệu");
        setMessage({ type: "success", text: result.message });
      } else {
        setMessage({ type: "error", text: result.message });
      }
    } catch {
      setMessage({ type: "error", text: "Có lỗi xảy ra." });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    handleReadData();
  }, []);

  return (
    <div className="min-h-screen bg-linear-to-br from-blue-50 via-white to-indigo-50 p-6">
      <div className="max-w-2xl mx-auto">
        <div className="text-center mb-8">
          <div className="flex justify-center mb-4">
            <div className="bg-linear-to-br from-blue-500 to-indigo-600 p-4 rounded-2xl shadow-xl">
              <FileText className="size-12 text-white" />
            </div>
          </div>
          <h1 className="text-3xl font-bold bg-linear-to-r from-blue-600 to-indigo-600 bg-clip-text text-transparent">
            Dữ liệu thẻ
          </h1>
        </div>

        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Thông tin trên thẻ</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="ont-mono text-sm whitespace-pre-wrap break-all">
              {isLoading ? (
                <div className="flex items-center justify-center h-full">
                  <Loader2 className="size-8 animate-spin text-blue-600" />
                </div>
              ) : (
                cardData || "Không có dữ liệu"
              )}
            </div>
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
            onClick={handleReadData}
            disabled={isLoading}
            className="flex-1 h-12 bg-linear-to-r from-blue-600 to-indigo-600"
          >
            {isLoading ? (
              <Loader2 className="size-5 animate-spin" />
            ) : (
              "Đọc lại"
            )}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ReadDataPage;
