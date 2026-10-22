import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../../_lib/auth";

export async function GET(req: NextRequest) {
  try {
    const { searchParams } = new URL(req.url);
    const keyword = searchParams.get("keyword") || "";

    if (!keyword.trim()) {
      return NextResponse.json({ content: [], totalElements: 0 });
    }

    const token = await getAuthToken();
    if (!token) {
      return NextResponse.json(
        { error: "Backend Spring Boot chưa sẵn sàng" },
        { status: 503 }
      );
    }

    const res = await fetch(
      `http://localhost:8081/api/users/search?keyword=${encodeURIComponent(
        keyword
      )}&page=0&size=20`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
        cache: "no-store",
      }
    );

    if (!res.ok) {
      return NextResponse.json({ content: [], totalElements: 0 });
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi tìm kiếm người dùng" },
      { status: 500 }
    );
  }
}
