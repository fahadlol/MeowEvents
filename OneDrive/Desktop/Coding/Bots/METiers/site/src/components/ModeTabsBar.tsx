"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import {
  ModeIcon,
  InfoIcon,
  DiscordIcon,
} from "./Icons";
import { MODES, MODE_NAMES } from "@/types";
import { InfoModal } from "./InfoModal";

const MODES_WITH_ICONS = MODES.map((slug) => ({
  slug,
  name: MODE_NAMES[slug],
}));

export function ModeTabsBar() {
  const pathname = usePathname();
  const serverIp = process.env.NEXT_PUBLIC_SERVER_IP || "me.vise.network";
  const discordUrl = process.env.NEXT_PUBLIC_DISCORD_INVITE_URL || "/discord";

  return (
    <motion.div
      initial={{ opacity: 0, y: -8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, ease: [0.25, 0.46, 0.45, 0.94] }}
      className="border-b border-[var(--border-subtle)] bg-[var(--card)]/80 backdrop-blur-sm"
    >
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-3">
        {/* Mode tabs */}
        <div className="flex items-center gap-1">
          <Link
            href="/leaderboards"
            className={`relative flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors duration-200 ${
              pathname === "/leaderboards" ? "text-[var(--accent)]" : "text-[var(--text-muted)] hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
            }`}
          >
            {pathname === "/leaderboards" && (
              <motion.span
                layoutId="mode-tab-pill"
                className="absolute inset-0 rounded-lg bg-[var(--accent-soft)]"
                transition={{ type: "spring", bounce: 0.2, duration: 0.4 }}
              />
            )}
            <span className="relative z-0 flex items-center gap-2">
              <ModeIcon mode="overall" className="h-4 w-4 shrink-0" />
              Overall
            </span>
          </Link>
          {MODES_WITH_ICONS.map(({ slug, name }) => {
            const isActive = pathname === `/mode/${slug}`;
            return (
              <Link
                key={slug}
                href={`/mode/${slug}`}
                className={`relative flex items-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors duration-200 ${
                  isActive ? "text-[var(--accent)]" : "text-[var(--text-muted)] hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
                }`}
              >
                {isActive && (
                  <motion.span
                    layoutId="mode-tab-pill"
                    className="absolute inset-0 rounded-lg bg-[var(--accent-soft)]"
                    transition={{ type: "spring", bounce: 0.2, duration: 0.4 }}
                  />
                )}
                <span className="relative z-0 flex items-center gap-2">
                  <ModeIcon mode={slug} className="h-4 w-4 shrink-0" />
                  {name}
                </span>
              </Link>
            );
          })}
        </div>

        {/* Information button + SERVER IP + Discord */}
        <div className="flex flex-wrap items-center gap-3">
          <InfoModal />
          {serverIp && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="flex items-center gap-2 rounded-lg border border-[var(--border-subtle)] bg-[var(--bg-elevated)] px-3 py-2 text-sm transition-colors hover:border-[var(--accent)]/30"
            >
              <span className="text-[var(--text-muted)]">SERVER IP</span>
              <span className="font-mono font-medium text-[var(--text)]">{serverIp}</span>
            </motion.div>
          )}
          <motion.a
            href={discordUrl}
            target="_blank"
            rel="noopener noreferrer"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            className="flex items-center gap-1.5 rounded-lg border border-[var(--border-subtle)] bg-[#5865F2]/20 px-3 py-2 text-sm font-medium text-white transition-colors hover:bg-[#5865F2]/30"
          >
            <DiscordIcon className="h-4 w-4" />
            Discord
          </motion.a>
        </div>
      </div>
    </motion.div>
  );
}

function TrophyIcon({ className }: { className?: string }) {
  return (
    <svg className={className} fill="currentColor" viewBox="0 0 24 24">
      <path d="M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H8v2h8v-2h-3v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z" />
    </svg>
  );
}
