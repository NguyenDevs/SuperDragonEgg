package com.NguyenDevs.superDragonEgg;

import com.NguyenDevs.superDragonEgg.Command.ReloadCommand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuperDragonEgg extends JavaPlugin implements Listener {

    private Set<Player> playersWithDragonEgg = new HashSet<>();
    private int buffInterval;
    private int debuffRadius;
    private double particleSpeed;
    private int particleDelay;
    private boolean enableParticles;
    private boolean enableDebuffs;
    private boolean enableSound;
    private Sound soundType;
    private List<String> buffEffects;
    private List<String> debuffEffects;
    private List<EntityType> affectedEntities;
    private BukkitRunnable particleTask;
    private Particle particleType;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("sde").setExecutor(new ReloadCommand(this));
        startEffectTask();
        if (enableParticles) {
            startParticleTask();
        }
        checkOnlinePlayers();
    }

    public void loadConfig() {
        reloadConfig();
        buffInterval = getConfig().getInt("buff-interval", 60);
        debuffRadius = getConfig().getInt("debuff-radius", 10);
        particleSpeed = getConfig().getDouble("particle-speed", 2.0);
        particleDelay = getConfig().getInt("particle-delay", 5);
        enableParticles = getConfig().getBoolean("enable-particles", true);
        enableDebuffs = getConfig().getBoolean("enable-debuffs", true);
        enableSound = getConfig().getBoolean("enable-sound", true);

        // Load particle effect from config
        String particleEffect = getConfig().getString("particle-effect", "WATER_WAKE").toUpperCase();
        try {
            particleType = Particle.valueOf(particleEffect);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid particle effect in config: " + particleEffect + ". Using default: SPEEL_WITCH");
            particleType = Particle.SPELL_WITCH;
        }

        // Load sound effect from config
        String soundName = getConfig().getString("sound-name", "BLOCK_AMETHYST_BLOCK_CHIME").toUpperCase();
        try {
            soundType = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid sound name in config: " + soundName + ". Using default: BLOCK_AMETHYST_BLOCK_CHIME");
            soundType = Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        }

        buffEffects = getConfig().getStringList("buff-effects");
        debuffEffects = getConfig().getStringList("debuff-effects");

        affectedEntities = new ArrayList<>();
        List<String> entityList = getConfig().getStringList("affected-entities");
        for (String entity : entityList) {
            try {
                if (entity.equalsIgnoreCase("PLAYER")) {
                    affectedEntities.add(EntityType.PLAYER);
                } else {
                    affectedEntities.add(EntityType.valueOf(entity.toUpperCase()));
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid entity type in config: " + entity);
            }
        }
    }

    private void checkOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasDragonEgg(player)) {
                playersWithDragonEgg.add(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (hasDragonEgg(player)) {
            playersWithDragonEgg.add(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playersWithDragonEgg.remove(event.getPlayer());
    }

    private boolean hasDragonEgg(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG);
    }

    private void startEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                playersWithDragonEgg.clear();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (hasDragonEgg(player)) {
                        playersWithDragonEgg.add(player);
                    }
                }

                for (Player player : playersWithDragonEgg) {
                    for (String effect : buffEffects) {
                        applyEffect(player, effect);
                    }

                    if (enableDebuffs) {
                        Location center = player.getLocation();
                        for (Entity entity : player.getWorld().getEntities()) {
                            if (entity != player && entity.getLocation().distance(center) <= debuffRadius && affectedEntities.contains(entity.getType())) {
                                for (String effect : debuffEffects) {
                                    applyEffect(entity, effect);
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, buffInterval * 20L);
    }

    private void startParticleTask() {
        if (particleTask != null) {
            particleTask.cancel();
        }

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : new HashSet<>(playersWithDragonEgg)) { // Copy set to avoid ConcurrentModificationException
                    if (!playersWithDragonEgg.contains(player)) continue;
                    Location center = player.getLocation().add(0, 0.5, 0);
                    double maxRadius = debuffRadius;
                    double ticksToMax = particleSpeed * 20;
                    double radiusIncrement = maxRadius / ticksToMax;

                    // Play sound if enabled
                    if (enableSound) {
                        player.getWorld().playSound(center, soundType, 1.0f, 1.0f);
                        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                            if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(center) <= debuffRadius) {
                                nearbyPlayer.playSound(center, soundType, 1.0f, 1.0f);
                            }
                        }
                    }

                    new BukkitRunnable() {
                        double currentRadius = 0;

                        @Override
                        public void run() {
                            if (currentRadius > maxRadius || !playersWithDragonEgg.contains(player)) {
                                cancel();
                                return;
                            }

                            double angleStep = Math.PI / (16 * (1 + currentRadius / maxRadius)); // Adjust density
                            for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
                                double x = currentRadius * Math.cos(angle);
                                double z = currentRadius * Math.sin(angle);
                                Location particleLoc = center.clone().add(x, 0, z);
                                player.getWorld().spawnParticle(particleType, particleLoc, 1, 0.1, 0, 0.1, 0);
                            }

                            currentRadius += radiusIncrement;
                        }
                    }.runTaskTimer(SuperDragonEgg.this, 0L, 1L).getTaskId(); // Start new task for each wave
                }
            }
        };
        if (enableParticles) {
            particleTask.runTaskTimer(this, 0L, particleDelay * 20L);
        }
    }

    private void applyEffect(Entity entity, String effectString) {
        try {
            String[] parts = effectString.split(":");
            if (parts.length != 3) {
                getLogger().warning("Invalid effect format: " + effectString);
                return;
            }

            PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase());
            if (type == null) {
                getLogger().warning("Invalid potion effect: " + parts[0]);
                return;
            }

            int level = Integer.parseInt(parts[1]) - 1;
            int duration = Integer.parseInt(parts[2]) * 20;

            if (entity instanceof Player) {
                ((Player) entity).addPotionEffect(new PotionEffect(type, duration, level, false, false));
            } else if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(type, duration, level, false, false));
                // Add red particle effect above entity head for debuff
                Location headLoc = entity.getLocation().add(0, entity.getHeight() + 0.5, 0);
                entity.getWorld().spawnParticle(Particle.REDSTONE, headLoc, 5, 0.2, 0.2, 0.2, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 1.0f));
            }
        } catch (NumberFormatException e) {
            getLogger().warning("Invalid number format in effect: " + effectString);
        }
    }

    public void reloadPlugin() {
        loadConfig();
        if (enableParticles && (particleTask == null || particleTask.isCancelled())) {
            startParticleTask();
        } else if (!enableParticles && particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
    }
}