"use client";

import { motion } from "framer-motion";
import { MODE_NAMES } from "@/types";

const sectionVariants = {
  hidden: { opacity: 0, y: 12 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { delay: i * 0.08, duration: 0.35 },
  }),
};

const MODE_LIST = Object.values(MODE_NAMES).join(", ");

export function RulesContent() {
  return (
    <>
      <motion.section
        custom={0}
        variants={sectionVariants}
        initial="hidden"
        animate="visible"
        className="mb-8 rounded-xl border border-[var(--border)]/50 bg-[var(--card)]/50 p-6"
      >
        <h2 className="mb-3 text-lg font-semibold">Tier system</h2>
        <p className="mb-2 text-[var(--muted)]">
          Ranks run from best to worst: <strong className="text-[var(--text)]">HT1, LT1, HT2, LT2, HT3, LT3, HT4, LT4, HT5, LT5</strong>.
        </p>
        <ul className="list-inside list-disc space-y-1 text-[var(--muted)]">
          <li>
            <strong className="text-emerald-400">HT1 – HT5</strong> — High Tier
          </li>
          <li>
            <strong className="text-red-400">LT1 – LT5</strong> — Low Tier
          </li>
        </ul>
        <p className="mt-3 text-[var(--muted)]">
          Tiers are whole levels only — no decimals, no ELO. Simple and transparent.
        </p>
      </motion.section>

      <motion.section
        custom={1}
        variants={sectionVariants}
        initial="hidden"
        animate="visible"
        className="mb-8 rounded-xl border border-[var(--border)]/50 bg-[var(--card)]/50 p-6"
      >
        <h2 className="mb-3 text-lg font-semibold">How to get ranked</h2>
        <ol className="list-inside list-decimal space-y-2 text-[var(--muted)]">
          <li>Join our Discord server (link in the nav).</li>
          <li>Request a test in the channel for the mode you want (e.g. Sword, Axe, Pot, Vanilla, Mace, UHC, SMP).</li>
          <li>A tester will run a fight with you on the server.</li>
          <li>Based on the fight, the tester assigns your tier (HT1–LT5).</li>
          <li>Your tier is added to this site manually so leaderboards stay accurate.</li>
        </ol>
        <p className="mt-3 text-[var(--muted)]">
          Manual testing keeps rankings trust-based. No inflated stats, no ELO abuse.
        </p>
      </motion.section>

      <motion.section
        custom={2}
        variants={sectionVariants}
        initial="hidden"
        animate="visible"
        className="rounded-xl border border-[var(--border)]/50 bg-[var(--card)]/50 p-6"
      >
        <h2 className="mb-3 text-lg font-semibold">Modes</h2>
        <p className="text-[var(--muted)]">
          We rank by mode: <strong className="text-[var(--text)]">{MODE_LIST}</strong>. Each mode has its own tier — e.g. you can be HT1 in Sword and LT1 in Pot. Tiers are independent per mode.
        </p>
      </motion.section>
    </>
  );
}
