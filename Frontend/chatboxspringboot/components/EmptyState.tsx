"use client";

import React from "react";

export default function EmptyState() {
  return (
    <div className="flex-1 flex flex-col items-center justify-center select-none bg-white p-6">
      <div className="relative mb-5 flex items-center justify-center">
        <div className="w-24 h-24 rounded-[32px] bg-gradient-to-tr from-[#f3e8ff] via-[#e0e7ff] to-[#fce7f3] border border-[#e2e8f0] shadow-[0_12px_32px_rgba(147,51,234,0.12)] flex items-center justify-center relative">
          <svg
            className="w-12 h-12 drop-shadow-[0_4px_10px_rgba(217,70,239,0.35)]"
            viewBox="0 0 24 24"
            fill="none"
          >
            <defs>
              <linearGradient id="cursorGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#ec4899" />
                <stop offset="50%" stopColor="#a855f7" />
                <stop offset="100%" stopColor="#3b82f6" />
              </linearGradient>
            </defs>
            <path d="M3 3L10.5 21L13.5 13.5L21 10.5L3 3Z" fill="url(#cursorGradient)" />
          </svg>
          <div className="absolute -bottom-2 left-6 w-4 h-4 bg-gradient-to-tr from-[#f3e8ff] to-[#e0e7ff] rotate-45 border-r border-b border-[#e2e8f0]"></div>
        </div>
      </div>

      <h2 className="text-[17px] font-bold text-[#050505] tracking-tight">
        Chưa chọn đoạn chat nào
      </h2>
      <p className="text-xs text-[#65676b] mt-1 max-w-sm text-center">
        Chọn một cuộc trò chuyện từ danh sách bên trái hoặc tìm kiếm bạn bè trên Supabase để bắt đầu nhắn tin.
      </p>
    </div>
  );
}
