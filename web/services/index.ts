import { JAVA_API_URL } from "@/constants/env.config";

const javaService = async <T = any>(
  url: string,
  options?: RequestInit,
  isBlob?: boolean
): Promise<T> => {
  const defaultHeaders: Record<string, string> = {
    Accept: "application/json",
  };

  const mergedHeaders = new Headers({
    ...(options?.body instanceof FormData
      ? defaultHeaders
      : { ...defaultHeaders, "Content-Type": "application/json" }),
    ...(options?.headers as Record<string, string> | undefined),
  });

  const res = await fetch(`${JAVA_API_URL}/api/v1${url}`, {
    ...options,
    headers: mergedHeaders,
  });

  const data =
    isBlob && res.ok ? await res.blob() : await res.json().catch(() => null);

  if (!res.ok) {
    throw new Error(`HTTP ${res.status} ${res.statusText}`);
  }

  return (data ?? undefined) as T;
};

export default javaService;
