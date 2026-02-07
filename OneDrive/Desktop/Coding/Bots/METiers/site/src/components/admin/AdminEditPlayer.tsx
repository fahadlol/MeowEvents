"use client";

import { useState } from "react";
import { ALL_TIERS, MODES, MODE_NAMES } from "@/types";

type Player = {
  id: string;
  username: string;
  region: string;
  discord_id?: string | null;
  player_tiers: { mode: string; tier: string; last_tested_at: string; tester_name: string }[];
};

type Props = { player: Player; onClose: () => void; onSaved: () => void };

export function AdminEditPlayer({ player, onClose, onSaved }: Props) {
  const [username, setUsername] = useState(player.username);
  const [region, setRegion] = useState(player.region);
  const [discordId, setDiscordId] = useState(player.discord_id ?? "");
  const [tiers, setTiers] = useState<Record<string, { tier: string; tester_name: string }>>(() => {
    const o: Record<string, { tier: string; tester_name: string }> = {};
    (player.player_tiers || []).forEach((t) => {
      o[t.mode] = { tier: t.tier, tester_name: t.tester_name };
    });
    MODES.forEach((m) => {
      if (!o[m]) o[m] = { tier: "", tester_name: "" };
    });
    return o;
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    const res = await fetch(`/api/admin/players/${player.id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.trim(),
        region: region.trim() || "Middle East",
        discord_id: discordId.trim() || null,
      }),
    });
    if (!res.ok) {
      const data = await res.json();
      setError(data.error || "Failed");
      setLoading(false);
      return;
    }
    let err = "";
    for (const mode of MODES) {
      const t = tiers[mode];
      if (!t?.tier || !t.tester_name.trim()) continue;
      const patchRes = await fetch(`/api/admin/players/${player.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tier: { mode, tier: t.tier, tester_name: t.tester_name.trim() },
        }),
      });
      if (!patchRes.ok) {
        const data = await patchRes.json();
        err = data.error || "Failed to update tier";
        break;
      }
    }
    setLoading(false);
    if (err) setError(err);
    else onSaved();
  }

  return (
    <div className="fixed inset-0 z-10 flex items-center justify-center bg-black/60 p-4">
      <div className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-lg border border-[var(--border)] bg-[var(--card)] p-6">
        <h2 className="mb-4 text-lg font-semibold">Edit player</h2>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-sm text-[var(--muted)]">Username</label>
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
            <label className="block text-sm text-[var(--muted)]">Tiers per mode</label>
            <p className="mt-1 text-xs text-[var(--muted)]">Set tier + tester to update. Timestamp updates automatically.</p>
            {MODES.map((mode) => (
              <div key={mode} className="mt-2 flex flex-wrap items-center gap-2 rounded border border-[var(--border)]/50 bg-black/20 p-2">
                <span className="w-16 text-sm text-[var(--muted)]">{MODE_NAMES[mode]}</span>
                <select
                  value={tiers[mode]?.tier ?? ""}
                  onChange={(e) =>
                    setTiers((prev) => ({
                      ...prev,
                      [mode]: { ...prev[mode], tier: e.target.value },
                    }))
                  }
                  className="rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-sm"
                >
                  <option value="">—</option>
                  {ALL_TIERS.map((x) => (
                    <option key={x} value={x}>{x}</option>
                  ))}
                </select>
                <input
                  value={tiers[mode]?.tester_name ?? ""}
                  onChange={(e) =>
                    setTiers((prev) => ({
                      ...prev,
                      [mode]: { ...prev[mode], tester_name: e.target.value },
                    }))
                  }
                  placeholder="Tester name"
                  className="flex-1 min-w-[100px] rounded border border-[var(--border)] bg-[var(--bg)] px-2 py-1 text-sm"
                />
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
              {loading ? "…" : "Save"}
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
