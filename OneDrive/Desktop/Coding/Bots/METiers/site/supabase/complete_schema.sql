-- =============================================================================
-- METiers Complete Database Schema
-- Run this in Supabase SQL Editor to set up or fix the database
-- =============================================================================

-- =============================================================================
-- PLAYERS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS players (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username TEXT UNIQUE NOT NULL,
  discord_id TEXT UNIQUE,
  region TEXT DEFAULT 'Middle East',
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- Add discord_id column if it doesn't exist (for existing tables)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'players' AND column_name = 'discord_id'
  ) THEN
    ALTER TABLE players ADD COLUMN discord_id TEXT UNIQUE;
  END IF;
END $$;

-- Create index for discord_id lookups
CREATE INDEX IF NOT EXISTS idx_players_discord_id ON players(discord_id);
CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);

-- =============================================================================
-- PLAYER_TIERS TABLE
-- =============================================================================
CREATE TABLE IF NOT EXISTS player_tiers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  mode TEXT NOT NULL,
  tier TEXT NOT NULL,
  peak_tier TEXT,
  tester_name TEXT DEFAULT 'Unknown',
  last_tested_at TIMESTAMPTZ DEFAULT now(),
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(player_id, mode)
);

-- Add peak_tier if missing (migration for existing DBs)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_name = 'player_tiers' AND column_name = 'peak_tier'
  ) THEN
    ALTER TABLE player_tiers ADD COLUMN peak_tier TEXT;
  END IF;
END $$;

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_player_tiers_player_id ON player_tiers(player_id);
CREATE INDEX IF NOT EXISTS idx_player_tiers_mode ON player_tiers(mode);
CREATE INDEX IF NOT EXISTS idx_player_tiers_tier ON player_tiers(tier);

-- =============================================================================
-- TIER AUDIT LOG (who changed which tier when)
-- =============================================================================
CREATE TABLE IF NOT EXISTS tier_audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
  username TEXT NOT NULL,
  mode TEXT NOT NULL,
  old_tier TEXT,
  new_tier TEXT NOT NULL,
  source TEXT NOT NULL CHECK (source IN ('admin', 'webhook', 'sync')),
  changed_by TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tier_audit_log_player_id ON tier_audit_log(player_id);
CREATE INDEX IF NOT EXISTS idx_tier_audit_log_created_at ON tier_audit_log(created_at DESC);

-- =============================================================================
-- ADMIN_USERS TABLE (optional - for future admin panel)
-- =============================================================================
CREATE TABLE IF NOT EXISTS admin_users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- =============================================================================
-- AUTO-UPDATE TIMESTAMPS
-- =============================================================================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to players table
DROP TRIGGER IF EXISTS players_updated_at ON players;
CREATE TRIGGER players_updated_at
  BEFORE UPDATE ON players
  FOR EACH ROW
  EXECUTE FUNCTION set_updated_at();

-- Apply trigger to player_tiers table
DROP TRIGGER IF EXISTS player_tiers_updated_at ON player_tiers;
CREATE TRIGGER player_tiers_updated_at
  BEFORE UPDATE ON player_tiers
  FOR EACH ROW
  EXECUTE FUNCTION set_updated_at();

-- =============================================================================
-- ROW LEVEL SECURITY (RLS)
-- =============================================================================

-- Enable RLS
ALTER TABLE players ENABLE ROW LEVEL SECURITY;
ALTER TABLE player_tiers ENABLE ROW LEVEL SECURITY;

-- Public read access
DROP POLICY IF EXISTS "Public read players" ON players;
CREATE POLICY "Public read players" ON players
  FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read player_tiers" ON player_tiers;
CREATE POLICY "Public read player_tiers" ON player_tiers
  FOR SELECT USING (true);

-- Service role can do everything (for API routes with service key)
DROP POLICY IF EXISTS "Service role full access players" ON players;
CREATE POLICY "Service role full access players" ON players
  FOR ALL USING (auth.role() = 'service_role');

DROP POLICY IF EXISTS "Service role full access player_tiers" ON player_tiers;
CREATE POLICY "Service role full access player_tiers" ON player_tiers
  FOR ALL USING (auth.role() = 'service_role');

-- tier_audit_log: service role only
ALTER TABLE tier_audit_log ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS "Service role full access tier_audit_log" ON tier_audit_log;
CREATE POLICY "Service role full access tier_audit_log" ON tier_audit_log
  FOR ALL USING (auth.role() = 'service_role');

-- =============================================================================
-- VALID MODES (for reference - enforced by application)
-- =============================================================================
-- sword, axe, netherite_pot, diamond_pot, vanilla, mace, uhc, smp

-- =============================================================================
-- VALID TIERS (for reference - enforced by application)
-- =============================================================================
-- HT1, LT1, HT2, LT2, HT3, LT3, HT4, LT4, HT5, LT5

-- =============================================================================
-- HELPER VIEWS (optional)
-- =============================================================================

-- View: Leaderboard with player info
CREATE OR REPLACE VIEW leaderboard_view AS
SELECT
  p.id as player_id,
  p.username,
  p.discord_id,
  p.region,
  pt.mode,
  pt.tier,
  pt.tester_name,
  pt.last_tested_at
FROM players p
JOIN player_tiers pt ON p.id = pt.player_id
ORDER BY
  pt.mode,
  CASE pt.tier
    WHEN 'HT1' THEN 0
    WHEN 'LT1' THEN 1
    WHEN 'HT2' THEN 2
    WHEN 'LT2' THEN 3
    WHEN 'HT3' THEN 4
    WHEN 'LT3' THEN 5
    WHEN 'HT4' THEN 6
    WHEN 'LT4' THEN 7
    WHEN 'HT5' THEN 8
    WHEN 'LT5' THEN 9
    ELSE 10
  END,
  pt.last_tested_at DESC;

-- =============================================================================
-- VERIFICATION QUERIES (run these to check setup)
-- =============================================================================
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'players';
-- SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'player_tiers';
-- SELECT * FROM leaderboard_view LIMIT 10;
