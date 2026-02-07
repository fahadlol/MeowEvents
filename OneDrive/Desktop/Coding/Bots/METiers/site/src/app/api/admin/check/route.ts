import { NextResponse } from "next/server";
import { cookies } from "next/headers";

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
const COOKIE_NAME = "metiers_admin";

export async function GET() {
  if (!ADMIN_PASSWORD) {
    return NextResponse.json({ ok: false }, { status: 500 });
  }
  const cookieStore = await cookies();
  const token = cookieStore.get(COOKIE_NAME)?.value;
  return NextResponse.json({ ok: token === "1" });
}
