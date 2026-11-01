"use client";

import React, { useState } from "react";
import { searchUsers, createGroupConversation } from "@/lib/api";
import { UserSummary } from "@/types/chat";

interface CreateGroupModalProps {
  onClose: () => void;
  onSuccess: (newGroup: any) => void;
  currentUserId?: string;
}

export default function CreateGroupModal({
  onClose,
  onSuccess,
  currentUserId,
}: CreateGroupModalProps) {
  const [groupName, setGroupName] = useState("");
  const [keyword, setKeyword] = useState("");
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<UserSummary[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async (val: string) => {
    setKeyword(val);
    if (!val.trim()) {
      setSearchResults([]);
      return;
    }
    setIsSearching(true);
    try {
      const results = await searchUsers(val.trim());
      // Lọc bỏ chính mình
      const filtered = results.filter((u) => u.id !== currentUserId);
      setSearchResults(filtered);
    } catch (err: any) {
      console.error("Lỗi tìm kiếm:", err);
    } finally {
      setIsSearching(false);
    }
  };

  const toggleUser = (user: UserSummary) => {
    if (selectedUsers.some((u) => u.id === user.id)) {
      setSelectedUsers(selectedUsers.filter((u) => u.id !== user.id));
    } else {
      setSelectedUsers([...selectedUsers, user]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupName.trim()) {
      setError("Vui lòng nhập tên nhóm chat.");
      return;
    }
    if (selectedUsers.length === 0) {
      setError("Vui lòng chọn ít nhất 1 thành viên tham gia nhóm.");
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      const memberIds = selectedUsers.map((u) => u.id);
      const res = await createGroupConversation(groupName.trim(), memberIds);
      onSuccess(res);
      onClose();
    } catch (err: any) {
      setError(err.message || "Không thể tạo nhóm trò chuyện.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 select-none animate-in fade-in duration-150">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-[#e4e6eb] overflow-hidden flex flex-col max-h-[90vh]">
        {/* HEADER */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-[#e4e6eb] bg-gradient-to-r from-[#0084ff] to-[#0064d1] text-white">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center">
              <svg className="w-5 h-5 text-white" fill="currentColor" viewBox="0 0 24 24">
                <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
              </svg>
            </div>
            <h2 className="text-lg font-bold">Tạo nhóm trò chuyện</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full flex items-center justify-center text-white/80 hover:text-white hover:bg-white/10 transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* BODY */}
        <form onSubmit={handleSubmit} className="p-6 flex-1 overflow-y-auto space-y-4">
          {error && (
            <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium">
              {error}
            </div>
          )}

          {/* TÊN NHÓM */}
          <div>
            <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
              Tên nhóm chat <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              value={groupName}
              onChange={(e) => setGroupName(e.target.value)}
              placeholder="Ví dụ: Team CCNLTHD Chiều Thứ 6"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505]"
              required
            />
          </div>

          {/* CHIPS THÀNH VIÊN ĐÃ CHỌN */}
          {selectedUsers.length > 0 && (
            <div>
              <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1.5">
                Đã chọn ({selectedUsers.length} thành viên)
              </label>
              <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto p-1.5 bg-[#f0f2f5] rounded-xl">
                {selectedUsers.map((user) => (
                  <span
                    key={user.id}
                    className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-white rounded-full text-xs font-medium text-[#050505] shadow-xs border border-[#ced0d4]"
                  >
                    <span>{user.tenTaiKhoan}</span>
                    <button
                      type="button"
                      onClick={() => toggleUser(user)}
                      className="text-[#65676b] hover:text-rose-600 cursor-pointer text-xs"
                    >
                      ✕
                    </button>
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* TÌM KIẾM THÀNH VIÊN */}
          <div>
            <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
              Tìm thành viên trên Supabase
            </label>
            <div className="relative">
              <input
                type="text"
                value={keyword}
                onChange={(e) => handleSearch(e.target.value)}
                placeholder="Nhập tên hiển thị hoặc username..."
                className="w-full pl-9 pr-3.5 py-2 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505]"
              />
              <svg
                className="w-4 h-4 text-[#65676b] absolute left-3 top-3"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
            </div>
          </div>

          {/* DANH SÁCH KẾT QUẢ TÌM KIẾM */}
          <div className="border border-[#e4e6eb] rounded-xl overflow-hidden max-h-48 overflow-y-auto">
            {isSearching ? (
              <div className="p-4 text-center text-xs text-[#65676b]">Đang tìm kiếm...</div>
            ) : searchResults.length > 0 ? (
              <div className="divide-y divide-[#f0f2f5]">
                {searchResults.map((user) => {
                  const isSelected = selectedUsers.some((u) => u.id === user.id);
                  return (
                    <button
                      type="button"
                      key={user.id}
                      onClick={() => toggleUser(user)}
                      className={`w-full flex items-center justify-between p-2.5 text-left transition-colors cursor-pointer ${
                        isSelected ? "bg-[#e7f3ff]" : "hover:bg-[#f0f2f5]"
                      }`}
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <div className="w-8 h-8 rounded-full bg-[#0084ff] text-white flex items-center justify-center font-bold text-xs shrink-0">
                          {user.tenTaiKhoan?.charAt(0) || "U"}
                        </div>
                        <div className="truncate">
                          <p className="text-sm font-semibold text-[#050505] truncate">
                            {user.tenTaiKhoan}
                          </p>
                          <p className="text-xs text-[#65676b] truncate">@{user.userName}</p>
                        </div>
                      </div>
                      <div
                        className={`w-5 h-5 rounded-md flex items-center justify-center border transition-colors shrink-0 ${
                          isSelected
                            ? "bg-[#0084ff] border-[#0084ff] text-white"
                            : "border-[#ced0d4] bg-white"
                        }`}
                      >
                        {isSelected && "✓"}
                      </div>
                    </button>
                  );
                })}
              </div>
            ) : keyword ? (
              <div className="p-4 text-center text-xs text-[#65676b]">
                Không tìm thấy người dùng phù hợp.
              </div>
            ) : (
              <div className="p-4 text-center text-xs text-[#65676b]">
                Gõ từ khóa để tìm người dùng thêm vào nhóm.
              </div>
            )}
          </div>

          {/* ACTIONS */}
          <div className="flex items-center justify-end gap-2 pt-2 border-t border-[#e4e6eb]">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl text-sm font-medium text-[#65676b] hover:bg-[#f0f2f5] transition-colors cursor-pointer"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting || selectedUsers.length === 0 || !groupName.trim()}
              className="px-5 py-2 rounded-xl text-sm font-bold bg-[#0084ff] hover:bg-[#0073e6] disabled:opacity-50 text-white transition-colors flex items-center gap-2 cursor-pointer shadow-sm"
            >
              {isSubmitting ? "Đang tạo..." : "Tạo nhóm"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
