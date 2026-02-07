"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import {
  HomeIcon,
  TrophyIcon,
  DiscordIcon,
  DocIcon,
  SearchIcon,
  ChevronDownIcon,
  ModeIcon,
} from "./Icons";
import { MODES, MODE_NAMES } from "@/types";

const MC_AVATAR = "https://mc-heads.net/avatar";

const links = [
  { href: "/", label: "Home", Icon: HomeIcon },
  { href: "/leaderboards", label: "Rankings", Icon: TrophyIcon },
  { href: "/rules", label: "Rules", Icon: DocIcon },
];

const DROPDOWN_MODES = MODES.map((slug) => ({
  slug,
  name: MODE_NAMES[slug],
  href: `/mode/${slug}`,
}));

interface PlayerResult {
  id: string;
  username: string;
  discord_id: string;
  region: string;
  tiers: Record<string, { tier: string; tester: string; date: string }>;
}

export function Nav() {
  const pathname = usePathname();
  const [discordOpen, setDiscordOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<PlayerResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResults, setShowResults] = useState(false);
  const [selectedPlayer, setSelectedPlayer] = useState<PlayerResult | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchRef = useRef<HTMLDivElement>(null);
  const searchTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDiscordOpen(false);
      }
      if (searchRef.current && !searchRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const searchPlayers = useCallback(async (query: string) => {
    if (query.length < 2) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    setIsSearching(true);
    try {
      const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
      const data = await res.json();
      setSearchResults(data.players || []);
      setShowResults(true);
    } catch {
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  }, []);

  function handleSearchChange(e: React.ChangeEvent<HTMLInputElement>) {
    const value = e.target.value;
    setSearchQuery(value);

    if (searchTimeoutRef.current) {
      clearTimeout(searchTimeoutRef.current);
    }

    searchTimeoutRef.current = setTimeout(() => {
      searchPlayers(value);
    }, 300);
  }

  function handlePlayerClick(player: PlayerResult) {
    setSelectedPlayer(player);
    setShowResults(false);
    setSearchQuery("");
  }

  return (
    <>
      <motion.nav
        initial={{ opacity: 0, y: -12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.25, 0.46, 0.45, 0.94] }}
        className="sticky top-0 z-50 border-b border-[var(--border-subtle)] bg-[var(--card)]/90 backdrop-blur-xl shadow-[var(--glow-card)]"
      >
        <div className="mx-auto flex h-14 max-w-6xl items-center justify-between gap-4 px-4">
          {/* Logo */}
          <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
            <Link
              href="/"
              className="block shrink-0 text-xl font-bold tracking-tight transition hover:opacity-90"
              style={{
                background: "linear-gradient(90deg, var(--logo-start), var(--logo-mid), var(--logo-end))",
                WebkitBackgroundClip: "text",
                WebkitTextFillColor: "transparent",
                backgroundClip: "text",
                textShadow: "0 1px 2px rgba(0,0,0,0.3)",
              }}
            >
              METiers
            </Link>
          </motion.div>

          {/* Center nav */}
          <ul className="flex items-center gap-0.5">
            {links.map(({ href, label, Icon }) => {
              const isActive = pathname === href || (href !== "/" && pathname.startsWith(href));
              return (
                <li key={href}>
                  <Link
                    href={href}
                    className={`relative flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors duration-200 ${
                      isActive ? "text-[var(--text)]" : "text-[var(--text-muted)] hover:text-[var(--text)]"
                    }`}
                  >
                    {isActive && (
                      <motion.span
                        layoutId="nav-pill"
                        className="absolute inset-0 rounded-lg bg-[var(--accent-soft)]"
                        transition={{ type: "spring", bounce: 0.2, duration: 0.4 }}
                      />
                    )}
                    <span className="relative z-0 flex items-center gap-2">
                      <Icon className="h-4 w-4 shrink-0" />
                      {label}
                    </span>
                  </Link>
                </li>
              );
            })}
            {/* Discords dropdown */}
            <li className="relative" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setDiscordOpen((o) => !o)}
                className={`flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition ${
                  discordOpen ? "bg-[var(--accent-soft)] text-[var(--accent)]" : "text-[var(--text-muted)] hover:text-[var(--text)]"
                }`}
                aria-expanded={discordOpen}
                aria-haspopup="true"
              >
                <DiscordIcon className="h-4 w-4 shrink-0" />
                Discords
                <ChevronDownIcon className={`h-3.5 w-3 shrink-0 transition ${discordOpen ? "rotate-180" : ""}`} />
              </button>
              <AnimatePresence>
                {discordOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: -4 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -4 }}
                    transition={{ duration: 0.15 }}
                    className="absolute left-0 top-full z-10 mt-1 w-56 rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] p-2 shadow-[var(--glow-card)]"
                  >
                    <Link
                      href="/discord"
                      className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-[var(--text-muted)] transition hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
                      onClick={() => setDiscordOpen(false)}
                    >
                      <DiscordIcon className="h-4 w-4" />
                      Join Discord
                    </Link>
                    <div className="my-1 border-t border-[var(--border-subtle)]" />
                    <p className="px-3 py-1 text-xs uppercase tracking-wider text-[var(--text-muted)]">Modes</p>
                    {DROPDOWN_MODES.map(({ slug, name, href }) => (
                      <Link
                        key={slug}
                        href={href}
                        className="flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-[var(--text-muted)] transition hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
                        onClick={() => setDiscordOpen(false)}
                      >
                        <ModeIcon mode={slug} className="h-4 w-4 shrink-0" />
                        {name}
                      </Link>
                    ))}
                  </motion.div>
                )}
              </AnimatePresence>
            </li>
          </ul>

          {/* Global search with dropdown */}
          <div className="relative shrink-0" ref={searchRef}>
            <div className="relative transition-transform duration-200 focus-within:scale-[1.01]">
              <SearchIcon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[var(--muted)] transition-colors group-focus-within:text-[var(--accent)]" />
              <input
                type="search"
                placeholder="Search player..."
                value={searchQuery}
                onChange={handleSearchChange}
                onFocus={() => searchResults.length > 0 && setShowResults(true)}
                className="group w-44 rounded-lg border border-[var(--border-subtle)] bg-[var(--bg-elevated)] py-2 pl-9 pr-8 text-sm text-[var(--text)] placeholder-[var(--text-muted)] transition-all duration-200 focus:border-[var(--accent)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)]/30 sm:w-52"
                aria-label="Search player"
              />
              {isSearching && (
                <div className="absolute right-2 top-1/2 -translate-y-1/2">
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-[var(--accent)] border-t-transparent" />
                </div>
              )}
              {!isSearching && (
                <kbd className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 rounded border border-[var(--border)] bg-black/30 px-1.5 py-0.5 text-[10px] text-[var(--text-muted)]">/</kbd>
              )}
            </div>

            {/* Search results dropdown */}
            <AnimatePresence>
              {showResults && (
                <motion.div
                  initial={{ opacity: 0, y: -4 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -4 }}
                  transition={{ duration: 0.15 }}
                  className="absolute right-0 top-full z-50 mt-2 w-72 rounded-xl border border-[var(--border-subtle)] bg-[var(--card)] shadow-xl"
                >
                  {searchResults.length === 0 ? (
                    <div className="p-4 text-center text-sm text-[var(--text-muted)]">
                      No players found
                    </div>
                  ) : (
                    <ul className="max-h-80 overflow-y-auto p-2">
                      {searchResults.map((player) => (
                        <li key={player.id}>
                          <button
                            type="button"
                            onClick={() => handlePlayerClick(player)}
                            className="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-left transition hover:bg-[var(--card-hover)]"
                          >
                            <span className="relative h-8 w-8 shrink-0 overflow-hidden rounded-lg border border-[var(--border-subtle)] bg-[var(--bg)]">
                              <Image
                                src={`${MC_AVATAR}/${encodeURIComponent(player.username)}/32`}
                                alt=""
                                width={32}
                                height={32}
                                unoptimized
                                className="object-cover"
                              />
                            </span>
                            <div className="min-w-0 flex-1">
                              <p className="truncate font-medium text-[var(--text)]">{player.username}</p>
                              <p className="text-xs text-[var(--text-muted)]">
                                {Object.keys(player.tiers).length} tier(s) • {player.region}
                              </p>
                            </div>
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </div>
      </motion.nav>

      {/* Player detail modal */}
      <AnimatePresence>
        {selectedPlayer && (
          <PlayerModal player={selectedPlayer} onClose={() => setSelectedPlayer(null)} />
        )}
      </AnimatePresence>
    </>
  );
}

function PlayerModal({ player, onClose }: { player: PlayerResult; onClose: () => void }) {
  useEffect(() => {
    function handleEsc(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    document.addEventListener("keydown", handleEsc);
    return () => document.removeEventListener("keydown", handleEsc);
  }, [onClose]);

  const tierEntries = Object.entries(player.tiers);

  return (
    <>
      {/* Backdrop */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        className="fixed inset-0 z-[100] bg-black/60 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 20 }}
        transition={{ type: "spring", bounce: 0.25, duration: 0.4 }}
        className="fixed left-1/2 top-1/2 z-[101] w-full max-w-md -translate-x-1/2 -translate-y-1/2 rounded-2xl border border-[var(--border-subtle)] bg-[var(--card)] p-6 shadow-2xl"
      >
        {/* Close button */}
        <button
          type="button"
          onClick={onClose}
          className="absolute right-4 top-4 rounded-lg p-1 text-[var(--text-muted)] transition hover:bg-[var(--card-hover)] hover:text-[var(--text)]"
        >
          <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        {/* Player header */}
        <div className="flex items-center gap-4">
          <div className="relative h-16 w-16 overflow-hidden rounded-xl border-2 border-[var(--border-subtle)] bg-[var(--bg)]">
            <Image
              src={`${MC_AVATAR}/${encodeURIComponent(player.username)}/64`}
              alt={player.username}
              width={64}
              height={64}
              unoptimized
              className="object-cover"
            />
          </div>
          <div>
            <h2 className="text-xl font-bold text-[var(--text)]">{player.username}</h2>
            <p className="text-sm text-[var(--text-muted)]">{player.region}</p>
          </div>
        </div>

        {/* Tiers */}
        <div className="mt-6">
          <h3 className="mb-3 text-sm font-semibold uppercase tracking-wider text-[var(--text-muted)]">
            Tiers ({tierEntries.length})
          </h3>
          {tierEntries.length === 0 ? (
            <p className="text-center text-sm text-[var(--text-muted)]">No tiers yet</p>
          ) : (
            <div className="space-y-2">
              {tierEntries.map(([mode, data]) => (
                <div
                  key={mode}
                  className="flex items-center justify-between rounded-lg border border-[var(--border-subtle)] bg-[var(--bg-elevated)] px-4 py-3"
                >
                  <div className="flex items-center gap-3">
                    <ModeIcon mode={mode as any} className="h-5 w-5 text-[var(--text-muted)]" />
                    <span className="font-medium text-[var(--text)]">
                      {MODE_NAMES[mode as keyof typeof MODE_NAMES] || mode}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block rounded-md px-2 py-0.5 text-sm font-bold ${getTierColor(data.tier)}`}>
                      {data.tier}
                    </span>
                    <p className="mt-0.5 text-xs text-[var(--text-muted)]">
                      by {data.tester}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* View profile link */}
        <Link
          href={`/player/${encodeURIComponent(player.username)}`}
          onClick={onClose}
          className="mt-6 flex w-full items-center justify-center gap-2 rounded-lg bg-[var(--accent)] px-4 py-2.5 font-medium text-white transition hover:opacity-90"
        >
          View Full Profile
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M14 5l7 7m0 0l-7 7m7-7H3" />
          </svg>
        </Link>
      </motion.div>
    </>
  );
}

function getTierColor(tier: string): string {
  if (tier.startsWith("HT1") || tier.startsWith("LT1")) return "bg-amber-500/20 text-amber-400";
  if (tier.startsWith("HT2") || tier.startsWith("LT2")) return "bg-violet-500/20 text-violet-400";
  if (tier.startsWith("HT3") || tier.startsWith("LT3")) return "bg-orange-500/20 text-orange-400";
  if (tier.startsWith("HT4") || tier.startsWith("LT4")) return "bg-emerald-500/20 text-emerald-400";
  return "bg-slate-500/20 text-slate-400";
}
