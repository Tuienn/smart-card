"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Power, Loader2 } from "lucide-react";
import { disconnectCardAction } from "../actions";
import { clearAuth } from "@/lib/auth";

export default function UserLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();
  const [isDisconnecting, setIsDisconnecting] = useState(false);

  const handleDisconnect = async () => {
    setIsDisconnecting(true);

    try {
      await disconnectCardAction();
      clearAuth();
      router.replace("/");
    } catch {
      // Still redirect even if disconnect fails
      clearAuth();
      router.replace("/");
    } finally {
      setIsDisconnecting(false);
    }
  };

  return (
    <div className="relative min-h-screen">
      {/* Disconnect Button - Fixed position */}
      <div className="fixed top-4 right-4 z-50">
        <Button
          variant="destructive"
          size="sm"
          onClick={handleDisconnect}
          disabled={isDisconnecting}
          className="shadow-lg"
        >
          {isDisconnecting ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Power className="size-4" />
          )}
          <span className="ml-2">Ngắt kết nối</span>
        </Button>
      </div>

      {/* Page Content */}
      {children}
    </div>
  );
}
