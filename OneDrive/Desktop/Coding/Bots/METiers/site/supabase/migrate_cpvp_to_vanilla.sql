-- Run this in Supabase SQL Editor if you have existing data with mode = 'cpvp'
-- (after renaming the slug from cpvp to vanilla in the app)

UPDATE player_tiers SET mode = 'vanilla' WHERE mode = 'cpvp';

-- If your player_tiers table has a CHECK constraint on mode that includes 'cpvp',
-- you may need to drop and re-add it. Example (constraint name may differ):
-- ALTER TABLE player_tiers DROP CONSTRAINT IF EXISTS player_tiers_mode_check;
-- ALTER TABLE player_tiers ADD CONSTRAINT player_tiers_mode_check
--   CHECK (mode IN ('sword','axe','netherite_pot','diamond_pot','vanilla','mace','uhc','smp'));
