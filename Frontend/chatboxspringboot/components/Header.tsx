"use client";

import React, { useState } from "react";
import { CurrentUser, UserSummary } from "@/types/chat";

interface HeaderProps {
  currentUser: CurrentUser | null;
  onLogout: () => void;
  searchQuery: string;
  setSearchQuery: (q: string) => void;
  searchResults: UserSummary[];
  isSearchingUsers: boolean;
  onSelectUser: (user: UserSummary) => void;
  notifications?: import("@/types/chat").AppNotification[];
  unreadNotificationCount?: number;
  onMarkAllNotificationsRead?: () => void;
  onSelectNotification?: (n: import("@/types/chat").AppNotification) => void;
  isSseConnected?: boolean;
}

export default function Header({
  currentUser,
  onLogout,
  searchQuery,
  setSearchQuery,
  searchResults,
  isSearchingUsers,
  onSelectUser,
  notifications = [],
  unreadNotificationCount = 0,
  onMarkAllNotificationsRead,
  onSelectNotification,
  isSseConnected = false,
}: HeaderProps) {
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);

  return (
    <header className="h-14 border-b border-[#e4e6eb] px-4 flex items-center justify-between bg-white z-30 select-none shadow-[0_1px_2px_rgba(0,0,0,0.04)]">
      {/* Ô TÌM KIẾM NGƯỜI DÙNG THẬT TRÊN CSDL SUPABASE */}
      <div className="flex items-center gap-3 relative">
        <div className="flex items-center bg-[#f0f2f5] hover:bg-[#e4e6eb] transition-colors rounded-full px-3 py-2 w-60 sm:w-80">
          <svg
            className="w-4 h-4 text-[#65676b] flex-shrink-0"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm kiếm người dùng trên Supabase..."
            className="bg-transparent border-none outline-none text-[14px] text-[#050505] placeholder-[#65676b] ml-2 w-full"
          />
        </div>

        {/* KẾT QUẢ TÌM KIẾM NGƯỜI DÙNG */}
        {searchQuery.trim().length > 0 && (
          <div className="absolute left-0 top-12 w-80 bg-white rounded-xl shadow-xl border border-[#e4e6eb] py-2 px-1 z-50">
            <div className="px-3 py-1 text-xs font-bold text-[#65676b] uppercase">
              {isSearchingUsers ? "Đang tìm kiếm trên CSDL..." : "Người dùng trên Supabase"}
            </div>
            {searchResults.length === 0 && !isSearchingUsers ? (
              <div className="p-3 text-sm text-[#65676b] text-center">
                Không tìm thấy tài khoản nào khớp
              </div>
            ) : (
              searchResults.map((u) => (
                <div
                  key={u.id}
                  onClick={() => onSelectUser(u)}
                  className="p-2.5 hover:bg-[#f0f2f5] rounded-lg transition-colors flex items-center justify-between cursor-pointer"
                >
                  <div className="flex items-center gap-2.5">
                    <div className="w-8 h-8 rounded-full bg-[#0084ff] text-white flex items-center justify-center font-bold text-xs">
                      {u.tenTaiKhoan?.charAt(0) || "U"}
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-[#050505]">{u.tenTaiKhoan}</p>
                      <p className="text-xs text-[#65676b]">@{u.userName}</p>
                    </div>
                  </div>
                  <span className="text-xs font-semibold text-[#0064d1] bg-blue-50 px-2 py-1 rounded-full">
                    Nhắn tin
                  </span>
                </div>
              ))
            )}
          </div>
        )}
      </div>

      {/* PHẦN GIỮA: TRẠNG THÁI REALTIME SSE & SUPABASE */}
      <div className="hidden md:flex items-center gap-2">
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-700 text-xs font-semibold border border-emerald-200 shadow-xs">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span>{isSseConnected ? "Realtime SSE Online" : "Supabase DB Online"}</span>
        </div>
      </div>

      {/* BÊN PHẢI: ICON CHUÔNG THÔNG BÁO VÀ AVATAR NGƯỜI DÙNG */}
      <div className="flex items-center gap-2 relative">
        <button
          onClick={() => {
            setShowNotifications(!showNotifications);
            setShowProfileMenu(false);
          }}
          title="Thông báo"
          className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors relative cursor-pointer ${
            showNotifications ? "bg-[#e7f3ff] text-[#0064d1]" : "bg-[#f0f2f5] hover:bg-[#e4e6eb] text-[#050505]"
          }`}
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z" />
          </svg>
          {unreadNotificationCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 min-w-5 h-5 px-1 bg-rose-600 text-white text-[11px] font-bold rounded-full flex items-center justify-center border-2 border-white shadow-xs animate-bounce">
              {unreadNotificationCount > 9 ? "9+" : unreadNotificationCount}
            </span>
          )}
        </button>

        {/* POPUP THÔNG BÁO REALTIME */}
        {showNotifications && (
          <div className="absolute right-0 top-12 w-80 sm:w-96 bg-white rounded-xl shadow-xl border border-[#e4e6eb] py-3 px-2 z-50">
            <div className="flex items-center justify-between px-3 pb-2 border-b border-[#f0f2f5]">
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-lg text-[#050505]">Thông báo</h3>
                {isSseConnected && (
                  <span className="px-1.5 py-0.5 text-[10px] font-bold bg-emerald-100 text-emerald-800 rounded-full">
                    Realtime
                  </span>
                )}
              </div>
              {unreadNotificationCount > 0 && onMarkAllNotificationsRead && (
                <button
                  type="button"
                  onClick={onMarkAllNotificationsRead}
                  className="text-xs text-[#0084ff] font-semibold hover:underline cursor-pointer"
                >
                  Đánh dấu đã đọc
                </button>
              )}
            </div>

            <div className="max-h-80 overflow-y-auto divide-y divide-[#f0f2f5]">
              {notifications.length === 0 ? (
                <div className="p-6 text-center text-sm text-[#65676b]">
                  <p>Hiện không có thông báo mới nào từ máy chủ.</p>
                </div>
              ) : (
                notifications.map((n) => (
                  <div
                    key={n.notificationId}
                    onClick={() => {
                      if (onSelectNotification) {
                        onSelectNotification(n);
                        setShowNotifications(false);
                      }
                    }}
                    className={`p-3 flex items-start gap-2.5 transition-colors cursor-pointer rounded-lg ${
                      !n.isRead ? "bg-[#f0f7ff] hover:bg-[#e4efff]" : "hover:bg-[#f9fafb]"
                    }`}
                  >
                    <div className="w-8 h-8 rounded-full bg-[#0084ff] text-white flex items-center justify-center font-bold text-xs shrink-0 mt-0.5 shadow-xs">
                      {n.senderName?.charAt(0) || "🔔"}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs text-[#050505] leading-snug">
                        <span className="font-bold">{n.senderName || "Hệ thống"}</span>{" "}
                        {n.type === "GROUP_MESSAGE"
                          ? "đã gửi tin nhắn trong nhóm"
                          : "đã gửi cho bạn một tin nhắn"}
                      </p>
                      {n.messageContent && (
                        <p className="text-xs text-[#65676b] truncate mt-0.5">
                          "{n.messageContent}"
                        </p>
                      )}
                      <span className="text-[10px] text-[#8a8d91] block mt-1">
                        {n.sendAt ? new Date(n.sendAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "Vừa xong"}
                      </span>
                    </div>
                    {!n.isRead && (
                      <span className="w-2.5 h-2.5 rounded-full bg-[#0084ff] shrink-0 mt-1.5 shadow-xs" />
                    )}
                  </div>
                ))
              )}
            </div>
          </div>
        )}

        {/* ICON AVATAR NGƯỜI DÙNG */}
        <div className="relative">
          <button
            onClick={() => {
              setShowProfileMenu(!showProfileMenu);
              setShowNotifications(false);
            }}
            title="Tài khoản cá nhân"
            className="w-10 h-10 rounded-full overflow-hidden border border-[#ced0d4] hover:opacity-90 transition-opacity cursor-pointer flex items-center justify-center bg-[#0084ff] text-white font-bold text-sm shadow-xs"
          >
            {currentUser?.tenTaiKhoan?.charAt(0) || "U"}
          </button>

          {/* POPUP MENU AVATAR & ĐĂNG XUẤT */}
          {showProfileMenu && (
            <div className="absolute right-0 top-12 w-72 bg-white rounded-xl shadow-xl border border-[#e4e6eb] py-2 px-1 z-50 animate-in fade-in slide-in-from-top-2 duration-150">
              <div className="p-3 border-b border-[#f0f2f5] flex items-center gap-3">
                <div className="w-11 h-11 rounded-full bg-[#0084ff] text-white flex items-center justify-center font-bold text-base shadow-xs">
                  {currentUser?.tenTaiKhoan?.charAt(0) || "U"}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="font-bold text-[#050505] text-sm leading-tight truncate">
                    {currentUser?.tenTaiKhoan || "Người dùng"}
                  </p>
                  <p className="text-xs text-[#65676b] truncate mt-0.5">
                    {currentUser?.email || `@${currentUser?.userName}`}
                  </p>
                  <p className="text-[11px] text-emerald-600 font-semibold mt-0.5">
                    Đã đăng nhập (Supabase)
                  </p>
                </div>
              </div>

              {/* NÚT ĐĂNG XUẤT */}
              <div className="mt-1">
                <button
                  onClick={() => {
                    setShowProfileMenu(false);
                    onLogout();
                  }}
                  className="w-full text-left px-3 py-2 text-sm text-[#e41e3f] hover:bg-rose-50 rounded-lg transition-colors flex items-center gap-2 cursor-pointer"
                >
                  <svg className="w-4 h-4 text-[#e41e3f]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                  </svg>
                  Đăng xuất tài khoản
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
