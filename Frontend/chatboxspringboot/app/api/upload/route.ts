import { NextRequest, NextResponse } from "next/server";
import { getAuthToken } from "../_lib/auth";

export async function POST(req: NextRequest) {
  try {
    const formData = await req.formData();
    const file = formData.get("file");

    if (!file) {
      return NextResponse.json(
        { error: "Vui lòng chọn file cần tải lên" },
        { status: 400 }
      );
    }

    const clientAuth = req.headers.get("authorization");
    const token = clientAuth ? clientAuth.replace("Bearer ", "") : await getAuthToken();

    if (!token) {
      return NextResponse.json(
        { error: "Vui lòng đăng nhập để upload ảnh lên Cloudinary" },
        { status: 401 }
      );
    }

    const backendFormData = new FormData();
    backendFormData.append("file", file);

    const uploadRes = await fetch("http://localhost:8081/api/files/upload", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: backendFormData,
    });

    const uploadResult = await uploadRes.json();
    return NextResponse.json(uploadResult, { status: uploadRes.status });
  } catch (err: any) {
    return NextResponse.json(
      { error: err.message || "Lỗi xử lý tải lên file" },
      { status: 500 }
    );
  }
}
