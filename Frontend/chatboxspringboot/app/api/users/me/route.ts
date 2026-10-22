import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../../_lib/auth";

export async function GET(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập" },
        { status: 401 }
      );
    }

    const res = await fetch("http://localhost:8081/api/users/me", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if (!res.ok) {
      return NextResponse.json({ error: "Không tìm thấy hồ sơ người dùng" }, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi lấy thông tin cá nhân" },
      { status: 500 }
    );
  }
}
