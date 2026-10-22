"use client";

import React, { useState, useEffect } from "react";
import Header from "@/components/Header";
import Sidebar from "@/components/Sidebar";
import ChatArea from "@/components/ChatArea";
import EmptyState from "@/components/EmptyState";
import AuthModal from "@/components/AuthModal";
import {
  fetchCurrentUser,
  clearStoredToken,
  fetchConversations,
  searchUsers,
  createConversation,
  uploadImageToCloudinary,
  sendMessageApi,
  fetchMessagesApi,
} from "@/lib/api";
import { Conversation, CurrentUser, Message, UserSummary } from "@/types/chat";

export default function MessengerPage() {
  // Trạng thái tài khoản người dùng
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  // Trạng thái cuộc hội thoại
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"all" | "unread" | "group">("all");
  const [isLoadingConversations, setIsLoadingConversations] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  // Trạng thái tìm kiếm người dùng thật từ Supabase
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [isSearchingUsers, setIsSearchingUsers] = useState(false);

  // 1. Kiểm tra phiên đăng nhập khi tải trang
  useEffect(() => {
    async function checkAuth() {
      setIsAuthChecking(true);
      try {
        const user = await fetchCurrentUser();
        if (user) {
          setCurrentUser(user);
        }
      } catch (err) {
        console.warn("Chưa đăng nhập:", err);
      } finally {
        setIsAuthChecking(false);
      }
    }
    checkAuth();
  }, []);

  // 2. Tải danh sách hội thoại từ Supabase khi người dùng đã đăng nhập
  const loadConversations = async () => {
    if (!currentUser) return;
    setIsLoadingConversations(true);
    try {
      const data = await fetchConversations();
      const mapped: Conversation[] = data.map((item: any) => ({
        id: item.conversationId,
        name: item.partnerName || item.groupName || "Người dùng",
        avatar:
          item.partnerAvatarUrl ||
          `https://ui-avatars.com/api/?name=${encodeURIComponent(
            item.partnerName || "User"
          )}&background=0084ff&color=fff&bold=true`,
        lastMessage: item.lastMessageContent || "Đoạn chat mới tạo",
        time: item.lastMessageAt
          ? new Date(item.lastMessageAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
          : "Mới",
        unread: (item.unreadCount || 0) > 0,
        online: true,
        isGroup: item.conversationType === "GROUP",
        messages: item.lastMessageContent
          ? [
              {
                id: "init",
                sender: "partner",
                text: item.lastMessageContent,
                time: "Gần đây",
              },
            ]
          : [],
      }));

      setConversations(mapped);
      if (mapped.length > 0 && !selectedId) {
        setSelectedId(mapped[0].id);
      }
    } catch (err) {
      console.warn("Lỗi tải hội thoại:", err);
    } finally {
      setIsLoadingConversations(false);
    }
  };

  useEffect(() => {
    if (currentUser) {
      loadConversations();
    }
  }, [currentUser]);

  // 3. Tìm kiếm người dùng thật trên Supabase
  useEffect(() => {
    const q = searchQuery.trim();
    if (!q || !currentUser) {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(async () => {
      setIsSearchingUsers(true);
      try {
        const results = await searchUsers(q);
        setSearchResults(results.filter((u) => u.id !== currentUser.id));
      } catch (err) {
        console.warn("Lỗi tìm kiếm:", err);
      } finally {
        setIsSearchingUsers(false);
      }
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, currentUser]);

  // 4. Tải lịch sử tin nhắn thật của cuộc trò chuyện hiện tại từ Backend / Supabase
  useEffect(() => {
    if (!selectedId || !currentUser) return;
    const currentConvId = selectedId;
    const currentUserId = currentUser.id;

    async function loadMessages() {
      try {
        const data = await fetchMessagesApi(currentConvId);
        if (Array.isArray(data)) {
          const mapped: Message[] = data.map((m: any) => ({
            id: m.messageId,
            sender: m.senderId === currentUserId ? "me" : "partner",
            text: m.messageContent || "",
            time: m.sendAt
              ? new Date(m.sendAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
              : "Vừa xong",
            type: m.type?.toLowerCase() || (m.imageUrl ? "image" : "text"),
            imageUrl: m.imageUrl || undefined,
            attachmentPublicId: m.attachmentPublicId || undefined,
          }));

          setConversations((prev) =>
            prev.map((c) =>
              c.id === currentConvId
                ? { ...c, messages: mapped, unread: false }
                : c
            )
          );
        }
      } catch (err) {
        console.warn("Lỗi tải tin nhắn:", err);
      }
    }
    loadMessages();
  }, [selectedId, currentUser]);

  // 5. Bắt đầu trò chuyện với người dùng tìm thấy
  const handleStartConversationWithUser = async (partner: UserSummary) => {
    try {
      const data = await createConversation(partner.id);
      const convId = data.conversationId;

      const newConv: Conversation = {
        id: convId,
        name: partner.tenTaiKhoan,
        avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(
          partner.tenTaiKhoan
        )}&background=0084ff&color=fff&bold=true`,
        lastMessage: "Đã bắt đầu cuộc trò chuyện mới",
        time: "Vừa xong",
        unread: false,
        online: true,
        messages: [],
      };

      setConversations((prev) => {
        const exists = prev.find((c) => c.id === convId);
        if (exists) return prev;
        return [newConv, ...prev];
      });

      setSelectedId(convId);
      setSearchQuery("");
    } catch (err: any) {
      alert("Lỗi khi mở cuộc trò chuyện: " + err.message);
    }
  };

  // 6. Gửi tin nhắn văn bản và lưu vào Supabase
  const handleSendMessage = async (text: string) => {
    if (!selectedId || !text.trim()) return;

    const tempId = Date.now().toString();
    const tempMsg: Message = {
      id: tempId,
      sender: "me",
      text,
      time: "Vừa xong",
      type: "text",
    };

    setConversations((prev) =>
      prev.map((c) => {
        if (c.id === selectedId) {
          return {
            ...c,
            lastMessage: `Bạn: ${text} · Vừa xong`,
            messages: [...c.messages, tempMsg],
          };
        }
        return c;
      })
    );

    try {
      const saved = await sendMessageApi({
        conversationId: selectedId,
        messageContent: text,
        type: "TEXT",
      });

      if (saved?.messageId) {
        setConversations((prev) =>
          prev.map((c) => {
            if (c.id === selectedId) {
              return {
                ...c,
                messages: c.messages.map((m) =>
                  m.id === tempId ? { ...m, id: saved.messageId } : m
                ),
              };
            }
            return c;
          })
        );
      }
    } catch (err: any) {
      console.error("Lỗi gửi tin nhắn:", err);
    }
  };

  // 7. Upload ảnh lên Cloudinary thật & Lưu link ảnh + publicId vào Supabase
  const handleUploadImage = async (file: File) => {
    if (!selectedId) return;
    setIsUploading(true);
    const tempId = Date.now().toString();

    const tempMsg: Message = {
      id: tempId,
      sender: "me",
      text: `⏳ Đang tải ảnh "${file.name}" lên Cloudinary...`,
      time: "Vừa xong",
      type: "text",
    };

    setConversations((prev) =>
      prev.map((c) =>
        c.id === selectedId ? { ...c, messages: [...c.messages, tempMsg] } : c
      )
    );

    try {
      // B1: Tải ảnh lên Cloudinary
      const data = await uploadImageToCloudinary(file);

      // B2: Lưu URL ảnh và Cloudinary publicId vào CSDL Supabase
      const saved = await sendMessageApi({
        conversationId: selectedId,
        imageUrl: data.url,
        attachmentPublicId: data.publicId,
        type: "IMAGE",
      });

      const realMsg: Message = {
        id: saved?.messageId || tempId,
        sender: "me",
        text: "",
        time: "Vừa xong",
        type: "image",
        imageUrl: data.url,
        attachmentPublicId: data.publicId,
        fileName: data.originalFilename || file.name,
      };

      setConversations((prev) =>
        prev.map((c) => {
          if (c.id === selectedId) {
            return {
              ...c,
              lastMessage: `Bạn: [Hình ảnh] · Vừa xong`,
              messages: c.messages.map((m) => (m.id === tempId ? realMsg : m)),
            };
          }
          return c;
        })
      );
    } catch (err: any) {
      alert("Lỗi upload ảnh lên Cloudinary: " + err.message);
      setConversations((prev) =>
        prev.map((c) =>
          c.id === selectedId
            ? { ...c, messages: c.messages.filter((m) => m.id !== tempId) }
            : c
        )
      );
    } finally {
      setIsUploading(false);
    }
  };

  // 7. Xử lý Đăng xuất
  const handleLogout = () => {
    clearStoredToken();
    setCurrentUser(null);
    setConversations([]);
    setSelectedId(null);
  };

  const selectedConversation = conversations.find((c) => c.id === selectedId) || null;

  // Nếu đang kiểm tra phiên đăng nhập
  if (isAuthChecking) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-white">
        <div className="flex flex-col items-center gap-3">
          <svg className="w-8 h-8 animate-spin text-[#0084ff]" viewBox="0 0 24 24" fill="none">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
          </svg>
          <span className="text-sm font-medium text-[#65676b]">Đang khởi động Messenger...</span>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden bg-white text-[#050505] font-sans antialiased">
      {/* NẾU CHƯA ĐĂNG NHẬP: HIỂN THỊ MODAL ĐĂNG NHẬP / ĐĂNG KÝ */}
      {!currentUser && (
        <AuthModal
          onSuccess={(user) => {
            setCurrentUser(user);
          }}
        />
      )}

      {/* THANH HEADER */}
      <Header
        currentUser={currentUser}
        onLogout={handleLogout}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        searchResults={searchResults}
        isSearchingUsers={isSearchingUsers}
        onSelectUser={handleStartConversationWithUser}
      />

      {/* NỘI DUNG CHÍNH */}
      <div className="flex flex-1 overflow-hidden">
        {/* SIDEBAR BÊN TRÁI */}
        <Sidebar
          conversations={conversations}
          selectedId={selectedId}
          onSelectChat={(id) => setSelectedId(id)}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          isLoading={isLoadingConversations}
          onRefresh={loadConversations}
        />

        {/* KHUNG CHAT BÊN PHẢI */}
        <main className="hidden sm:flex flex-1 flex-col h-[calc(100vh-56px)] bg-white relative">
          {!selectedConversation ? (
            <EmptyState />
          ) : (
            <ChatArea
              conversation={selectedConversation}
              onSendMessage={handleSendMessage}
              onUploadImage={handleUploadImage}
              isUploading={isUploading}
            />
          )}
        </main>
      </div>
    </div>
  );
}
