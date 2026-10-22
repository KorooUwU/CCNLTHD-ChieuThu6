"use client";

import React, { useState, useRef, useEffect } from "react";
import { Conversation } from "@/types/chat";

interface ChatAreaProps {
  conversation: Conversation;
  onSendMessage: (text: string) => void;
  onUploadImage: (file: File) => Promise<void>;
  isUploading: boolean;
}

export default function ChatArea({
  conversation,
  onSendMessage,
  onUploadImage,
  isUploading,
}: ChatAreaProps) {
  const [inputText, setInputText] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [conversation.messages]);

  const handleSend = () => {
    if (!inputText.trim()) return;
    onSendMessage(inputText.trim());
    setInputText("");
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onUploadImage(file);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  return (
    <div className="flex-1 flex flex-col h-full bg-white">
      {/* Input ẩn để upload file lên Cloudinary */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileChange}
        accept="image/*"
        className="hidden"
      />

      {/* HEADER CUỘC HỘI THOẠI */}
      <div className="h-16 border-b border-[#e4e6eb] px-4 flex items-center justify-between bg-white shadow-sm flex-shrink-0">
        <div className="flex items-center gap-3">
          <div className="relative">
            <img
              src={conversation.avatar}
              alt={conversation.name}
              className="w-10 h-10 rounded-full object-cover border border-[#e4e6eb]"
            />
            {conversation.online && (
              <span className="absolute bottom-0 right-0 w-3 h-3 rounded-full bg-[#31a24c] border-2 border-white" />
            )}
          </div>
          <div>
            <h3 className="font-bold text-[15px] text-[#050505] leading-tight">
              {conversation.name}
            </h3>
            <p className="text-[12px] text-emerald-600 font-medium">
              Đang kết nối Supabase
            </p>
          </div>
        </div>

        {/* NÚT GỌI THOẠI / VIDEO / THÔNG TIN */}
        <div className="flex items-center gap-1 text-[#0084ff]">
          <button className="w-9 h-9 rounded-full hover:bg-[#f0f2f5] flex items-center justify-center transition-colors cursor-pointer" title="Bắt đầu cuộc gọi thoại">
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M6.62 10.79a15.053 15.053 0 006.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z" />
            </svg>
          </button>
          <button className="w-9 h-9 rounded-full hover:bg-[#f0f2f5] flex items-center justify-center transition-colors cursor-pointer" title="Bắt đầu cuộc gọi video">
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z" />
            </svg>
          </button>
          <button className="w-9 h-9 rounded-full hover:bg-[#f0f2f5] flex items-center justify-center transition-colors cursor-pointer" title="Thông tin về cuộc trò chuyện">
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z" />
            </svg>
          </button>
        </div>
      </div>

      {/* DANH SÁCH TIN NHẮN */}
      <div className="flex-1 overflow-y-auto p-4 space-y-3 bg-white">
        <div className="text-center my-4">
          <div className="w-16 h-16 rounded-full overflow-hidden mx-auto mb-2 border border-[#e4e6eb]">
            <img
              src={conversation.avatar}
              alt={conversation.name}
              className="w-full h-full object-cover"
            />
          </div>
          <h4 className="font-bold text-[#050505] text-base">{conversation.name}</h4>
          <p className="text-xs text-[#65676b] mt-0.5">Cuộc trò chuyện thực tế trong CSDL Supabase</p>
        </div>

        {conversation.messages.map((m) => {
          const isMe = m.sender === "me";
          return (
            <div
              key={m.id}
              className={`flex items-end gap-2 ${isMe ? "justify-end" : "justify-start"}`}
            >
              {!isMe && (
                <img
                  src={conversation.avatar}
                  alt="avatar"
                  className="w-7 h-7 rounded-full object-cover mb-1 flex-shrink-0"
                />
              )}

              {/* TIN NHẮN (ẢNH / VĂN BẢN / HOẶC CẢ HAI) */}
              <div className={`flex flex-col gap-1 max-w-[70%] sm:max-w-sm ${isMe ? "items-end" : "items-start"}`}>
                {m.imageUrl && (
                  <div className="w-full rounded-2xl overflow-hidden border border-[#e4e6eb] shadow-sm bg-white">
                    <img
                      src={m.imageUrl}
                      alt="Ảnh đính kèm Cloudinary"
                      className="w-full h-auto object-cover max-h-72 cursor-pointer hover:opacity-95 transition-opacity"
                      onClick={() => window.open(m.imageUrl, "_blank")}
                    />
                    <div className="bg-[#f0f2f5] px-3 py-1.5 flex items-center justify-between text-xs text-[#65676b]">
                      <span className="truncate max-w-[180px] font-medium">{m.fileName || "Ảnh đính kèm"}</span>
                      <span className="text-emerald-700 font-bold bg-emerald-100 px-1.5 py-0.5 rounded text-[10px]">
                        Cloudinary
                      </span>
                    </div>
                  </div>
                )}
                {m.text && (
                  <div
                    className={`px-3.5 py-2 rounded-2xl text-[14.5px] leading-relaxed shadow-sm ${
                      isMe
                        ? "bg-[#0084ff] text-white rounded-br-sm"
                        : "bg-[#f0f2f5] text-[#050505] rounded-bl-sm"
                    }`}
                  >
                    {m.text}
                  </div>
                )}
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      {/* THANH NHẬP LIỆU */}
      <div className="p-3 border-t border-[#e4e6eb] bg-white flex items-center gap-2">
        {/* NÚT TẢI ẢNH LÊN CLOUDINARY */}
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={isUploading}
          className={`w-9 h-9 rounded-full flex items-center justify-center transition-colors cursor-pointer ${
            isUploading
              ? "bg-blue-100 text-[#0084ff] animate-pulse"
              : "hover:bg-[#f0f2f5] text-[#0084ff]"
          }`}
          title="Tải ảnh lên Cloudinary thật"
        >
          {isUploading ? (
            <svg className="w-5 h-5 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
          ) : (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z" />
            </svg>
          )}
        </button>

        <div className="flex-1 bg-[#f0f2f5] rounded-full px-4 py-2 flex items-center">
          <input
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") handleSend();
            }}
            placeholder="Aa"
            className="bg-transparent border-none outline-none text-[14.5px] text-[#050505] placeholder-[#65676b] w-full"
          />
        </div>

        {inputText.trim() ? (
          <button
            onClick={handleSend}
            className="w-9 h-9 rounded-full bg-[#0084ff] hover:bg-[#0073e6] text-white flex items-center justify-center transition-colors shadow-sm cursor-pointer"
            title="Gửi"
          >
            <svg className="w-4 h-4 translate-x-0.5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
            </svg>
          </button>
        ) : (
          <button
            onClick={() => onSendMessage("👍")}
            className="w-9 h-9 rounded-full hover:bg-[#f0f2f5] flex items-center justify-center text-[#0084ff] transition-colors cursor-pointer"
            title="Thích"
          >
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
              <path d="M1 21h4V9H1v12zm22-11c0-1.1-.9-2-2-2h-6.31l.95-4.57.03-.32c0-.41-.17-.79-.44-1.06L14.17 1 7.59 7.59C7.22 7.95 7 8.45 7 9v10c0 1.1.9 2 2 2h9c.83 0 1.54-.5 1.84-1.22l3.02-7.05c.09-.23.14-.47.14-.73v-2z" />
            </svg>
          </button>
        )}
      </div>
    </div>
  );
}
