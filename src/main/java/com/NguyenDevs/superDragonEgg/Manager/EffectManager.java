package com.NguyenDevs.superDragonEgg.Manager;

import com.NguyenDevs.superDragonEgg.Manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.GameMode;

import java.util.HashMap;
import java.util.Map;

public class EffectManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerManager playerManager;
    private final Map<Entity, BukkitRunnable> debuffParticleTasks = new HashMap<>();
    private final Map<Entity, Long> allyEffectStartTimes = new HashMap<>();
    private BukkitRunnable effectTask;

    public EffectManager(JavaPlugin plugin, ConfigManager configManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerManager = playerManager;
    }

    public void startEffectTask() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }

        effectTask = new BukkitRunnable() {
            @Override
            public void run() {
                playerManager.updatePlayersWithDragonEgg();

                for (Player player : playerManager.getPlayersWithDragonEgg()) {
                    if (!player.hasPermission("sde.use") || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                        continue;
                    }
                    for (String effect : configManager.getBuffEffects()) {
                        applyEffect(player, effect, 1.0);
                    }
                    if (configManager.isResonanceEnabled()) {
                        Location center = player.getLocation();
                        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                            if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(center) <= configManager.getResonanceRadius() && playerManager.isPlayerWithDragonEgg(nearbyPlayer)) {
                                for (String effect : configManager.getBuffEffects()) {
                                    applyEffect(nearbyPlayer, effect, configManager.getResonanceCoefficient());
                                }
                                // Apply resonance coefficient to debuffs for enemies
                                if (configManager.isDebuffsEnabled()) {
                                    for (Entity entity : player.getWorld().getEntities()) {
                                        if (entity != player &&
                                                entity.getLocation().distance(center) <= configManager.getDebuffRadius() &&
                                                configManager.getAffectedEntities().contains(entity.getType()) &&
                                                !playerManager.isPlayerWithDragonEgg(entity)) {
                                            if (entity instanceof Player) {
                                                Player targetPlayer = (Player) entity;
                                                if (targetPlayer.isOp() ||
                                                        targetPlayer.getGameMode() == GameMode.CREATIVE ||
                                                        targetPlayer.getGameMode() == GameMode.SPECTATOR ||
                                                        targetPlayer.hasPermission("sde.bypass")) {
                                                    continue;
                                                }
                                            }
                                            for (String effect : configManager.getDebuffEffects()) {
                                                applyEffect(entity, effect, configManager.getResonanceCoefficient());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (configManager.isDebuffsEnabled()) {
                        Location center = player.getLocation();
                        for (Entity entity : player.getWorld().getEntities()) {
                            if (entity != player &&
                                    entity.getLocation().distance(center) <= configManager.getDebuffRadius() &&
                                    configManager.getAffectedEntities().contains(entity.getType()) &&
                                    !playerManager.isPlayerWithDragonEgg(entity)) {
                                if (entity instanceof Player) {
                                    Player targetPlayer = (Player) entity;
                                    if (targetPlayer.isOp() ||
                                            targetPlayer.getGameMode() == GameMode.CREATIVE ||
                                            targetPlayer.getGameMode() == GameMode.SPECTATOR ||
                                            targetPlayer.hasPermission("sde.bypass")) {
                                        continue;
                                    }
                                }
                                for (String effect : configManager.getDebuffEffects()) {
                                    applyEffect(entity, effect, 1.0);
                                }
                            }
                        }
                    }

                    if (configManager.isAlliesEnabled()) {
                        Location center = player.getLocation();
                        for (Entity entity : player.getWorld().getEntities()) {
                            if (entity != player && entity.getLocation().distance(center) <= configManager.getDebuffRadius() && configManager.getAllyEntities().contains(entity.getType())) {
                                long currentTime = System.currentTimeMillis();
                                long startTime = allyEffectStartTimes.getOrDefault(entity, 0L);
                                if (currentTime - startTime >= configManager.getAlliesDelay() * 1000L) {
                                    for (String effect : configManager.getBuffEffects()) {
                                        applyEffect(entity, effect, configManager.getAlliesCoefficient());
                                    }
                                    allyEffectStartTimes.put(entity, currentTime);
                                }
                            }
                        }
                    }
                }
            }
        };
        effectTask.runTaskTimer(plugin, 0L, configManager.getBuffInterval() * 20L);
    }

    private void startDebuffParticleTask(Entity entity) {
        if (debuffParticleTasks.containsKey(entity)) {
            debuffParticleTasks.get(entity).cancel();
        }

        BukkitRunnable task = new BukkitRunnable() {
            double ticksRemaining = configManager.getDebuffParticleDuration() * 20.0;

            @Override
            public void run() {
                if (ticksRemaining <= 0 || entity.isDead() || !entity.isValid()) {
                    debuffParticleTasks.remove(entity);
                    cancel();
                    return;
                }

                Location headLoc = entity.getLocation().add(0, entity.getHeight() + 0.5, 0);
                if (configManager.getDebuffParticleType() == Particle.REDSTONE) {
                    entity.getWorld().spawnParticle(configManager.getDebuffParticleType(), headLoc, 3, 0.2, 0.2, 0.2, 0,
                            new Particle.DustOptions(Color.fromRGB(configManager.getDebuffParticleColor().getColor().asRGB()), 1.0f));
                } else {
                    entity.getWorld().spawnParticle(configManager.getDebuffParticleType(), headLoc, 3, 0.2, 0.2, 0.2, 0);
                }

                ticksRemaining -= 5;
            }
        };

        task.runTaskTimer(plugin, 0L, 5L);
        debuffParticleTasks.put(entity, task);
    }

    private void applyEffect(Entity entity, String effectString, double multiplier) {
        try {
            String[] parts = effectString.split(":");
            if (parts.length < 3 || parts.length > 4) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid effect format: §f" + effectString);
                return;
            }

            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
            if (type == null) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid potion effect: §f" + parts[0]);
                return;
            }
            int baseLevel = Integer.parseInt(parts[1]) - 1;
            int duration = Integer.parseInt(parts[2]) * 20;
            int level = baseLevel;
            if (multiplier != 1.0) {
                if (entity instanceof Player && playerManager.isPlayerWithDragonEgg(entity)) {
                    level = baseLevel + (int) multiplier;
                    if (level < 0) level = 0;
                } else {
                    duration = (int) (duration * multiplier);
                }
            }

            DyeColor glowColor = null;
            if (type == PotionEffectType.GLOWING && parts.length == 4) {
                try {
                    glowColor = DyeColor.valueOf(parts[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid color for §eGLOWING effect: §f" + parts[3] + "§c. Using default glow.");
                }
            }

            boolean isBuffEffect = (entity instanceof Player && playerManager.isPlayerWithDragonEgg(entity))
                    || configManager.getAllyEntities().contains(entity.getType());

            if (entity instanceof Player) {
                PotionEffect potionEffect = new PotionEffect(
                        type,
                        duration,
                        level,
                        isBuffEffect,
                        false
                );
                ((Player) entity).addPotionEffect(potionEffect);

                if (type == PotionEffectType.GLOWING && glowColor != null) {
                    playerManager.startGlowEffect(entity, glowColor, duration);
                }
            } else if (entity instanceof LivingEntity) {
                PotionEffect potionEffect = new PotionEffect(
                        type,
                        duration,
                        level,
                        isBuffEffect,
                        false
                );
                ((LivingEntity) entity).addPotionEffect(potionEffect);

                if (configManager.isDebuffParticleEnabled() && !playerManager.isPlayerWithDragonEgg(entity) && !configManager.getAllyEntities().contains(entity.getType())) {
                    startDebuffParticleTask(entity);
                }
                if (type == PotionEffectType.GLOWING && glowColor != null && entity instanceof Player) {
                    playerManager.startGlowEffect(entity, glowColor, duration);
                }
            }
        } catch (NumberFormatException e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §deffect: §f" + effectString);
        }
    }

    public void cleanup() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }

        for (BukkitRunnable task : debuffParticleTasks.values()) {
            task.cancel();
        }
        debuffParticleTasks.clear();

        allyEffectStartTimes.clear();
        playerManager.cleanupGlowTasks();
    }
}