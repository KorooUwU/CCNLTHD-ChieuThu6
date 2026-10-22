import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../_lib/auth";

export async function GET(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập để xem đoạn chat" },
        { status: 401 }
      );
    }

    const res = await fetch("http://localhost:8081/api/conversations/get", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(err, { status: res.status });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi lấy danh sách hội thoại" },
      { status: 500 }
    );
  }
}

export async function POST(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập" },
        { status: 401 }
      );
    }

    const body = await req.json();

    const res = await fetch("http://localhost:8081/api/conversations/create", {
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
      { error: err.message || "Lỗi tạo hội thoại" },
      { status: 500 }
    );
  }
}
