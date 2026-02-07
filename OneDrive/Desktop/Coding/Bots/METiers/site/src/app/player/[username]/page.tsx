import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getPlayerByUsername, getOverallRankAndTotal } from "@/lib/data";
import { MODES, tierRank } from "@/types";
import { PlayerProfileCard } from "@/components/PlayerProfileCard";

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || "";

type Props = { params: Promise<{ username: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { username } = await params;
  const player = await getPlayerByUsername(decodeURIComponent(username));
  if (!player) return { title: "Player not found" };
  const rankInfo = await getOverallRankAndTotal(player.id);
  const tiers = player.tiers || {};
  const testedCount = MODES.filter((m) => tiers[m]).length;
  const description =
    rankInfo?.rank != null
      ? `Rank #${rankInfo.rank} of ${rankInfo.total} · ${testedCount} modes tested · ${player.region}`
      : `${testedCount} modes tested · ${player.region}`;
  const url = siteUrl ? `${siteUrl}/player/${encodeURIComponent(player.username)}` : undefined;
  return {
    title: `${player.username} — Profile`,
    description,
    openGraph: url ? { title: `${player.username} | METiers`, description, url } : undefined,
    twitter: { card: "summary", title: `${player.username} | METiers`, description },
  };
}

export default async function PlayerPage({ params }: Props) {
  const { username } = await params;
  const player = await getPlayerByUsername(decodeURIComponent(username));
  if (!player) notFound();

  const tiers = player.tiers || {};
  const hasTiers = MODES.some((m) => tiers[m]);
  const bestTier = hasTiers
    ? (MODES.map((m) => tiers[m]?.tier).filter(Boolean) as string[]).sort(
        (a, b) => tierRank(a as any) - tierRank(b as any)
      )[0]
    : null;

  const rankInfo = await getOverallRankAndTotal(player.id);

  return (
    <PlayerProfileCard
      player={player}
      bestTier={bestTier}
      overallRank={rankInfo?.rank}
      totalPlayers={rankInfo?.total}
    />
  );
}
