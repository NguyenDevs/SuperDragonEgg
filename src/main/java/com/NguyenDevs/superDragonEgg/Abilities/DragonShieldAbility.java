package com.NguyenDevs.superDragonEgg.Abilities;

import com.NguyenDevs.superDragonEgg.Manager.ConfigManager;
import com.NguyenDevs.superDragonEgg.Manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DragonShieldAbility implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerManager playerManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    public DragonShieldAbility(JavaPlugin plugin, ConfigManager configManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerManager = playerManager;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        if (!configManager.isAbilityEnabled()) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        if (event.isSneaking() && isHoldingDragonEgg(player) && player.hasPermission("sde.use") && configManager.getEnableWorlds().contains(player.getWorld().getName())) {
            long currentTime = System.currentTimeMillis();
            long lastUsed = cooldowns.getOrDefault(playerId, 0L);

            if (currentTime - lastUsed < configManager.getAbilityCooldown() * 1000L) {
                player.sendMessage(configManager.getMessages().getString("messages.ability-on-cooldown", "§d[§5SuperDragonEgg§d] §cAbility is on cooldown!"));
                return;
            }

            if (activeTasks.containsKey(playerId)) {
                activeTasks.get(playerId).cancel();
                activeTasks.remove(playerId);
                player.removePotionEffect(PotionEffectType.SLOW);
            }

            if (!playerManager.isPlayerWithDragonEgg(player)) {
               // plugin.getLogger().warning("Player " + player.getName() + " attempted to activate ability without dragon egg!");
                return;
            }

            // Update cooldown
            cooldowns.put(playerId, currentTime);

            // Apply Slow 1 effect
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 1, false, false));

            // Start ability effect
            BukkitRunnable task = new BukkitRunnable() {
                int ticks = 0;
                final int soundIntervalTicks = 10;

                @Override
                public void run() {
                    if (!player.isSneaking() || !playerManager.isPlayerWithDragonEgg(player) || !player.isOnline() || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                        cancel();
                        player.removePotionEffect(PotionEffectType.SLOW);
                        activeTasks.remove(playerId);
                        return;
                    }

                    Location center = player.getLocation().add(0, 0.5, 0);
                    spawnSphereParticles(center);
                    applyKnockback(center, player);

                    if (ticks % soundIntervalTicks == 0) {
                        player.getWorld().playSound(center, configManager.getAbilitySound(), configManager.getSoundVolume(), configManager.getSoundPitch());
                    }

                    ticks++;
                }
            };
            task.runTaskTimer(plugin, 0L, 1L);
            activeTasks.put(playerId, task);
        }
        else if (!event.isSneaking() && activeTasks.containsKey(playerId)) {
            activeTasks.get(playerId).cancel();
            activeTasks.remove(playerId);
            player.removePotionEffect(PotionEffectType.SLOW);
        }
    }

    private boolean isHoldingDragonEgg(Player player) {
        if (player.getItemInHand() != null && player.getItemInHand().getType() == Material.DRAGON_EGG) {
            return true;
        }
        return false;
    }

    private void spawnSphereParticles(Location center) {
        double radius = configManager.getAbilityShieldRadius();
        double time = System.currentTimeMillis() / 1000.0;
        int spirals = 4;
        int pointsPerSpiral = 24;
        double height = radius * 2;

        for (int spiral = 0; spiral < spirals; spiral++) {
            double spiralOffset = 2 * Math.PI * spiral / spirals;

            for (int point = 0; point < pointsPerSpiral; point++) {

                double progress = (double) point / (pointsPerSpiral - 1);
                double y = -radius + height * progress;

                double normalizedHeight = (y + radius) / height;
                double sphereRadius;

                if (normalizedHeight <= 0.5) {

                    sphereRadius = radius * Math.sqrt(1 - Math.pow(2 * normalizedHeight - 1, 2));
                } else {

                    sphereRadius = radius * Math.sqrt(1 - Math.pow(2 * normalizedHeight - 1, 2));
                }

                double rotations = 3.0;
                double theta = spiralOffset + time + rotations * 2 * Math.PI * progress;

                double x = sphereRadius * Math.cos(theta);
                double z = sphereRadius * Math.sin(theta);

                Location particleLoc = center.clone().add(x, y, z);
                center.getWorld().spawnParticle(
                        configManager.getParticleType(),
                        particleLoc,
                        1,
                        0.01, 0.01, 0.01, 0
                );
            }
        }
    }

    private void applyKnockback(Location center, Player activator) {
        double minRadius = configManager.getAbilityMinRadius();
        double maxRadius = configManager.getAbilityMaxRadius();
        double strength = configManager.getAbilityStrength();

        for (Entity entity : center.getWorld().getNearbyEntities(center, maxRadius, maxRadius, maxRadius)) {
            if (entity == activator) {
                continue;
            }
            if (entity instanceof Player && playerManager.isPlayerWithDragonEgg((Player) entity)) {
                continue;
            }

            double distance = entity.getLocation().distance(center);
            if (distance < minRadius || distance > maxRadius) continue;

            double strengthMultiplier = Math.pow(1.0 - (distance - minRadius) / (maxRadius - minRadius), 2);
            double knockbackStrength = strength * strengthMultiplier;

            Vector direction = entity.getLocation().toVector().subtract(center.toVector());
            if (direction.lengthSquared() == 0) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cZero vector detected in apply Knockback at entity: " + entity.getLocation());
                continue;
            }
            double yDifference = entity.getLocation().getY() - center.getY();

            if (Math.abs(yDifference) <= 2.0) {
                direction.setY(0);
            } else {
                direction.setY(0.2);
            }

            direction = direction.normalize();
            direction.multiply(knockbackStrength);

            entity.setVelocity(direction);
        }
    }

    public void cleanup() {
        for (BukkitRunnable task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
        cooldowns.clear();

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPotionEffect(PotionEffectType.SLOW)) {
                player.removePotionEffect(PotionEffectType.SLOW);
            }
        }
    }
}