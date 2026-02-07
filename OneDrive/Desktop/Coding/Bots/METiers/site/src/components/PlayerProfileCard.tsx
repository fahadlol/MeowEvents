"use client";

import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { motion } from "framer-motion";
import { MODES, MODE_NAMES, tierPoints, tierRankForPeak } from "@/types";
import { ModeIcon } from "./Icons";
import type { PlayerWithTiers } from "@/types";

const MC_AVATAR = "https://mc-heads.net/avatar";
const NAMEMC = "https://namemc.com/profile";

const container = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.05, delayChildren: 0.1 },
  },
};

const item = {
  hidden: { opacity: 0, y: 12 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] },
  },
};

type Props = {
  player: PlayerWithTiers;
  bestTier: string | null;
  overallRank?: number | null;
  totalPlayers?: number | null;
};

export function PlayerProfileCard({ player, bestTier, overallRank, totalPlayers }: Props) {
  const [shareStatus, setShareStatus] = useState<"idle" | "copied" | "shared">("idle");
  const tiers = player.tiers || {};
  const totalPoints = MODES.reduce((sum, slug) => sum + (tiers[slug] ? tierPoints(tiers[slug]!.tier) : 0), 0);

  async function handleShare() {
    if (typeof window === "undefined") return;
    const url = `${window.location.origin}/player/${encodeURIComponent(player.username)}`;
    const title = `${player.username} | METiers`;
    if (typeof navigator !== "undefined" && navigator.share) {
      try {
        await navigator.share({ title, url });
        setShareStatus("shared");
        setTimeout(() => setShareStatus("idle"), 2000);
      } catch {
        copyUrl(url);
      }
    } else {
      copyUrl(url);
    }
  }

  function copyUrl(url: string) {
    if (typeof navigator === "undefined") return;
    navigator.clipboard.writeText(url).then(() => {
      setShareStatus("copied");
      setTimeout(() => setShareStatus("idle"), 2000);
    });
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] }}
      className="mx-auto max-w-2xl px-4 py-12"
    >
      <motion.div
        variants={container}
        initial="hidden"
        animate="visible"
        className="rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-6 shadow-[var(--glow-card)]"
      >
        <div className="flex flex-col items-center text-center">
          <motion.div
            variants={item}
            className="relative h-24 w-24 overflow-hidden rounded-full border-2 border-[var(--border-subtle)] bg-[var(--bg-elevated)] ring-2 ring-[var(--card)]"
            whileHover={{ scale: 1.05 }}
            transition={{ type: "spring", stiffness: 400, damping: 20 }}
          >
            <Image
              src={`${MC_AVATAR}/${encodeURIComponent(player.username)}/128`}
              alt=""
              width={96}
              height={96}
              unoptimized
              className="object-cover"
            />
          </motion.div>
          <motion.h1 variants={item} className="mt-4 text-2xl font-bold text-[var(--text)]">
            {player.username}
          </motion.h1>
          {bestTier && (
            <motion.span
              variants={item}
              className="mt-2 inline-flex items-center gap-1.5 rounded-lg border border-purple-500/40 bg-purple-500/20 px-3 py-1 text-sm font-medium text-purple-300"
            >
              <span className="text-white">◆</span>
              {bestTier} (best)
            </motion.span>
          )}
          <motion.p variants={item} className="mt-2 text-sm text-[var(--text-muted)]">
            {player.region}
          </motion.p>

          {/* POSITION pill - inspo: golden pill with rank + OVERALL (points) */}
          {(overallRank != null && totalPlayers != null) || totalPoints > 0 ? (
            <motion.div
              variants={item}
              className="mt-4 flex w-full max-w-xs flex-col items-center gap-2"
            >
              <span className="text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]">
                Position
              </span>
              <div className="flex w-full items-center justify-center rounded-full bg-gradient-to-r from-amber-500/25 to-amber-600/20 px-4 py-2 ring-1 ring-amber-500/30">
                <span className="text-lg font-bold text-white">
                  {overallRank != null ? `${overallRank}.` : "—"}
                </span>
                <span className="mx-2 text-amber-200/90">·</span>
                <span className="text-sm font-semibold text-white">
                  OVERALL ({totalPoints} points)
                </span>
                {overallRank != null && totalPlayers != null && (
                  <span className="ml-1 text-xs text-amber-200/70">
                    of {totalPlayers}
                  </span>
                )}
              </div>
            </motion.div>
          ) : null}

          <motion.div variants={item} className="mt-4 flex flex-wrap items-center justify-center gap-2">
            <button
              type="button"
              onClick={handleShare}
              className="inline-flex items-center gap-2 rounded-lg border border-[var(--accent)]/40 bg-[var(--accent-soft)] px-4 py-2 text-sm font-medium text-[var(--accent)] transition-colors hover:bg-[var(--accent)]/20"
            >
              {shareStatus === "copied" ? (
                "Copied!"
              ) : shareStatus === "shared" ? (
                "Shared!"
              ) : (
                <>
                  <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
                  </svg>
                  Share profile
                </>
              )}
            </button>
            <a
              href={`${NAMEMC}/${encodeURIComponent(player.username)}`}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg border border-[var(--border-subtle)] bg-[var(--bg-elevated)] px-4 py-2 text-sm font-medium text-[var(--text)] transition-colors duration-200 hover:bg-[var(--card-hover)]"
            >
              <span className="font-mono text-[var(--muted)]">n</span>
              NameMC
              <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          </motion.div>
        </div>

        {/* TIERS - only show modes the player has been tested in */}
        {MODES.some((slug) => tiers[slug]) && (
          <div className="mt-8">
            <motion.h2
              variants={item}
              className="mb-3 text-xs font-medium uppercase tracking-wider text-[var(--text-muted)]"
            >
              Tiers
            </motion.h2>
            <div className="flex flex-wrap justify-center gap-4">
              {MODES.filter((slug) => tiers[slug]).map((slug) => {
                const name = MODE_NAMES[slug];
                const t = tiers[slug]!;
                const points = tierPoints(t.tier);
                const peakTierRaw = t.peak_tier ?? null;
                const peakTier = peakTierRaw && String(peakTierRaw).trim() ? String(peakTierRaw).toUpperCase() : null;
                const currentTier = String(t.tier).toUpperCase();
                const showPeakLost =
                  peakTier &&
                  currentTier &&
                  tierRankForPeak(peakTier) < tierRankForPeak(currentTier);
                const lostDate = new Date(t.last_tested_at).toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" });

                return (
                  <motion.div
                    key={slug}
                    variants={item}
                    className="group relative flex flex-col items-center gap-1.5"
                  >
                    <div className="pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 hidden w-36 -translate-x-1/2 rounded-lg bg-[var(--bg-elevated)]/95 px-3 py-2.5 text-center shadow-xl backdrop-blur-sm ring-1 ring-[var(--border-subtle)] group-hover:block">
                      <div className="text-sm font-bold text-[var(--text)]">
                        {showPeakLost && peakTier ? `Peak ${peakTier}` : t.tier}
                      </div>
                      {showPeakLost && peakTier && (
                        <div className="mt-0.5 text-[10px] text-[var(--text-muted)]">lost {lostDate}</div>
                      )}
                      <div className="mt-1 text-xs font-medium text-[var(--accent)]">{points} points</div>
                      <div className="text-[10px] text-[var(--text-muted)]">{name}</div>
                    </div>
                    <div className="flex h-10 w-10 items-center justify-center rounded-full border-2 border-[var(--border-subtle)] bg-[var(--bg-elevated)] p-1.5 transition-colors group-hover:border-[var(--accent)]/40">
                      <ModeIcon mode={slug} className="h-full w-full object-contain opacity-90" />
                    </div>
                    <span className="rounded-full bg-amber-500/20 px-2 py-0.5 text-[10px] font-semibold text-amber-200 ring-1 ring-amber-500/30">
                      {t.tier}
                      {peakTier && tierRankForPeak(peakTier) < tierRankForPeak(currentTier) && " †"}
                    </span>
                  </motion.div>
                );
              })}
            </div>
          </div>
        )}

        <motion.div variants={item} className="mt-8 flex justify-center">
          <Link
            href="/leaderboards"
            className="text-sm font-medium text-[var(--accent)] transition-colors hover:opacity-90 hover:underline"
          >
            ← Back to leaderboards
          </Link>
        </motion.div>
      </motion.div>
    </motion.div>
  );
}
