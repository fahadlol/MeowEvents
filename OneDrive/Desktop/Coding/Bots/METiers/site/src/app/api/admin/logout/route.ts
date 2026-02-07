import { NextResponse } from "next/server";
import { cookies } from "next/headers";

export async function POST() {
  const cookieStore = await cookies();
  cookieStore.delete("metiers_admin");
  return NextResponse.json({ ok: true });
}
