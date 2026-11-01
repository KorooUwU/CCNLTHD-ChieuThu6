"use client";

import React, { useState, useEffect, useRef } from "react";
import Header from "@/components/Header";
import Sidebar from "@/components/Sidebar";
import ChatArea from "@/components/ChatArea";
import EmptyState from "@/components/EmptyState";
import AuthModal from "@/components/AuthModal";
import CreateGroupModal from "@/components/CreateGroupModal";
import {
  fetchCurrentUser,
  clearStoredToken,
  getStoredToken,
  fetchConversations,
  searchUsers,
  createConversation,
  uploadImageToCloudinary,
  sendMessageApi,
  fetchMessagesApi,
  fetchNotifications,
  fetchUnreadNotificationCount,
  markAllNotificationsRead,
  markNotificationAsRead,
  markNotificationsReadBySender,
} from "@/lib/api";
import { Conversation, CurrentUser, Message, UserSummary, AppNotification } from "@/types/chat";

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

  // Trạng thái nhóm & thông báo realtime
  const [isCreateGroupOpen, setIsCreateGroupOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0);
  const [isSseConnected, setIsSseConnected] = useState(false);

  // Trạng thái tìm kiếm người dùng thật từ Supabase
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [isSearchingUsers, setIsSearchingUsers] = useState(false);

  // Ref lưu selectedId hiện tại để dùng trong SSE callback không bị stale closure
  const selectedIdRef = useRef<string | null>(null);
  useEffect(() => {
    selectedIdRef.current = selectedId;
  }, [selectedId]);

  // Ref lưu danh sách conversations hiện tại để tìm kiếm nhanh và chính xác
  const conversationsRef = useRef<Conversation[]>([]);
  useEffect(() => {
    conversationsRef.current = conversations;
  }, [conversations]);

  // Tự động đồng bộ số lượng thông báo chưa đọc từ danh sách thông báo
  useEffect(() => {
    const unread = notifications.filter((n) => !n.isRead).length;
    setUnreadNotificationCount(unread);
  }, [notifications]);

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
        name: item.partnerName || item.groupName || item.otherTenTaiKhoan || "Người dùng",
        partnerId: item.partnerId || item.otherUserId,
        avatar:
          item.partnerAvatarUrl ||
          `https://ui-avatars.com/api/?name=${encodeURIComponent(
            item.partnerName || item.otherTenTaiKhoan || "User"
          )}&background=0084ff&color=fff&bold=true`,
        lastMessage: item.lastMessageContent || "Đoạn chat mới tạo",
        time: item.lastMessageAt
          ? new Date(item.lastMessageAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
          : "Mới",
        unread: (item.unreadCount || 0) > 0,
        online: true,
        isGroup: item.conversationType === "GROUP",
        memberCount: item.memberCount,
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

  // Tải danh sách thông báo & số lượng chưa đọc ban đầu
  const loadNotifications = async () => {
    if (!currentUser) return;
    try {
      const [list, count] = await Promise.all([
        fetchNotifications(),
        fetchUnreadNotificationCount(),
      ]);
      setNotifications(list);
      setUnreadNotificationCount(count || list.filter((n: any) => !n.isRead).length);
    } catch (err) {
      console.warn("Lỗi tải thông báo:", err);
    }
  };

  const handleMarkAllNotificationsRead = async () => {
    await markAllNotificationsRead();
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    setUnreadNotificationCount(0);
  };

  // Mở kết nối SSE Realtime khi người dùng đăng nhập
  useEffect(() => {
    if (!currentUser) return;

    loadNotifications();

    const token = getStoredToken();
    if (!token) return;

    let eventSource: EventSource | null = null;
    try {
      const sseUrl = `http://localhost:8081/api/notifications/stream?token=${encodeURIComponent(token)}`;
      eventSource = new EventSource(sseUrl);

      eventSource.addEventListener("CONNECTED", () => {
        setIsSseConnected(true);
      });

      // Nhận tin nhắn mới realtime (cả chat 1-1 và nhóm)
      eventSource.addEventListener("NEW_MESSAGE", (e) => {
        try {
          const data = JSON.parse(e.data);
          const currentSelected = selectedIdRef.current;

          const incomingMsg: Message = {
            id: data.messageId,
            sender: data.senderId === currentUser.id ? "me" : "partner",
            senderName: data.senderName,
            senderId: data.senderId,
            text: data.messageContent || "",
            time: data.sendAt
              ? new Date(data.sendAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
              : "Vừa xong",
            type: data.type?.toLowerCase() || (data.imageUrl ? "image" : "text"),
            imageUrl: data.imageUrl,
            attachmentPublicId: data.attachmentPublicId,
          };

          setConversations((prev) => {
            const exists = prev.find((c) => c.id === data.conversationId);
            if (!exists) {
              loadConversations();
              return prev;
            }
            return prev.map((c) => {
              if (c.id === data.conversationId) {
                const isMsgExist = c.messages.some((m) => m.id === data.messageId);
                return {
                  ...c,
                  lastMessage: `${data.senderId === currentUser.id ? "Bạn: " : (c.isGroup ? `${data.senderName}: ` : "")}${data.messageContent || "[Hình ảnh]"}`,
                  unread: c.id !== currentSelected,
                  messages: isMsgExist ? c.messages : [...c.messages, incomingMsg],
                };
              }
              return c;
            });
          });
        } catch (err) {
          console.error("Lỗi parse sự kiện NEW_MESSAGE:", err);
        }
      });

      // Nhận thông báo realtime
      eventSource.addEventListener("NOTIFICATION", (e) => {
        try {
          const data = JSON.parse(e.data);
          const currentSelected = selectedIdRef.current;
          const currentConvs = conversationsRef.current;
          const currentConv = currentConvs.find((c) => c.id === currentSelected);

          // Nếu người dùng đang mở đúng cuộc trò chuyện này, coi như đã đọc ngay
          const isViewingThisChat =
            currentSelected &&
            ((data.conversationId && data.conversationId === currentSelected) ||
              (currentConv?.partnerId && data.senderId === currentConv.partnerId) ||
              (currentConv?.isGroup && data.conversationId === currentSelected));

          const incomingNotif: AppNotification = {
            ...data,
            isRead: Boolean(isViewingThisChat),
          };

          if (isViewingThisChat && data.notificationId) {
            markNotificationAsRead(data.notificationId);
          }

          setNotifications((prev) => [incomingNotif, ...prev]);
        } catch (err) {
          console.error("Lỗi parse sự kiện NOTIFICATION:", err);
        }
      });

      // Nhận sự kiện cập nhật preview hội thoại realtime
      eventSource.addEventListener("CONVERSATION_UPDATED", (e) => {
        try {
          const data = JSON.parse(e.data);
          const currentSelected = selectedIdRef.current;
          setConversations((prev) =>
            prev.map((c) => {
              if (c.id === data.conversationId) {
                return {
                  ...c,
                  lastMessage: data.lastMessageContent || c.lastMessage,
                  unread: data.unreadCount > 0 && c.id !== currentSelected,
                };
              }
              return c;
            })
          );
        } catch (err) {
          console.error("Lỗi parse sự kiện CONVERSATION_UPDATED:", err);
        }
      });

      eventSource.onerror = () => {
        setIsSseConnected(false);
      };
    } catch (err) {
      console.warn("Không thể mở kết nối SSE:", err);
    }

    return () => {
      if (eventSource) {
        eventSource.close();
        setIsSseConnected(false);
      }
    };
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

  // Xử lý khi người dùng chọn/chuyển qua một cuộc trò chuyện
  const handleSelectConversation = (convId: string) => {
    setSelectedId(convId);

    // 1. Đánh dấu hội thoại đó là đã đọc trong danh sách sidebar
    setConversations((prev) =>
      prev.map((c) => (c.id === convId ? { ...c, unread: false } : c))
    );

    // 2. Tìm thông tin hội thoại đích để đối soát chính xác
    const targetConv =
      conversationsRef.current.find((c) => c.id === convId) ||
      conversations.find((c) => c.id === convId);

    // 3. Tự động xóa thông báo và trừ chấm đỏ trên chuông cho hội thoại này
    setNotifications((prev) => {
      const idsToMarkRead: string[] = [];
      const updated = prev.map((n) => {
        const isMatch =
          (n.conversationId && n.conversationId === convId) ||
          (targetConv?.partnerId && n.senderId === targetConv.partnerId) ||
          (targetConv && n.senderName && (
            targetConv.name.toLowerCase().includes(n.senderName.toLowerCase()) ||
            n.senderName.toLowerCase().includes(targetConv.name.toLowerCase())
          )) ||
          (targetConv?.isGroup && (n.conversationId === convId || n.type === "GROUP_MESSAGE"));

        if (isMatch && !n.isRead) {
          idsToMarkRead.push(n.notificationId);
          return { ...n, isRead: true };
        }
        return n;
      });

      // Lưu trạng thái đã đọc vào CSDL Supabase
      if (idsToMarkRead.length > 0) {
        idsToMarkRead.forEach((id) => markNotificationAsRead(id));
      }
      if (targetConv?.partnerId) {
        markNotificationsReadBySender(targetConv.partnerId);
      }

      return updated;
    });
  };

  // Xử lý khi người dùng bấm vào một thông báo trong popup chuông
  const handleSelectNotification = (n: AppNotification) => {
    // 1. Đánh dấu thông báo này là đã đọc ngay lập tức và lưu DB
    setNotifications((prev) =>
      prev.map((item) =>
        item.notificationId === n.notificationId ? { ...item, isRead: true } : item
      )
    );
    if (n.notificationId) {
      markNotificationAsRead(n.notificationId);
    }
    if (n.senderId) {
      markNotificationsReadBySender(n.senderId);
    }

    // 2. Chuyển ngay đến cuộc trò chuyện tương ứng
    if (n.conversationId) {
      handleSelectConversation(n.conversationId);
    } else {
      const currentList = conversationsRef.current.length > 0 ? conversationsRef.current : conversations;
      const matched = currentList.find((c) => {
        if (n.senderId && c.partnerId && c.partnerId === n.senderId) return true;
        if (n.senderName && c.name && (
          c.name.toLowerCase().includes(n.senderName.toLowerCase()) ||
          n.senderName.toLowerCase().includes(c.name.toLowerCase())
        )) return true;
        return false;
      });

      if (matched) {
        handleSelectConversation(matched.id);
      }
    }
  };

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
            senderName: m.senderName,
            senderId: m.senderId,
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
        partnerId: partner.id,
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

      handleSelectConversation(convId);
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

      {/* MODAL TẠO NHÓM CHAT MỚI */}
      {isCreateGroupOpen && (
        <CreateGroupModal
          currentUserId={currentUser?.id}
          onClose={() => setIsCreateGroupOpen(false)}
          onSuccess={async (newGroup) => {
            await loadConversations();
            if (newGroup?.conversationId) {
              handleSelectConversation(newGroup.conversationId);
            }
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
        notifications={notifications}
        unreadNotificationCount={unreadNotificationCount}
        onMarkAllNotificationsRead={handleMarkAllNotificationsRead}
        onSelectNotification={handleSelectNotification}
        isSseConnected={isSseConnected}
      />

      {/* NỘI DUNG CHÍNH */}
      <div className="flex flex-1 overflow-hidden">
        {/* SIDEBAR BÊN TRÁI */}
        <Sidebar
          conversations={conversations}
          selectedId={selectedId}
          onSelectChat={handleSelectConversation}
          activeTab={activeTab}
          setActiveTab={setActiveTab}
          isLoading={isLoadingConversations}
          onRefresh={loadConversations}
          onOpenCreateGroup={() => setIsCreateGroupOpen(true)}
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
