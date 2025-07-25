package com.NguyenDevs.superDragonEgg.Manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ParticleManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final PlayerManager playerManager;
    private final Map<Player, BukkitRunnable> playerParticleTasks = new HashMap<>();

    public ParticleManager(JavaPlugin plugin, ConfigManager configManager, PlayerManager playerManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerManager = playerManager;
    }

    public void startParticleTask() {
        stopParticleTasks();

        new BukkitRunnable() {
            @Override
            public void run() {
                playerManager.updatePlayersWithDragonEgg();
                playerParticleTasks.entrySet().removeIf(entry -> {
                    Player player = entry.getKey();
                    if (!playerManager.getPlayersWithDragonEgg().contains(player) || !player.hasPermission("sde.use") || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                        entry.getValue().cancel();
                        return true;
                    }
                    return false;
                });

                for (Player player : new HashSet<>(playerManager.getPlayersWithDragonEgg())) {
                    if (!player.hasPermission("sde.use") || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                        continue;
                    }

                    if (!playerParticleTasks.containsKey(player)) {
                        BukkitRunnable particleTask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (!playerManager.getPlayersWithDragonEgg().contains(player) || !player.hasPermission("sde.use") || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                                    cancel();
                                    playerParticleTasks.remove(player);
                                    return;
                                }

                                Location center = player.getLocation().add(0, 0.5, 0);
                                double maxRadius = configManager.getDebuffRadius();
                                double ticksToMax = configManager.getParticleSpeed() * 20;
                                double radiusIncrement = maxRadius / ticksToMax;

                                if (configManager.isSoundEnabled()) {
                                    player.getWorld().playSound(center, configManager.getSoundType(), configManager.getSoundVolume(), configManager.getSoundPitch());
                                    for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                                        if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(center) <= configManager.getDebuffRadius()) {
                                            nearbyPlayer.playSound(center, configManager.getSoundType(), configManager.getSoundVolume(), configManager.getSoundPitch());
                                        }
                                    }
                                }

                                new BukkitRunnable() {
                                    double currentRadius = 0;
                                    double spiralAngle = 0;

                                    @Override
                                    public void run() {
                                        if (currentRadius > maxRadius || !playerManager.getPlayersWithDragonEgg().contains(player) || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                                            cancel();
                                            return;
                                        }

                                        switch (configManager.getParticlePatternType()) {
                                            case "LINEAR":
                                                spawnLinearParticles(center, currentRadius, maxRadius);
                                                break;
                                            case "SPHERE":
                                                spawnSphereParticles(center, currentRadius, maxRadius);
                                                break;
                                            case "SPIRAL":
                                                spawnSpiralParticles(center, currentRadius, maxRadius, spiralAngle);
                                                spiralAngle += 0.1;
                                                break;
                                        }

                                        currentRadius += radiusIncrement;
                                    }
                                }.runTaskTimer(plugin, 0L, 1L);
                            }
                        };
                        particleTask.runTaskTimer(plugin, 0L, configManager.getParticleDelay() * 20L);
                        playerParticleTasks.put(player, particleTask);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopParticleTasks() {
        for (BukkitRunnable task : playerParticleTasks.values()) {
            task.cancel();
        }
        playerParticleTasks.clear();
    }

    private void spawnLinearParticles(Location center, double currentRadius, double maxRadius) {
        if (currentRadius <= 0) return; // Prevent zero radius causing zero vector
        if (!isLocationValid(center)) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid center location in spawnLinearParticles: " + center);
            return;
        }

        double angleStep = Math.PI / (16 * (1 + currentRadius / maxRadius));
        for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
            double x = currentRadius * Math.cos(angle);
            double z = currentRadius * Math.sin(angle);
            Location particleLoc = center.clone().add(x, 0, z);
            if (!isLocationValid(particleLoc)) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid particle location in spawnLinearParticles: " + particleLoc);
                continue;
            }
            center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.1, 0, 0.1, 0);
            applyKnockback(center, particleLoc, currentRadius, maxRadius);
        }
    }

    private void spawnSphereParticles(Location center, double currentRadius, double maxRadius) {
        if (currentRadius <= 0) return;
        if (!isLocationValid(center)) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid center location in spawnSphereParticles: " + center);
            return;
        }

        int layers = Math.max(8, (int) (currentRadius * 2));
        for (int layer = 0; layer < layers; layer++) {
            double phi = Math.PI * layer / (layers - 1);
            double y = currentRadius * Math.cos(phi);
            double radiusAtLayer = currentRadius * Math.sin(phi);

            int pointsInLayer = Math.max(2, (int) (radiusAtLayer * 8));

            for (int point = 0; point < pointsInLayer; point++) {
                double theta = 2 * Math.PI * point / pointsInLayer;
                double x = radiusAtLayer * Math.cos(theta);
                double z = radiusAtLayer * Math.sin(theta);

                Location particleLoc = center.clone().add(x, y, z);
                if (!isLocationValid(particleLoc)) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid particle location in spawnSphereParticles: " + particleLoc);
                    continue;
                }
                center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.02, 0.02, 0.02, 0);
                applyKnockback(center, particleLoc, currentRadius, maxRadius);
            }
        }
    }

    private void spawnSpiralParticles(Location center, double currentRadius, double maxRadius, double spiralAngle) {
        if (currentRadius <= 0) return;
        if (!isLocationValid(center)) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid center location in spawnSpiralParticles: " + center);
            return;
        }

        int basePoints = 8;

        for (int i = 0; i < basePoints; i++) {
            double baseAngle = (2 * Math.PI * i) / basePoints;

            double spiralRotation = spiralAngle + (currentRadius / maxRadius) * Math.PI * 4;
            double totalAngle = baseAngle + spiralRotation;

            double x = currentRadius * Math.cos(totalAngle);
            double z = currentRadius * Math.sin(totalAngle);

            double y = Math.sin(currentRadius * 0.8 + spiralAngle * 2) * 0.5;

            Location particleLoc = center.clone().add(x, y, z);
            if (!isLocationValid(particleLoc)) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid particle location in spawnSpiralParticles: " + particleLoc);
                continue;
            }
            center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.05, 0.02, 0.05, 0);
            applyKnockback(center, particleLoc, currentRadius, maxRadius);

            if (currentRadius > 1.0) {
                double midRadius = currentRadius * 0.7;
                double midX = midRadius * Math.cos(totalAngle);
                double midZ = midRadius * Math.sin(totalAngle);
                double midY = Math.sin(midRadius * 0.8 + spiralAngle * 2) * 0.3;

                Location midParticleLoc = center.clone().add(midX, midY, midZ);
                if (!isLocationValid(midParticleLoc)) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid mid particle location in spawnSpiralParticles: " + midParticleLoc);
                    continue;
                }
                center.getWorld().spawnParticle(configManager.getParticleType(), midParticleLoc, 1, 0.03, 0.02, 0.03, 0);
                applyKnockback(center, midParticleLoc, currentRadius, maxRadius);
            }
        }
    }

    private void applyKnockback(Location center, Location particleLoc, double currentRadius, double maxRadius) {
        if (!isLocationValid(center) || !isLocationValid(particleLoc)) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid locations in applyKnockback: center=" + center + ", particleLoc=" + particleLoc);
            return;
        }

        double knockbackStrength = configManager.getKnockbackStrength();
        double knockbackVertical = configManager.getKnockbackVertical();
        if (!Double.isFinite(knockbackStrength) || !Double.isFinite(knockbackVertical)) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid knockback values: strength=" + knockbackStrength + ", vertical=" + knockbackVertical);
            return;
        }

        // Skip knockback if both strength and vertical are 0
        if (knockbackStrength == 0.0 && knockbackVertical == 0.0) {
            return;
        }

        for (Entity entity : particleLoc.getWorld().getNearbyEntities(particleLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player && playerManager.isPlayerWithDragonEgg((Player) entity)) &&
                    configManager.getAffectedEntities().contains(entity.getType()) &&
                    !configManager.getAllyEntities().contains(entity.getType())) {
                Vector direction = particleLoc.toVector().subtract(center.toVector());
                if (direction.lengthSquared() == 0) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cZero vector detected in applyKnockback at particleLoc: " + particleLoc);
                    continue;
                }
                direction = direction.normalize();
                if (!isVectorFinite(direction)) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid direction vector in applyKnockback: " + direction);
                    continue;
                }
                direction.multiply(knockbackStrength);
                direction.setY(knockbackVertical);
                entity.setVelocity(direction);
            }
        }
    }

    private boolean isLocationValid(Location location) {
        if (location == null || location.getWorld() == null) return false;
        return Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
    }

    private boolean isVectorFinite(Vector vector) {
        return Double.isFinite(vector.getX()) && Double.isFinite(vector.getY()) && Double.isFinite(vector.getZ());
    }

    public void cleanup() {
        stopParticleTasks();
    }
}