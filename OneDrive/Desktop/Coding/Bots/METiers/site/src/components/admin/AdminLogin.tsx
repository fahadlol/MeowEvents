"use client";

import { useState } from "react";

type Props = { onSuccess: () => void };

export function AdminLogin({ onSuccess }: Props) {
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    const res = await fetch("/api/admin/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ password }),
    });
    const data = await res.json();
    setLoading(false);
    if (data.ok) onSuccess();
    else setError("Invalid password.");
  }

  return (
    <div className="mx-auto max-w-sm px-4 py-16">
      <h1 className="mb-6 text-xl font-semibold">Admin login</h1>
      <form onSubmit={submit} className="space-y-4">
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Password"
          className="w-full rounded border border-[var(--border)] bg-[var(--card)] px-4 py-2 focus:border-[var(--accent)] focus:outline-none"
          autoFocus
        />
        {error && <p className="text-sm text-red-400">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded bg-[var(--accent)] py-2 font-medium text-white hover:opacity-90 disabled:opacity-50"
        >
          {loading ? "…" : "Log in"}
        </button>
      </form>
    </div>
  );
}
