"use client";

import { motion } from "framer-motion";
import { LeaderboardModeCards } from "@/components/LeaderboardModeCards";
import type { ModeSlug } from "@/types";

const container = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.06, delayChildren: 0.05 },
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
  modes: readonly ModeSlug[];
  modeNames: Record<ModeSlug, string>;
};

export function LeaderboardsPageContent({ modes, modeNames }: Props) {
  return (
    <motion.div
      variants={container}
      initial="hidden"
      animate="visible"
      className="mx-auto max-w-4xl px-4 py-12"
    >
      <motion.h1
        variants={item}
        className="mb-2 text-2xl font-bold text-[var(--text)] md:text-3xl"
      >
        Rankings
      </motion.h1>
      <motion.p
        variants={item}
        className="mb-8 text-[var(--text-muted)]"
      >
        Each PvP mode has its own tier. Pick a mode to see tier columns.
      </motion.p>
      <motion.div variants={item}>
        <LeaderboardModeCards modes={modes} modeNames={modeNames} />
      </motion.div>
    </motion.div>
  );
}
