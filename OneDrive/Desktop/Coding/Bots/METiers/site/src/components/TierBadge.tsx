"use client";

import { motion } from "framer-motion";
import { tierToBand, tierRankForPeak } from "@/types";

type Props = { tier: string; peakTier?: string | null; showPeakLabel?: boolean };

const BAND_STYLES: Record<number, string> = {
  1: "bg-amber-500/15 text-amber-400 border-amber-500/30 shadow-[0_0_12px_rgba(245,158,11,0.15)]",
  2: "bg-violet-500/15 text-violet-400 border-violet-500/30",
  3: "bg-orange-500/15 text-orange-400 border-orange-500/30",
  4: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
  5: "bg-slate-500/15 text-slate-400 border-slate-500/30",
};

export function TierBadge({ tier, peakTier, showPeakLabel }: Props) {
  const band = tierToBand(tier);
  const style = BAND_STYLES[band] ?? BAND_STYLES[5];
  const showPeak =
    peakTier &&
    tierRankForPeak(peakTier) < tierRankForPeak(tier);
  const title = showPeak ? `Peak: ${peakTier}` : undefined;

  return (
    <span className="inline-flex flex-col items-center gap-0.5">
      <motion.span
        title={title}
        initial={{ opacity: 0, scale: 0.85 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ type: "spring", stiffness: 400, damping: 20 }}
        whileHover={{ scale: 1.08 }}
        whileTap={{ scale: 0.95 }}
        className={`inline-flex items-center justify-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors ${style}`}
      >
        {tier}
        {showPeak && (
          <span className="ml-1 opacity-70" aria-hidden>†</span>
        )}
      </motion.span>
      {showPeak && showPeakLabel && (
        <span className="text-[10px] text-[var(--text-muted)]" title={title}>
          Peak: {peakTier}
        </span>
      )}
    </span>
  );
}
