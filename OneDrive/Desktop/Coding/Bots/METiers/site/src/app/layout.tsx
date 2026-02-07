import type { Metadata } from "next";
import { Space_Grotesk } from "next/font/google";
import "./globals.css";
import { Nav } from "@/components/Nav";
import { ModeTabsBar } from "@/components/ModeTabsBar";

const spaceGrotesk = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-sans",
});

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL || "https://metiers.example.com";

export const metadata: Metadata = {
  icons: {
    icon: "/favicon.svg",
  },
  title: {
    default: "METiers — Middle East PvP Rankings",
    template: "%s | METiers",
  },
  description:
    "Middle East PvP ranking. Earn manual skill tiers through official tests and appear on public leaderboards for Sword, Axe, Pot, Vanilla, Mace, UHC, SMP and more.",
  keywords: ["PvP", "rankings", "Middle East", "Minecraft", "tiers", "leaderboard", "Sword", "Axe", "Pot", "UHC", "SMP"],
  authors: [{ name: "METiers" }],
  openGraph: {
    type: "website",
    locale: "en_US",
    url: siteUrl,
    siteName: "METiers",
    title: "METiers — Middle East PvP Rankings",
    description: "Middle East PvP ranking. Earn manual skill tiers through official tests and appear on public leaderboards per PvP mode.",
  },
  twitter: {
    card: "summary_large_image",
    title: "METiers — Middle East PvP Rankings",
    description: "Middle East PvP ranking. Earn manual skill tiers through official tests and appear on public leaderboards per PvP mode.",
  },
  robots: {
    index: true,
    follow: true,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={spaceGrotesk.variable}>
      <body className="min-h-screen antialiased">
        <Nav />
        <ModeTabsBar />
        <main className="relative z-10 min-h-[calc(100vh-7rem)]">{children}</main>
      </body>
    </html>
  );
}
