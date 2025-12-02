import { Alert, AlertTitle } from "@/components/ui/alert";
import { Loader2, CheckCircle2, XCircle } from "lucide-react";

export interface MessageState {
  type: "success" | "error" | "info" | null;
  text: string;
}

interface MessageAlertProps {
  message: MessageState;
  className?: string;
}

export function MessageAlert({ message, className = "" }: MessageAlertProps) {
  if (!message.type) return null;

  return (
    <div
      className={`max-w-md mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500 ${className}`}
    >
      <Alert
        variant={message.type === "error" ? "destructive" : "default"}
        className={`${
          message.type === "success"
            ? "bg-green-50 border-green-300 text-green-800"
            : message.type === "info"
            ? "bg-blue-50 border-blue-300 text-blue-800"
            : ""
        }`}
      >
        {message.type === "success" && (
          <CheckCircle2 className="size-5 text-green-600" />
        )}
        {message.type === "error" && (
          <XCircle className="size-5 text-red-600" />
        )}
        {message.type === "info" && (
          <Loader2 className="size-5 text-blue-600 animate-spin" />
        )}
        <AlertTitle>{message.text}</AlertTitle>
      </Alert>
    </div>
  );
}
