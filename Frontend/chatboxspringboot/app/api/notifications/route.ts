import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../_lib/auth";

export async function GET(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập để xem thông báo" },
        { status: 401 }
      );
    }

    const res = await fetch("http://localhost:8081/api/notifications", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi tải thông báo" },
      { status: 500 }
    );
  }
}
