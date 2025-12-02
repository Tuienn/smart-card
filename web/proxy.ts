import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Routes that require User PIN verification
const PROTECTED_USER_ROUTES = [
  "/user/read-data",
  "/user/write-data",
  "/user/change-pin",
];

// Routes that require Admin PIN verification
const PROTECTED_ADMIN_ROUTES = ["/admin/reset-user-pin"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Check User protected routes
  if (PROTECTED_USER_ROUTES.some((route) => pathname.startsWith(route))) {
    const isVerified =
      request.cookies.get("user_pin_verified")?.value === "true";

    if (!isVerified) {
      return NextResponse.redirect(new URL("/user", request.url));
    }
  }

  // Check Admin protected routes
  if (PROTECTED_ADMIN_ROUTES.some((route) => pathname.startsWith(route))) {
    const isVerified =
      request.cookies.get("admin_pin_verified")?.value === "true";

    if (!isVerified) {
      return NextResponse.redirect(new URL("/admin", request.url));
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/user/:path*", "/admin/:path*"],
};
