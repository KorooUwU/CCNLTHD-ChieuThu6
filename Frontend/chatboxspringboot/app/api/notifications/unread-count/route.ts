import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../../_lib/auth";

export async function GET(req: NextRequest) {
  try {
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json({ unreadCount: 0 });
    }

    const res = await fetch("http://localhost:8081/api/notifications/unread-count", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    const data = await res.json();
    return NextResponse.json(data, { status: res.status });
  } catch {
    return NextResponse.json({ unreadCount: 0 });
  }
}
