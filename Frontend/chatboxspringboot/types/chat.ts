export interface Message {
  id: string;
  sender: "me" | "partner";
  senderName?: string;
  senderId?: string;
  text: string;
  time: string;
  type?: "text" | "image" | "file";
  imageUrl?: string;
  attachmentPublicId?: string;
  fileName?: string;
}

export interface Conversation {
  id: string;
  name: string;
  avatar: string;
  lastMessage: string;
  time: string;
  unread: boolean;
  online?: boolean;
  isGroup?: boolean;
  memberCount?: number;
  partnerId?: string;
  messages: Message[];
}

export interface UserSummary {
  id: string;
  tenTaiKhoan: string;
  userName: string;
}

export interface CurrentUser {
  id: string;
  tenTaiKhoan: string;
  email: string;
  userName: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface AppNotification {
  notificationId: string;
  type: string;
  isRead: boolean;
  senderId?: string;
  senderName?: string;
  messageContent?: string;
  conversationId?: string;
  sendAt: string;
}

