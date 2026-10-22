"use client";

import React, { useMemo } from "react";
import { Conversation } from "@/types/chat";

interface SidebarProps {
  conversations: Conversation[];
  selectedId: string | null;
  onSelectChat: (id: string) => void;
  activeTab: "all" | "unread" | "group";
  setActiveTab: (tab: "all" | "unread" | "group") => void;
  isLoading: boolean;
  onRefresh: () => void;
}

export default function Sidebar({
  conversations,
  selectedId,
  onSelectChat,
  activeTab,
  setActiveTab,
  isLoading,
  onRefresh,
}: SidebarProps) {
  const filteredConversations = useMemo(() => {
    return conversations.filter((c) => {
      if (activeTab === "unread") return c.unread;
      if (activeTab === "group") return c.isGroup;
      return true;
    });
  }, [conversations, activeTab]);

  return (
    <aside className="w-full sm:w-80 md:w-96 border-r border-[#e4e6eb] flex flex-col h-[calc(100vh-56px)] bg-white select-none">
      {/* HEADER SIDEBAR: TIÊU ĐỀ "ĐOẠN CHAT" & NÚT LÀM MỚI */}
      <div className="px-4 pt-3 pb-2 flex items-center justify-between">
        <h1 className="text-2xl font-bold tracking-tight text-[#050505]">Đoạn chat</h1>
        <div className="flex items-center gap-2">
          <button
            onClick={onRefresh}
            className="w-9 h-9 rounded-full bg-[#f0f2f5] hover:bg-[#e4e6eb] flex items-center justify-center transition-colors text-[#050505] cursor-pointer"
            title="Tải lại từ CSDL Supabase"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
      </div>

      {/* BỘ LỌC TABS */}
      <div className="px-4 py-2 flex items-center gap-2">
        <button
          onClick={() => setActiveTab("all")}
          className={`px-3 py-1.5 rounded-full text-[14px] font-semibold transition-colors cursor-pointer ${
            activeTab === "all"
              ? "bg-[#e7f3ff] text-[#0064d1]"
              : "bg-transparent text-[#65676b] hover:bg-[#f0f2f5]"
          }`}
        >
          Tất cả
        </button>
        <button
          onClick={() => setActiveTab("unread")}
          className={`px-3 py-1.5 rounded-full text-[14px] font-semibold transition-colors cursor-pointer ${
            activeTab === "unread"
              ? "bg-[#e7f3ff] text-[#0064d1]"
              : "bg-transparent text-[#65676b] hover:bg-[#f0f2f5]"
          }`}
        >
          Chưa đọc
        </button>
        <button
          onClick={() => setActiveTab("group")}
          className={`px-3 py-1.5 rounded-full text-[14px] font-semibold transition-colors cursor-pointer ${
            activeTab === "group"
              ? "bg-[#e7f3ff] text-[#0064d1]"
              : "bg-transparent text-[#65676b] hover:bg-[#f0f2f5]"
          }`}
        >
          Nhóm
        </button>
      </div>

      {/* DANH SÁCH CUỘC HỘI THOẠI THẬT TỪ CSDL SUPABASE */}
      <div className="flex-1 overflow-y-auto px-2 space-y-0.5">
        {isLoading ? (
          <div className="p-8 text-center text-sm text-[#65676b] flex flex-col items-center gap-2">
            <svg className="w-5 h-5 animate-spin text-[#0084ff]" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>Đang kết nối CSDL Supabase...</span>
          </div>
        ) : filteredConversations.length === 0 ? (
          <div className="p-8 text-center text-sm text-[#65676b] flex flex-col items-center gap-3">
            <div className="w-12 h-12 rounded-full bg-[#f0f2f5] flex items-center justify-center text-[#65676b]">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div>
              <p className="font-semibold text-[#050505]">Chưa có đoạn chat nào</p>
              <p className="text-xs text-[#65676b] mt-1">
                Gõ tên hoặc username vào thanh tìm kiếm ở trên để kết nối với người dùng trong Supabase.
              </p>
            </div>
          </div>
        ) : (
          filteredConversations.map((c) => {
            const isSelected = c.id === selectedId;
            return (
              <div
                key={c.id}
                onClick={() => onSelectChat(c.id)}
                className={`flex items-center gap-3 p-2 rounded-lg cursor-pointer transition-colors relative ${
                  isSelected ? "bg-[#ebf5ff]" : "hover:bg-[#f2f2f2]"
                }`}
              >
                <div className="relative flex-shrink-0">
                  <div className="w-12 h-12 rounded-full overflow-hidden bg-gray-100 border border-[#e4e6eb]">
                    <img src={c.avatar} alt={c.name} className="w-full h-full object-cover" />
                  </div>
                  {c.online && (
                    <span className="absolute bottom-0 right-0 w-3.5 h-3.5 rounded-full bg-[#31a24c] border-2 border-white shadow-sm" />
                  )}
                </div>

                <div className="flex-1 min-w-0 pr-4">
                  <h4 className="text-[15px] font-semibold text-[#050505] truncate leading-tight">
                    {c.name}
                  </h4>
                  <p className={`text-[13px] truncate mt-0.5 ${c.unread ? "text-[#050505] font-semibold" : "text-[#65676b]"}`}>
                    {c.lastMessage}
                  </p>
                </div>

                {c.unread && (
                  <span className="w-2.5 h-2.5 rounded-full bg-[#0084ff] flex-shrink-0 mr-1" />
                )}
              </div>
            );
          })
        )}
      </div>
    </aside>
  );
}
