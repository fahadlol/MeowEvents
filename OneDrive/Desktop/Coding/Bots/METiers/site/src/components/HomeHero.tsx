"use client";

import Link from "next/link";
import { motion } from "framer-motion";

const container = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.08, delayChildren: 0.05 },
  },
};

const item = {
  hidden: { opacity: 0, y: 16 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.45, ease: [0.25, 0.46, 0.45, 0.94] },
  },
};

export function HomeHero() {
  return (
    <motion.section
      variants={container}
      initial="hidden"
      animate="visible"
      className="relative mb-12 overflow-hidden rounded-2xl border border-[var(--border-subtle)] bg-[var(--card)]/60 p-8 text-center shadow-[var(--glow-card)] backdrop-blur-sm md:p-12"
    >
      {/* Subtle inner glow */}
      <div className="pointer-events-none absolute inset-0 rounded-2xl bg-gradient-to-b from-[var(--accent)]/5 to-transparent opacity-60" />
      <div className="relative">
        <motion.h1
          variants={item}
          className="mb-4 text-3xl font-bold tracking-tight text-white drop-shadow-sm md:text-4xl lg:text-5xl"
          style={{
            background: "linear-gradient(90deg, var(--logo-start), var(--logo-mid), var(--logo-end))",
            WebkitBackgroundClip: "text",
            WebkitTextFillColor: "transparent",
            backgroundClip: "text",
          }}
        >
          METiers
        </motion.h1>
        <motion.p
          variants={item}
          className="mx-auto max-w-xl text-lg leading-relaxed text-[var(--muted)]"
        >
          Middle East PvP ranking. Earn manual skill tiers through official tests
          and appear on public leaderboards per PvP mode.
        </motion.p>
        <motion.div
          variants={item}
          className="mt-8 flex flex-wrap justify-center gap-4"
        >
          <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.98 }}>
            <Link
              href="/leaderboards"
              className="inline-flex items-center gap-2 rounded-xl bg-[var(--accent)] px-6 py-3 font-medium text-white shadow-lg shadow-[var(--accent)]/25 transition-all duration-300 hover:shadow-xl hover:shadow-[var(--accent)]/30"
            >
              View leaderboards
              <span className="opacity-80">→</span>
            </Link>
          </motion.div>
          <motion.div whileHover={{ scale: 1.03 }} whileTap={{ scale: 0.98 }}>
            <Link
              href="/discord"
              className="inline-flex items-center gap-2 rounded-xl border border-[var(--border)] bg-white/5 px-6 py-3 font-medium transition-all duration-300 hover:border-[var(--accent)]/40 hover:bg-white/10"
            >
              Join Discord
            </Link>
          </motion.div>
        </motion.div>
      </div>
    </motion.section>
  );
}
