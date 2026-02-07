import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { randomBytes, timingSafeEqual } from "crypto";

const ADMIN_PASSWORD = process.env.ADMIN_PASSWORD;
const COOKIE_NAME = "metiers_admin";

// In-memory rate limiter for login attempts
const loginAttempts = new Map<string, { count: number; resetAt: number }>();
const MAX_ATTEMPTS = 5;
const WINDOW_MS = 15 * 60 * 1000; // 15 minutes

function isRateLimited(ip: string): boolean {
  const now = Date.now();
  const entry = loginAttempts.get(ip);
  if (!entry || now > entry.resetAt) {
    loginAttempts.set(ip, { count: 1, resetAt: now + WINDOW_MS });
    return false;
  }
  entry.count++;
  return entry.count > MAX_ATTEMPTS;
}

// Store valid session tokens in memory (cleared on restart)
const validTokens = new Set<string>();

export function isValidToken(token: string): boolean {
  return validTokens.has(token);
}

export async function POST(req: Request) {
  const ip = req.headers.get("x-forwarded-for") || req.headers.get("x-real-ip") || "unknown";
  if (isRateLimited(ip)) {
    return NextResponse.json({ ok: false, error: "Too many login attempts. Try again later." }, { status: 429 });
  }

  let password: string | undefined;
  try {
    const body = await req.json();
    password = body.password;
  } catch {
    return NextResponse.json({ ok: false }, { status: 400 });
  }

  if (!ADMIN_PASSWORD || !password || typeof password !== "string") {
    return NextResponse.json({ ok: false }, { status: 401 });
  }

  // Timing-safe comparison
  const expected = Buffer.from(ADMIN_PASSWORD);
  const received = Buffer.from(password);
  if (expected.length !== received.length || !timingSafeEqual(expected, received)) {
    return NextResponse.json({ ok: false }, { status: 401 });
  }

  // Generate a cryptographically random session token
  const token = randomBytes(32).toString("hex");
  validTokens.add(token);

  const cookieStore = await cookies();
  cookieStore.set(COOKIE_NAME, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    maxAge: 60 * 60 * 24 * 7, // 7 days
    path: "/",
  });
  return NextResponse.json({ ok: true });
}
