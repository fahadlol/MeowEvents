"use client";

import { useState } from "react";
import { ALL_TIERS, MODES, MODE_NAMES } from "@/types";

type Props = { onClose: () => void; onSaved: () => void };

export function AdminAddPlayer({ onClose, onSaved }: Props) {
  const [username, setUsername] = useState("");
  const [region, setRegion] = useState("Middle East");
  const [discordId, setDiscordId] = useState("");
  const [tiers, setTiers] = useState<{ mode: string; tier: string; tester_name: string }[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function addTier() {
    setTiers((t) => [...t, { mode: "sword", tier: "LT1", tester_name: "" }]);
  }

  function updateTier(i: number, field: "mode" | "tier" | "tester_name", value: string) {
    setTiers((t) => {
      const next = [...t];
      next[i] = { ...next[i], [field]: value };
      return next;
    });
  }

  function removeTier(i: number) {
    setTiers((t) => t.filter((_, j) => j !== i));
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    const res = await fetch("/api/admin/players", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.trim(),
        region: region.trim() || "Middle East",
        discord_id: discordId.trim() || undefined,
        tiers: tiers.filter((t) => t.tier && t.tester_name.trim()),
      }),
    });
    const data = await res.json();
    setLoading(false);
    if (res.ok) onSaved();
    else setError(data.error || "Failed");
  }

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/60 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--card)] p-6">
        <h2 className="mb-4 text-lg font-semibold">Add player</h2>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-sm text-[var(--muted)]">Minecraft username</label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--border)] bg-[var(--bg)] px-3 py-2 focus:border-[var(--accent)] focus:outline-none"
              required
            />
          </div>
          <div>
            <label className="block text-sm text-[var(--muted)]">Region</label>
            <input
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              className="mt-1 w-full rounded border border-[var(--border)] bg-[var(--bg)] px-3 py-2 focus:border-[var(--accent)] focus:outline-none"
            />
          </div>
          <div>
            <label className="block text-sm text-[var(--muted)]">Discord user ID (optional, for tier sync)</label>
            <input
              value={discordId}
              onChange={(e) => setDiscordId(e.target.value)}
              placeholder="e.g. 123456789012345678"
              className="mt-1 w-full rounded border border-[var(--border)] bg-[var(--bg)] px-3 py-2 focus:border-[var(--accent)] focus:outline-none"
            />
          </div>
          <div>
            <div className="flex items-center justify-between">
              <label className="block text-sm text-[var(--muted)]">Tiers (optional)</label>
              <button type="button" onClick={addTier} className="text-sm text-[var(--accent)] hover:underline">
                + Add tier
              </button>
            </div>
            {tiers.map((t, i) => (
              <div key={i} className="mt-2 flex flex-wrap items-center gap-2 rounded border border-[var(--border)]/50 bg-black/20 p-2">
                <select
                  value={t.mode}
                  onChange={(e) => updateTier(i, "mode", e.target.value)}
                  className="rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-sm"
                >
                  {MODES.map((m) => (
                    <option key={m} value={m}>{MODE_NAMES[m]}</option>
                  ))}
                </select>
                <select
                  value={t.tier}
                  onChange={(e) => updateTier(i, "tier", e.target.value)}
                  className="rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-sm"
                >
                  {ALL_TIERS.map((x) => (
                    <option key={x} value={x}>{x}</option>
                  ))}
                </select>
                <input
                  value={t.tester_name}
                  onChange={(e) => updateTier(i, "tester_name", e.target.value)}
                  placeholder="Tester name"
                  className="flex-1 min-w-[100px] rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-sm"
                />
                <button type="button" onClick={() => removeTier(i)} className="text-red-400 hover:underline text-sm">
                  Remove
                </button>
              </div>
            ))}
          </div>
          {error && <p className="text-sm text-red-400">{error}</p>}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={loading}
              className="rounded bg-[var(--accent)] px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
            >
              {loading ? "…" : "Add player"}
            </button>
            <button type="button" onClick={onClose} className="rounded border border-[var(--border)] px-4 py-2 text-sm hover:bg-white/5">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
