/* eslint-disable no-unused-vars */
"use client";

import * as React from "react";
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp";
import { Button } from "@/components/ui/button";
import { DeleteIcon } from "lucide-react";

export interface OtpViewRef {
  getValue: () => string;
  setValue: (value: string) => void;
  clear: () => void;
}

interface OtpViewProps {
  maxLength?: number;
  onComplete?: (value: string) => void;
  className?: string;
}

export const OtpView = React.forwardRef<OtpViewRef, OtpViewProps>(
  ({ onComplete }, ref) => {
    const [value, setValue] = React.useState("");
    const [isSubmitting, setIsSubmitting] = React.useState(false);
    const maxLength = 6;

    // Expose methods via ref
    React.useImperativeHandle(ref, () => ({
      getValue: () => value,
      setValue: (newValue: string) => {
        setIsSubmitting(false);
        setValue(newValue.slice(0, maxLength));
      },
      clear: () => {
        setIsSubmitting(false);
        setValue("");
      },
    }));

    // Handle OTP input change (from keyboard or paste)
    const handleOtpChange = (newValue: string) => {
      setValue(newValue);
      if (newValue.length === maxLength && onComplete && !isSubmitting) {
        setIsSubmitting(true);
        onComplete(newValue);
      }
    };

    const handleNumberClick = (num: number) => {
      if (value.length < maxLength) {
        const newValue = value + num.toString();
        setValue(newValue);

        if (newValue.length === maxLength && onComplete && !isSubmitting) {
          setIsSubmitting(true);
          onComplete(newValue);
        }
      }
    };

    const handleBackspace = () => {
      setValue(value.slice(0, -1));
    };

    const handleClear = () => {
      setValue("");
    };

    return (
      <div>
        <div className="flex flex-col items-center gap-6">
          {/* OTP Input Display */}
          <InputOTP
            maxLength={maxLength}
            value={value}
            onChange={handleOtpChange}
          >
            <InputOTPGroup>
              {Array.from({ length: maxLength }).map((_, index) => (
                <InputOTPSlot
                  key={index}
                  index={index}
                  className="size-14 text-lg font-semibold border-gray-300"
                />
              ))}
            </InputOTPGroup>
          </InputOTP>

          {/* Number Pad */}
          <div className="w-full max-w-xs">
            <div className="grid grid-cols-3 gap-3">
              {/* Numbers 1-9 */}
              {[1, 2, 3, 4, 5, 6, 7, 8, 9].map((num) => (
                <Button
                  key={num}
                  variant="outline"
                  size="lg"
                  onClick={() => handleNumberClick(num)}
                  disabled={value.length >= maxLength}
                  className="text-lg font-semibold h-20"
                >
                  {num}
                </Button>
              ))}

              {/* Bottom row: Clear, 0, Backspace */}
              <Button
                variant="destructive"
                size="lg"
                className="text-lg font-semibold h-20"
                onClick={handleClear}
              >
                Xóa
              </Button>
              <Button
                variant="outline"
                size="lg"
                onClick={() => handleNumberClick(0)}
                disabled={value.length >= maxLength}
                className="text-lg font-semibold h-20"
              >
                0
              </Button>
              <Button
                variant="ghost"
                size="lg"
                onClick={handleBackspace}
                disabled={value.length === 0}
                className="h-20"
              >
                <DeleteIcon className="size-6 text-red-500" />
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }
);

OtpView.displayName = "OtpView";
