package me.oblueberrey.meowMcEvents.listeners;

import me.oblueberrey.meowMcEvents.MeowMCEvents;
import me.oblueberrey.meowMcEvents.managers.DamageTracker;
import me.oblueberrey.meowMcEvents.managers.EventManager;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Tracks damage attribution for proper kill credit.
 * Handles:
 * - Projectiles (arrows, tridents, snowballs, eggs, potions)
 * - Explosions (TNT, creepers, beds, crystals)
 * - Fire/Lava (burning from player-placed fire or knockback into lava)
 * - Falling (knockback causing fall damage)
 * 
 * Works with FatalDamageListener to ensure correct killer attribution.
 */
public class DamageAttributionListener implements Listener {

    private final MeowMCEvents plugin;
    private final EventManager eventManager;
    private final DamageTracker damageTracker;

    public DamageAttributionListener(MeowMCEvents plugin, EventManager eventManager, DamageTracker damageTracker) {
        this.plugin = plugin;
        this.eventManager = eventManager;
        this.damageTracker = damageTracker;
    }

    /**
     * Track all damage events for attribution.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player victim = (Player) event.getEntity();
        
        // Only track damage for event players
        if (!eventManager.isEventRunning() || !eventManager.isPlayerInEvent(victim)) {
            return;
        }

        // Handle entity damage specifically
        if (event instanceof EntityDamageByEntityEvent) {
            handleEntityDamage((EntityDamageByEntityEvent) event, victim);
            return;
        }

        // Handle environmental damage that might have a combat tag
        EntityDamageEvent.DamageCause cause = event.getCause();
        switch (cause) {
            case FIRE:
            case FIRE_TICK:
            case LAVA:
            case FALL:
            case VOID:
            case DROWNING:
            case SUFFOCATION:
                // These can be attributed to the last attacker if combat tag is active
                // The DamageTracker will handle expiration
                break;
            default:
                break;
        }
    }

    /**
     * Handle damage caused by entities (players, projectiles, explosions).
     */
    private void handleEntityDamage(EntityDamageByEntityEvent event, Player victim) {
        Entity damager = event.getDamager();
        Player attacker = null;
        String cause = "unknown";

        // Direct player damage
        if (damager instanceof Player) {
            attacker = (Player) damager;
            cause = "melee";
        }
        // Projectile damage
        else if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            ProjectileSource shooter = projectile.getShooter();
            
            if (shooter instanceof Player) {
                attacker = (Player) shooter;
                cause = getProjectileName(projectile);
            }
        }
        // Explosion damage (TNT, creepers, beds, crystals)
        else if (damager instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) damager;
            Entity source = tnt.getSource();
            if (source instanceof Player) {
                attacker = (Player) source;
                cause = "TNT";
            }
        }
        else if (damager instanceof Creeper) {
            // Creepers don't have a player source, but check combat tag
            cause = "creeper";
        }
        else if (damager instanceof EnderCrystal) {
            // End crystals can be attributed to last attacker via combat tag
            cause = "crystal";
        }
        else if (damager instanceof Firework) {
            Firework firework = (Firework) damager;
            // Try to get shooter from firework (crossbow rockets)
            if (firework.getShooter() instanceof Player) {
                attacker = (Player) firework.getShooter();
                cause = "firework";
            }
        }
        else if (damager instanceof AreaEffectCloud) {
            AreaEffectCloud cloud = (AreaEffectCloud) damager;
            ProjectileSource source = cloud.getSource();
            if (source instanceof Player) {
                attacker = (Player) source;
                cause = "potion cloud";
            }
        }
        else if (damager instanceof EvokerFangs) {
            EvokerFangs fangs = (EvokerFangs) damager;
            LivingEntity owner = fangs.getOwner();
            if (owner instanceof Player) {
                attacker = (Player) owner;
                cause = "evoker fangs";
            }
        }
        // Wolf/tamed animals
        else if (damager instanceof Tameable) {
            Tameable tameable = (Tameable) damager;
            if (tameable.isTamed() && tameable.getOwner() instanceof Player) {
                attacker = (Player) tameable.getOwner();
                cause = "pet";
            }
        }

        // Record the damage if we found an attacker
        if (attacker != null && eventManager.isPlayerInEvent(attacker) && !attacker.equals(victim)) {
            damageTracker.recordDamage(victim, attacker, cause);
        }
    }

    /**
     * Get a readable name for the projectile type.
     */
    private String getProjectileName(Projectile projectile) {
        if (projectile instanceof Arrow) {
            if (projectile instanceof SpectralArrow) {
                return "spectral arrow";
            }
            return "arrow";
        } else if (projectile instanceof Trident) {
            return "trident";
        } else if (projectile instanceof Snowball) {
            return "snowball";
        } else if (projectile instanceof Egg) {
            return "egg";
        } else if (projectile instanceof EnderPearl) {
            return "ender pearl";
        } else if (projectile instanceof ThrownPotion) {
            return "potion";
        } else if (projectile instanceof Fireball) {
            return "fireball";
        } else if (projectile instanceof WitherSkull) {
            return "wither skull";
        } else if (projectile instanceof ShulkerBullet) {
            return "shulker bullet";
        } else if (projectile instanceof LlamaSpit) {
            return "llama spit";
        }
        return "projectile";
    }
}
