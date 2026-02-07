"use client";

import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import { motion, AnimatePresence } from "framer-motion";
import { InfoIcon } from "./Icons";

export function InfoModal() {
  const [open, setOpen] = useState(false);
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  const overlay = mounted && (
    <AnimatePresence>
      {open && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 z-[100] bg-black/70 backdrop-blur-md"
            onClick={() => setOpen(false)}
            aria-hidden
          />
          <motion.dialog
            open
            initial={{ opacity: 0, scale: 0.9, y: 8 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 4 }}
            transition={{ type: "spring", bounce: 0.2, duration: 0.4 }}
            className="fixed left-1/2 top-1/2 z-[101] w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-[var(--border)] bg-[var(--card)] p-6 shadow-2xl outline-none"
            onClose={() => setOpen(false)}
          >
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold text-[var(--text)]">How tiers work</h3>
                <button
                  type="button"
                  onClick={() => setOpen(false)}
                  className="rounded-lg p-1 text-[var(--text-muted)] transition hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
                  aria-label="Close"
                >
                  <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
              <div className="mt-4 space-y-4 text-sm text-[var(--text-muted)]">
                <p>
                  <strong className="text-[var(--text)]">Tier 1–5</strong> — Players are grouped by skill. Tier 1 is best (HT1, LT1), Tier 5 is worst (HT5, LT5).
                </p>
                <p>
                  <strong className="text-[var(--text)]">HT / LT</strong> — High and Low tier. Ranks best to worst: HT1 → LT1 → HT2 → LT2 → … → HT5 → LT5. No decimals, no ELO numbers.
                </p>
                <p>
                  <strong className="text-[var(--text)]">Getting ranked</strong> — Join Discord, request a test in the right mode (Sword, Axe, Pot, Vanilla, Mace, UHC, SMP). A tester fights you and assigns your tier. Tiers are added manually to keep rankings trust-based.
                </p>
            </div>
          </motion.dialog>
        </>
      )}
    </AnimatePresence>
  );

  return (
    <>
      <motion.button
        type="button"
        onClick={() => setOpen(true)}
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
        className="flex items-center gap-2 rounded-lg border border-[var(--border-subtle)] bg-[var(--card)] px-3 py-2 text-sm text-[var(--text-muted)] transition-colors hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
        aria-label="How tiers work"
      >
        <InfoIcon className="h-4 w-4" />
        Information
      </motion.button>
      {mounted && createPortal(overlay, document.body)}
    </>
  );
}
