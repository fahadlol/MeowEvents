"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { motion } from "framer-motion";
import { TIERS } from "@/types";
import type { ModeSlug } from "@/types";

type Props = { mode: ModeSlug; currentTier?: string };

export function LeaderboardFilters({ mode, currentTier }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();

  function setFilter(key: string, value: string | null) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    router.push(`/mode/${mode}?${next.toString()}`);
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.25, 0.46, 0.45, 0.94] }}
      className="flex flex-wrap items-center gap-4 rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-4 shadow-[var(--glow-card)]"
    >
      <div className="flex items-center gap-2">
        <span className="text-sm text-[var(--text-muted)]">Tier</span>
        <select
          value={currentTier ?? ""}
          onChange={(e) => setFilter("tier", e.target.value || null)}
          className="rounded-lg border border-[var(--border-subtle)] bg-[var(--bg-elevated)] px-3 py-1.5 text-sm text-[var(--text)] transition focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)]"
        >
          <option value="">All</option>
          {TIERS.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
      </div>
    </motion.div>
  );
}
