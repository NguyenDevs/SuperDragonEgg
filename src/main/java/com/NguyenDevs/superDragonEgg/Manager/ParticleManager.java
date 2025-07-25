package com.NguyenDevs.superDragonEgg.Manager;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
        // Stop all existing tasks
        stopParticleTasks();

        // Start a task to periodically check and manage player-specific tasks
        new BukkitRunnable() {
            @Override
            public void run() {
                // Update players with dragon egg
                playerManager.updatePlayersWithDragonEgg();

                // Remove tasks for players who no longer qualify
                playerParticleTasks.entrySet().removeIf(entry -> {
                    Player player = entry.getKey();
                    if (!playerManager.getPlayersWithDragonEgg().contains(player) || !player.hasPermission("sde.use") || !configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                        entry.getValue().cancel();
                        return true;
                    }
                    return false;
                });

                // Start tasks for new qualifying players
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
                                    double currentHeight = 0;

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
        }.runTaskTimer(plugin, 0L, 20L); // Run every second to check for new players
    }

    public void stopParticleTasks() {
        for (BukkitRunnable task : playerParticleTasks.values()) {
            task.cancel();
        }
        playerParticleTasks.clear();
    }

    private void spawnLinearParticles(Location center, double currentRadius, double maxRadius) {
        double angleStep = Math.PI / (16 * (1 + currentRadius / maxRadius));
        for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
            double x = currentRadius * Math.cos(angle);
            double z = currentRadius * Math.sin(angle);
            Location particleLoc = center.clone().add(x, 0, z);
            center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.1, 0, 0.1, 0);
        }
    }

    private void spawnSphereParticles(Location center, double currentRadius, double maxRadius) {
        if (currentRadius <= 0) return;

        int layers = Math.max(8, (int) (currentRadius * 2));
        for (int layer = 0; layer < layers; layer++) {
            double phi = Math.PI * layer / (layers - 1);
            double y = currentRadius * Math.cos(phi);
            double radiusAtLayer = currentRadius * Math.sin(phi);

            int pointsInLayer = Math.max(8, (int) (radiusAtLayer * 8));

            for (int point = 0; point < pointsInLayer; point++) {
                double theta = 2 * Math.PI * point / pointsInLayer;
                double x = radiusAtLayer * Math.cos(theta);
                double z = radiusAtLayer * Math.sin(theta);

                Location particleLoc = center.clone().add(x, y, z);
                center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.02, 0.02, 0.02, 0);
            }
        }
    }

    private void spawnSpiralParticles(Location center, double currentRadius, double maxRadius, double spiralAngle) {
        if (currentRadius <= 0) return;

        int basePoints = 8;

        for (int i = 0; i < basePoints; i++) {
            double baseAngle = (2 * Math.PI * i) / basePoints;

            double spiralRotation = spiralAngle + (currentRadius / maxRadius) * Math.PI * 4;
            double totalAngle = baseAngle + spiralRotation;

            double x = currentRadius * Math.cos(totalAngle);
            double z = currentRadius * Math.sin(totalAngle);

            double y = Math.sin(currentRadius * 0.8 + spiralAngle * 2) * 0.5;

            Location particleLoc = center.clone().add(x, y, z);
            center.getWorld().spawnParticle(configManager.getParticleType(), particleLoc, 1, 0.05, 0.02, 0.05, 0);

            if (currentRadius > 1.0) {
                double midRadius = currentRadius * 0.7;
                double midX = midRadius * Math.cos(totalAngle);
                double midZ = midRadius * Math.sin(totalAngle);
                double midY = Math.sin(midRadius * 0.8 + spiralAngle * 2) * 0.3;

                Location midParticleLoc = center.clone().add(midX, midY, midZ);
                center.getWorld().spawnParticle(configManager.getParticleType(), midParticleLoc, 1, 0.03, 0.02, 0.03, 0);
            }
        }
    }

    public void cleanup() {
        stopParticleTasks();
    }
}