import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../_lib/auth";

export async function POST(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập để gửi tin nhắn" },
        { status: 401 }
      );
    }

    const body = await req.json();

    const res = await fetch("http://localhost:8081/api/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi gửi tin nhắn" },
      { status: 500 }
    );
  }
}
