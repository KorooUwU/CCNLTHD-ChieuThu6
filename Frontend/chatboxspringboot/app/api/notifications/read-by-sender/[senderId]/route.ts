import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../../../_lib/auth";

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<{ senderId: string }> }
) {
  try {
    const { senderId } = await params;
    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const res = await fetch(`http://localhost:8081/api/notifications/read-by-sender/${senderId}`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    return new NextResponse(null, { status: res.status });
  } catch (err: any) {
    return NextResponse.json({ error: err.message }, { status: 500 });
  }
}
