"use client";

import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import { TierBadge } from "./TierBadge";
import type { LeaderboardRow } from "@/lib/data";

const MC_AVATAR = "https://mc-heads.net/avatar";

type Props = {
  rows: LeaderboardRow[];
  mode: string;
};

const rowVariants = {
  hidden: { opacity: 0, x: -12 },
  visible: (i: number) => ({
    opacity: 1,
    x: 0,
    transition: { delay: i * 0.035, duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] },
  }),
};

function RegionBadge({ region }: { region: string }) {
  const short = region === "Middle East" ? "ME" : region.slice(0, 2).toUpperCase();
  return (
    <span className="inline-flex rounded-full border border-[var(--border-subtle)] bg-[var(--bg-elevated)] px-2 py-0.5 text-xs font-medium text-[var(--text-muted)]">
      {short}
    </span>
  );
}

export function LeaderboardTable({ rows, mode }: Props) {
  if (rows.length === 0) {
    return (
      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-8 text-center text-[var(--text-muted)] shadow-[var(--glow-card)]"
      >
        No players ranked in this mode yet.
      </motion.p>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.25 }}
      className="overflow-hidden rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] shadow-[var(--glow-card)]"
    >
      <table className="w-full text-left">
        <thead>
          <tr className="border-b border-[var(--border-subtle)] bg-[var(--bg-elevated)]">
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">#</th>
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">Players</th>
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">Region</th>
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">Tier</th>
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">Last tested</th>
            <th className="px-4 py-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">Tester</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <motion.tr
              key={row.id}
              custom={i}
              variants={rowVariants}
              initial="hidden"
              animate="visible"
              className="border-b border-[var(--border-subtle)] transition-colors duration-200 hover:bg-[var(--card-hover)]"
            >
              <td className="px-4 py-3 text-[var(--text-muted)]">{i + 1}</td>
              <td className="px-4 py-3">
                <Link
                  href={`/player/${encodeURIComponent(row.username)}`}
                  className="flex items-center gap-3 transition hover:opacity-90"
                >
                  <motion.span
                        className="relative block h-8 w-8 shrink-0 overflow-hidden rounded-lg border border-[var(--border)] bg-[var(--bg)]"
                        whileHover={{ scale: 1.08 }}
                        transition={{ type: "spring", stiffness: 400, damping: 20 }}
                      >
                        <Image
                      src={`${MC_AVATAR}/${encodeURIComponent(row.username)}/32`}
                      alt=""
                      width={32}
                      height={32}
                      unoptimized
                      className="object-cover"
                          />
                        </motion.span>
                  <span className="font-medium text-[var(--accent)] hover:underline">{row.username}</span>
                </Link>
              </td>
              <td className="px-4 py-3">
                <RegionBadge region={row.region} />
              </td>
              <td className="px-4 py-3">
                <TierBadge tier={row.tier} peakTier={row.peak_tier} />
              </td>
              <td className="px-4 py-3 text-[var(--text-muted)]">
                {new Date(row.last_tested_at).toLocaleDateString()}
              </td>
              <td className="px-4 py-3 text-[var(--text-muted)]">{row.tester_name}</td>
            </motion.tr>
          ))}
        </tbody>
      </table>
    </motion.div>
  );
}
