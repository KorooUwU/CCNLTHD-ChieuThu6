import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../../../_lib/auth";

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const { id } = await params;
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập để xem tin nhắn" },
        { status: 401 }
      );
    }

    const res = await fetch(`http://localhost:8081/api/conversations/${id}/messages`, {
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
      { error: err.message || "Lỗi lấy tin nhắn cuộc hội thoại" },
      { status: 500 }
    );
  }
}
