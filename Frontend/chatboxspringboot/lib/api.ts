import { AuthResponse, CurrentUser, UserSummary } from "@/types/chat";

const TOKEN_KEY = "chat_access_token";

export function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setStoredToken(token: string): void {
  if (typeof window !== "undefined") {
    localStorage.setItem(TOKEN_KEY, token);
  }
}

export function clearStoredToken(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem(TOKEN_KEY);
  }
}

export function getAuthHeaders(): HeadersInit {
  const token = getStoredToken();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  return headers;
}

export async function loginUser(userName: string, password: string): Promise<AuthResponse> {
  const res = await fetch("/api/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userName, password }),
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.errorMessage || data.message || "Tên đăng nhập hoặc mật khẩu không đúng");
  }

  setStoredToken(data.accessToken);
  return data;
}

export async function registerUser(
  tenTaiKhoan: string,
  email: string,
  userName: string,
  password: string
): Promise<any> {
  const res = await fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ tenTaiKhoan, email, userName, password }),
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.errorMessage || data.message || "Đăng ký không thành công");
  }

  return data;
}

export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const token = getStoredToken();
  if (!token) return null;

  try {
    const res = await fetch("/api/users/me", {
      headers: getAuthHeaders(),
    });
    if (!res.ok) {
      if (res.status === 401) {
        clearStoredToken();
      }
      return null;
    }
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchConversations(): Promise<any[]> {
  const res = await fetch("/api/conversations", {
    headers: getAuthHeaders(),
  });
  if (!res.ok) {
    return [];
  }
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

export async function createConversation(partnerId: string): Promise<any> {
  const res = await fetch("/api/conversations", {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify({ partnerId }),
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.errorMessage || data.message || "Lỗi tạo hội thoại");
  }
  return data;
}

export async function searchUsers(keyword: string): Promise<UserSummary[]> {
  if (!keyword.trim()) return [];
  const res = await fetch(`/api/users/search?keyword=${encodeURIComponent(keyword)}`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  const data = await res.json();
  return data.content || [];
}

export async function uploadImageToCloudinary(file: File): Promise<any> {
  const formData = new FormData();
  formData.append("file", file);

  const token = getStoredToken();
  const headers: HeadersInit = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const res = await fetch("/api/upload", {
    method: "POST",
    headers,
    body: formData,
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.error || data.errorMessage || data.message || "Tải ảnh thất bại");
  }
  return data;
}

export async function sendMessageApi(payload: {
  conversationId: string;
  messageContent?: string;
  type?: string;
  imageUrl?: string;
  attachmentPublicId?: string;
}): Promise<any> {
  const res = await fetch("/api/messages", {
    method: "POST",
    headers: getAuthHeaders(),
    body: JSON.stringify(payload),
  });

  const data = await res.json();
  if (!res.ok) {
    throw new Error(data.errorMessage || data.message || "Lỗi gửi tin nhắn");
  }
  return data;
}

export async function fetchMessagesApi(conversationId: string): Promise<any[]> {
  const res = await fetch(`/api/conversations/${conversationId}/messages`, {
    headers: getAuthHeaders(),
  });
  if (!res.ok) return [];
  const data = await res.json();
  return Array.isArray(data) ? data : [];
}

