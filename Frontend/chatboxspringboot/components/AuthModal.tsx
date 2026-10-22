"use client";

import React, { useState } from "react";
import { loginUser, registerUser, fetchCurrentUser } from "@/lib/api";
import { CurrentUser } from "@/types/chat";

interface AuthModalProps {
  onSuccess: (user: CurrentUser) => void;
}

export default function AuthModal({ onSuccess }: AuthModalProps) {
  const [isRegister, setIsRegister] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const [tenTaiKhoan, setTenTaiKhoan] = useState("");
  const [email, setEmail] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        if (!tenTaiKhoan.trim() || !email.trim() || !userName.trim() || !password.trim()) {
          throw new Error("Vui lòng điền đầy đủ tất cả các trường.");
        }
        if (password.length < 6) {
          throw new Error("Mật khẩu phải từ 6 ký tự trở lên.");
        }
        // Gọi API đăng ký
        await registerUser(tenTaiKhoan.trim(), email.trim(), userName.trim(), password);
        // Tự động đăng nhập ngay sau khi đăng ký thành công
        await loginUser(userName.trim(), password);
      } else {
        if (!userName.trim() || !password.trim()) {
          throw new Error("Vui lòng nhập tên đăng nhập và mật khẩu.");
        }
        // Gọi API đăng nhập
        await loginUser(userName.trim(), password);
      }

      // Lấy thông tin user vừa đăng nhập
      const user = await fetchCurrentUser();
      if (user) {
        onSuccess(user);
      } else {
        throw new Error("Không thể tải thông tin hồ sơ sau khi đăng nhập.");
      }
    } catch (err: any) {
      setError(err.message || "Đã có lỗi xảy ra");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 select-none">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-2xl border border-[#e4e6eb] overflow-hidden animate-in fade-in zoom-in-95 duration-200">
        {/* HEADER MODAL */}
        <div className="bg-gradient-to-r from-[#0084ff] to-[#0064d1] px-6 py-6 text-white text-center">
          <div className="w-14 h-14 bg-white/20 rounded-full flex items-center justify-center mx-auto mb-3 shadow-inner">
            <svg className="w-8 h-8 text-white" fill="currentColor" viewBox="0 0 24 24">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold tracking-tight">
            {isRegister ? "Tạo tài khoản mới" : "Đăng nhập Messenger"}
          </h2>
          <p className="text-xs text-white/80 mt-1">
            Kết nối trực tiếp CSDL Supabase PostgreSQL & Cloudinary
          </p>
        </div>

        {/* THÔNG BÁO LỖI NẾU CÓ */}
        {error && (
          <div className="mx-6 mt-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs font-medium flex items-center gap-2">
            <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
            </svg>
            <span>{error}</span>
          </div>
        )}

        {/* FORM ĐĂNG NHẬP / ĐĂNG KÝ */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {isRegister && (
            <>
              <div>
                <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
                  Tên hiển thị
                </label>
                <input
                  type="text"
                  value={tenTaiKhoan}
                  onChange={(e) => setTenTaiKhoan(e.target.value)}
                  placeholder="Ví dụ: Khải Kiệt"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505] transition-all"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
                  Email
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505] transition-all"
                  required
                />
              </div>
            </>
          )}

          <div>
            <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
              Tên đăng nhập (Username)
            </label>
            <input
              type="text"
              value={userName}
              onChange={(e) => setUserName(e.target.value)}
              placeholder="Ví dụ: khaikiet"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505] transition-all"
              required
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-[#65676b] uppercase mb-1">
              Mật khẩu
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full px-3.5 py-2.5 rounded-xl border border-[#ced0d4] focus:border-[#0084ff] focus:ring-2 focus:ring-[#0084ff]/20 outline-none text-sm text-[#050505] transition-all"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-[#0084ff] hover:bg-[#0073e6] disabled:opacity-60 text-white font-bold rounded-xl shadow-md transition-colors text-sm flex items-center justify-center gap-2 cursor-pointer mt-2"
          >
            {loading ? (
              <>
                <svg className="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                </svg>
                <span>Đang xử lý...</span>
              </>
            ) : isRegister ? (
              "Đăng ký tài khoản"
            ) : (
              "Đăng nhập"
            )}
          </button>
        </form>

        {/* FOOTER CHUYỂN ĐỔI GIỮA ĐĂNG NHẬP / ĐĂNG KÝ */}
        <div className="bg-[#f0f2f5] px-6 py-4 text-center text-xs text-[#65676b] border-t border-[#e4e6eb]">
          {isRegister ? (
            <p>
              Đã có tài khoản?{" "}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(false);
                  setError(null);
                }}
                className="text-[#0084ff] font-bold hover:underline cursor-pointer ml-1"
              >
                Đăng nhập ngay
              </button>
            </p>
          ) : (
            <p>
              Chưa có tài khoản?{" "}
              <button
                type="button"
                onClick={() => {
                  setIsRegister(true);
                  setError(null);
                }}
                className="text-[#0084ff] font-bold hover:underline cursor-pointer ml-1"
              >
                Đăng ký tài khoản mới
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
