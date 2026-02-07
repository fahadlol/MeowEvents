import { cookies } from "next/headers";

const COOKIE_NAME = "metiers_admin";

export async function isAdmin(): Promise<boolean> {
  if (!process.env.ADMIN_PASSWORD) return false;
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_NAME)?.value;
  if (!token || token.length !== 64) return false;
  // Dynamically import to access the in-memory token set from the login route
  const { isValidToken } = await import("@/app/api/admin/login/route");
  return isValidToken(token);
}
