"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ModeIcon } from "./Icons";
import type { ModeSlug } from "@/types";

type Props = {
  modes: readonly ModeSlug[];
  modeNames: Record<ModeSlug, string>;
};

const cardVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.08, duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] },
  }),
};

export function LeaderboardModeCards({ modes, modeNames }: Props) {
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      <motion.div custom={0} variants={cardVariants} initial="hidden" animate="visible">
        <motion.div whileHover={{ y: -4 }} whileTap={{ scale: 0.99 }} transition={{ type: "spring", stiffness: 400, damping: 25 }}>
          <Link
            href="/leaderboards"
            className="group block rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-6 shadow-[var(--glow-card)] transition-all duration-300 hover:border-[var(--accent)]/40 hover:bg-[var(--card-hover)] hover:shadow-[0_8px_32px_rgba(0,0,0,0.4),0_0_0_1px_var(--border-subtle)]"
          >
            <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-amber-500/20 text-amber-400">
              <span className="text-xl font-bold">Σ</span>
            </div>
            <h2 className="text-lg font-semibold text-[var(--text)] transition group-hover:text-[var(--accent)]">
              Overall
            </h2>
            <p className="mt-1 text-sm text-[var(--text-muted)] transition group-hover:text-[var(--text)]">
              Points across all modes (top 100) →
            </p>
          </Link>
        </motion.div>
      </motion.div>
      {modes.map((slug, i) => (
        <motion.div
          key={slug}
          custom={i + 1}
          variants={cardVariants}
          initial="hidden"
          animate="visible"
        >
          <motion.div whileHover={{ y: -4 }} whileTap={{ scale: 0.99 }} transition={{ type: "spring", stiffness: 400, damping: 25 }}>
          <Link
            href={`/mode/${slug}`}
            className="group block rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-6 shadow-[var(--glow-card)] transition-all duration-300 hover:border-[var(--accent)]/40 hover:bg-[var(--card-hover)] hover:shadow-[0_8px_32px_rgba(0,0,0,0.4),0_0_0_1px_var(--border-subtle)]"
          >
            <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-lg bg-[var(--bg-elevated)]">
              <ModeIcon mode={slug} className="h-6 w-6 object-contain" />
            </div>
            <h2 className="text-lg font-semibold text-[var(--text)] transition group-hover:text-[var(--accent)]">
              {modeNames[slug]}
            </h2>
            <p className="mt-1 text-sm text-[var(--text-muted)] transition group-hover:text-[var(--text)]">
              View {modeNames[slug]} rankings →
            </p>
          </Link>
          </motion.div>
        </motion.div>
      ))}
    </div>
  );
}
