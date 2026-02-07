-- METiers MVP Schema
-- Run this in Supabase SQL Editor to create tables

-- Players: Minecraft username, region (Middle East), optional Discord ID for tier sync
create table if not exists public.players (
  id uuid primary key default gen_random_uuid(),
  username text not null unique,
  region text not null default 'Middle East',
  discord_id text unique,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Add discord_id if table already existed without it (run in SQL Editor if needed):
-- alter table public.players add column if not exists discord_id text unique;

-- Per-mode tiers: one row per player per mode
create table if not exists public.player_tiers (
  id uuid primary key default gen_random_uuid(),
  player_id uuid not null references public.players(id) on delete cascade,
  mode text not null check (mode in ('sword', 'axe', 'netherite_pot', 'diamond_pot', 'vanilla', 'mace', 'uhc', 'smp')),
  tier text not null check (tier in (
    'HT1','LT1','HT2','LT2','HT3','LT3','HT4','LT4','HT5','LT5'
  )),
  last_tested_at timestamptz not null default now(),
  tester_name text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(player_id, mode)
);

-- Admin users: simple table for admin-only auth (email or list)
create table if not exists public.admin_users (
  id uuid primary key default gen_random_uuid(),
  email text not null unique,
  created_at timestamptz not null default now()
);

-- RLS: allow public read on players and player_tiers
alter table public.players enable row level security;
alter table public.player_tiers enable row level security;

create policy "Public read players" on public.players for select using (true);
create policy "Public read player_tiers" on public.player_tiers for select using (true);

-- Admin write: use service role or add policy for authenticated admin
-- For MVP we use service role in API routes, so no insert/update policies needed for anon.

-- Trigger to update updated_at
create or replace function public.set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create trigger players_updated_at before update on public.players
  for each row execute function public.set_updated_at();
create trigger player_tiers_updated_at before update on public.player_tiers
  for each row execute function public.set_updated_at();
