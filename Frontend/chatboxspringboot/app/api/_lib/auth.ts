export async function getAuthToken(): Promise<string | null> {
  try {
    let res = await fetch("http://localhost:8081/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        userName: "khaikiet_cloud",
        password: "password123",
      }),
      cache: "no-store",
    });

    if (res.status === 401) {
      // Đăng ký tài khoản nếu chưa có trong DB Supabase
      await fetch("http://localhost:8081/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tenTaiKhoan: "Khải Kiệt",
          email: "khaikiet_cloud@gmail.com",
          userName: "khaikiet_cloud",
          password: "password123",
        }),
      });

      res = await fetch("http://localhost:8081/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userName: "khaikiet_cloud",
          password: "password123",
        }),
      });
    }

    if (!res.ok) return null;
    const data = await res.json();
    return data.accessToken || null;
  } catch {
    return null;
  }
}
