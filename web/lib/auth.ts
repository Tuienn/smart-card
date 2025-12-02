const USER_AUTH_KEY = "user_pin_verified";
const ADMIN_AUTH_KEY = "admin_pin_verified";

// User Auth
export function setUserAuthVerified() {
  if (typeof window !== "undefined") {
    document.cookie = `${USER_AUTH_KEY}=true; path=/; SameSite=Strict`;
  }
}

export function isUserAuthVerified(): boolean {
  if (typeof window !== "undefined") {
    return document.cookie.includes(`${USER_AUTH_KEY}=true`);
  }
  return false;
}

export function clearUserAuth() {
  if (typeof window !== "undefined") {
    document.cookie = `${USER_AUTH_KEY}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
  }
}

// Admin Auth
export function setAdminAuthVerified() {
  if (typeof window !== "undefined") {
    document.cookie = `${ADMIN_AUTH_KEY}=true; path=/; SameSite=Strict`;
  }
}

export function isAdminAuthVerified(): boolean {
  if (typeof window !== "undefined") {
    return document.cookie.includes(`${ADMIN_AUTH_KEY}=true`);
  }
  return false;
}

export function clearAdminAuth() {
  if (typeof window !== "undefined") {
    document.cookie = `${ADMIN_AUTH_KEY}=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
  }
}

// Legacy exports for backward compatibility
export const setAuthVerified = setUserAuthVerified;
export const isAuthVerified = isUserAuthVerified;
export const clearAuth = clearUserAuth;
