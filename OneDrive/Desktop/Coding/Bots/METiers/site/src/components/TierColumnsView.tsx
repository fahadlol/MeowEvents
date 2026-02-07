"use client";

import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import { TIER_BANDS, tierToBand } from "@/types";
import type { LeaderboardRow } from "@/lib/data";

const MC_AVATAR = "https://mc-heads.net/avatar";

const BAND_COLORS: Record<number, string> = {
  1: "border-amber-500/40 bg-amber-500/10 text-amber-400",
  2: "border-violet-500/40 bg-violet-500/10 text-violet-400",
  3: "border-orange-500/40 bg-orange-500/10 text-orange-400",
  4: "border-emerald-500/40 bg-emerald-500/10 text-emerald-400",
  5: "border-slate-500/40 bg-slate-500/10 text-slate-400",
};

type Props = {
  rows: LeaderboardRow[];
};

export function TierColumnsView({ rows }: Props) {
  const byBand: Record<number, LeaderboardRow[]> = { 1: [], 2: [], 3: [], 4: [], 5: [] };
  rows.forEach((row) => {
    const band = tierToBand(row.tier);
    byBand[band].push(row);
  });

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {TIER_BANDS.map(({ band, label }) => {
        const list = byBand[band];
        const colorClass = BAND_COLORS[band];
        return (
          <motion.div
            key={band}
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: band * 0.06, duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] }}
            whileHover={{ y: -2, boxShadow: "0 8px 32px rgba(0,0,0,0.4)" }}
            className="flex min-h-0 flex-col rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] shadow-[var(--glow-card)] transition-shadow duration-200"
          >
            <div
              className={`flex items-center gap-2 rounded-t-xl border-b border-[var(--border-subtle)] px-4 py-3 ${colorClass}`}
            >
              <TrophyIcon className="h-4 w-4 shrink-0" />
              <span className="font-semibold">{label}</span>
              <span className="ml-auto text-xs opacity-80">{list.length}</span>
            </div>
            <div className="flex-1 overflow-y-auto p-2" style={{ maxHeight: "min(60vh, 480px)" }}>
              {list.length === 0 ? (
                <p className="py-6 text-center text-sm text-[var(--text-muted)]">No players</p>
              ) : (
                <ul className="space-y-1">
                  {list.map((row, i) => (
                    <motion.li
                      key={row.id}
                      initial={{ opacity: 0, x: -4 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.02, duration: 0.25 }}
                    >
                      <Link
                        href={`/player/${encodeURIComponent(row.username)}`}
                        className="group flex items-center gap-3 rounded-lg px-2 py-2 transition-colors duration-200 hover:bg-[var(--card-hover)]"
                      >
                        <span className="relative h-8 w-8 shrink-0 overflow-hidden rounded-lg border border-[var(--border-subtle)] bg-[var(--bg)]">
                          <Image
                            src={`${MC_AVATAR}/${encodeURIComponent(row.username)}/32`}
                            alt=""
                            width={32}
                            height={32}
                            unoptimized
                            className="object-cover"
                          />
                        </span>
                        <span className="min-w-0 flex-1 truncate font-medium text-[var(--text)] hover:text-[var(--accent)]">
                          {row.username}
                        </span>
                        <span className="shrink-0 text-[var(--text-muted)] transition-transform group-hover:translate-y-[-1px]">
                          <ArrowUpIcon className="h-3.5 w-3.5" />
                        </span>
                      </Link>
                    </motion.li>
                  ))}
                </ul>
              )}
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}

function TrophyIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24">
      <path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H8v2h8v-2h-3v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z" />
    </svg>
  );
}

function ArrowUpIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M5 10l7-7m0 0l7 7m-7-7v18" />
    </svg>
  );
}
