package com.NguyenDevs.superDragonEgg.Manager;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messages;

    private String particlePatternType;
    private int buffInterval;
    private int debuffRadius;
    private double particleSpeed;
    private int particleDelay;
    private boolean enableParticles;
    private boolean enableDebuffs;
    private boolean enableSound;
    private boolean debuffParticleEnabled;
    private Particle debuffParticleType;
    private double debuffParticleDuration;
    private DyeColor debuffParticleColor;
    private Sound soundType;
    private float soundVolume;
    private float soundPitch;
    private List<String> buffEffects;
    private List<String> debuffEffects;
    private List<EntityType> affectedEntities;
    private Particle particleType;
    private List<String> enableWorlds;
    private boolean respawnEggEnabled;
    private int respawnEggChance;
    private boolean alliesEnabled;
    private int alliesDelay;
    private double alliesCoefficient;
    private List<EntityType> allyEntities;
    private boolean resonanceEnabled;
    private int resonanceRadius;
    private double resonanceCoefficient;
    private double knockbackStrength;
    private double knockbackVertical;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        plugin.saveDefaultConfig();
        loadMessages();
        plugin.reloadConfig();
        loadConfig();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §aCreated new messages.yml file");
        }
        try {
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §aSuccessfully loaded messages.yml");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cError loading messages.yml: " + e.getMessage());
        }
    }

    public void saveMessages() {
        if (messages == null || messagesFile == null) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cCannot save messages.yml: messages or messagesFile is null");
            return;
        }

        try {
            messages.save(messagesFile);
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §aSuccessfully saved messages.yml");
        } catch (IOException e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cCould not save messages.yml: " + e.getMessage());
        }
    }

    private void loadConfig() {
        try {
            FileConfiguration config = plugin.getConfig();

            buffInterval = config.getInt("buff-interval", 5);

            enableDebuffs = config.getBoolean("debuff.enable", true);
            debuffRadius = config.getInt("debuff.radius", 20);

            enableParticles = config.getBoolean("particle.enable", true);
            particlePatternType = config.getString("particle.type", "LINEAR").toUpperCase();
            if (!particlePatternType.equals("LINEAR") && !particlePatternType.equals("SPHERE") && !particlePatternType.equals("SPIRAL")) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid particle-type in config: §f" + particlePatternType + "§c. Using default: §bLINEAR");
                particlePatternType = "LINEAR";
            }
            String particleEffect = config.getString("particle.effect", "WITCH").toUpperCase();
            try {
                particleType = Particle.valueOf(particleEffect);
            } catch (IllegalArgumentException e) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid particle effect: §f" + particleEffect + "§c. Using default: §5SCULK_SOUL");
                particleType = Particle.SCULK_SOUL;
            }
            particleSpeed = config.getDouble("particle.speed", 1.0);
            particleDelay = config.getInt("particle.delay", 5);
            knockbackStrength = config.getDouble("particle.knockback-strength", 0.3);
            knockbackVertical = config.getDouble("particle.knockback-vertical", 0.1);

            debuffParticleEnabled = config.getBoolean("debuff-particle.enable", true);
            String debuffParticleName = config.getString("debuff-particle.type", "REDSTONE").toUpperCase();
            try {
                debuffParticleType = Particle.valueOf(debuffParticleName);
            } catch (IllegalArgumentException e) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid debuff particle type: §f" + debuffParticleName + "§c. Using default: §4REDSTONE");
                debuffParticleType = Particle.REDSTONE;
            }
            debuffParticleDuration = config.getDouble("debuff-particle.duration", 2.0);
            String debuffColorName = config.getString("debuff-particle.color", "PURPLE").toUpperCase();
            try {
                debuffParticleColor = DyeColor.valueOf(debuffColorName);
            } catch (IllegalArgumentException e) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid debuff particle color: §f" + debuffColorName + "§c. Using default: §4PURPLE");
                debuffParticleColor = DyeColor.PURPLE;
            }

            enableSound = config.getBoolean("sound.enable", true);
            String soundName = config.getString("sound.name", "BLOCK_AMETHYST_BLOCK_RESONATE").toUpperCase();
            try {
                soundType = Sound.valueOf(soundName);
            } catch (IllegalArgumentException e) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid sound name: §f" + soundName + "§c. Using default: §dBLOCK_AMETHYST_BLOCK_RESONATE");
                soundType = Sound.BLOCK_AMETHYST_BLOCK_RESONATE;
            }
            soundVolume = (float) config.getDouble("sound.volume", 1.0);
            soundPitch = (float) config.getDouble("sound.pitch", 1.0);

            buffEffects = config.getStringList("buff-effects");
            debuffEffects = config.getStringList("debuff-effects");

            affectedEntities = new ArrayList<>();
            List<String> entityList = config.getStringList("affected-entities");
            for (String entity : entityList) {
                try {
                    if (entity.equalsIgnoreCase("PLAYER")) {
                        affectedEntities.add(EntityType.PLAYER);
                    } else {
                        affectedEntities.add(EntityType.valueOf(entity.toUpperCase()));
                    }
                } catch (IllegalArgumentException e) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid entity type: §f" + entity);
                }
            }

            enableWorlds = config.getStringList("enable-worlds");
            if (enableWorlds.isEmpty()) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cNo worlds specified in enable-worlds, defaulting to all worlds");
            }

            respawnEggEnabled = config.getBoolean("respawn-egg.enable", true);
            respawnEggChance = config.getInt("respawn-egg.chance", 40);
            if (respawnEggChance < 0 || respawnEggChance > 100) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid respawn-egg chance: §f" + respawnEggChance + "§c. Using default: §b40");
                respawnEggChance = 40;
            }

            alliesEnabled = config.getBoolean("allies.enable", true);
            alliesDelay = config.getInt("allies.delay", 10);
            alliesCoefficient = config.getDouble("allies.coefficient", 50.0) / 100.0;
            if (alliesCoefficient < 0 || alliesCoefficient > 1) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid allies coefficient: §f" + alliesCoefficient + "§c. Using default: §b0.5");
                alliesCoefficient = 0.5;
            }
            allyEntities = new ArrayList<>();
            List<String> allyList = config.getStringList("allies.list");
            for (String entity : allyList) {
                try {
                    allyEntities.add(EntityType.valueOf(entity.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid ally entity type: §f" + entity);
                }
            }

            resonanceEnabled = config.getBoolean("resonance.enable", true);
            resonanceRadius = config.getInt("resonance.radius", 5);
            resonanceCoefficient = config.getDouble("resonance.coefficient", 2.0);
            if (resonanceCoefficient < 0) {
                Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid resonance coefficient: §f" + resonanceCoefficient + "§c. Using default: §b2.0");
                resonanceCoefficient = 2.0;
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cError loading config: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        try {
            plugin.reloadConfig();
            loadMessages();
            loadConfig();
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §aConfiguration and messages reloaded successfully!");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cError reloading configuration: " + e.getMessage());
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getParticlePatternType() {
        return particlePatternType;
    }

    public int getBuffInterval() {
        return buffInterval;
    }

    public int getDebuffRadius() {
        return debuffRadius;
    }

    public double getParticleSpeed() {
        return particleSpeed;
    }

    public int getParticleDelay() {
        return particleDelay;
    }

    public double getKnockbackStrength() {
        return knockbackStrength;
    }

    public double getKnockbackVertical() {
        return knockbackVertical;
    }

    public boolean isParticlesEnabled() {
        return enableParticles;
    }

    public boolean isDebuffsEnabled() {
        return enableDebuffs;
    }

    public boolean isSoundEnabled() {
        return enableSound;
    }

    public boolean isDebuffParticleEnabled() {
        return debuffParticleEnabled;
    }

    public Particle getDebuffParticleType() {
        return debuffParticleType;
    }

    public double getDebuffParticleDuration() {
        return debuffParticleDuration;
    }

    public DyeColor getDebuffParticleColor() {
        return debuffParticleColor;
    }

    public Sound getSoundType() {
        return soundType;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public List<String> getBuffEffects() {
        return buffEffects;
    }

    public List<String> getDebuffEffects() {
        return debuffEffects;
    }

    public List<EntityType> getAffectedEntities() {
        return affectedEntities;
    }

    public Particle getParticleType() {
        return particleType;
    }

    public List<String> getEnableWorlds() {
        return enableWorlds;
    }

    public boolean isRespawnEggEnabled() {
        return respawnEggEnabled;
    }

    public int getRespawnEggChance() {
        return respawnEggChance;
    }

    public boolean isAlliesEnabled() {
        return alliesEnabled;
    }

    public int getAlliesDelay() {
        return alliesDelay;
    }

    public double getAlliesCoefficient() {
        return alliesCoefficient;
    }

    public List<EntityType> getAllyEntities() {
        return allyEntities;
    }

    public boolean isResonanceEnabled() {
        return resonanceEnabled;
    }

    public int getResonanceRadius() {
        return resonanceRadius;
    }

    public double getResonanceCoefficient() {
        return resonanceCoefficient;
    }

    // Ability configuration methods
    public boolean isAbilityEnabled() {
        FileConfiguration config = plugin.getConfig();
        boolean enabled = config.getBoolean("ability.enable", true);
        if (!config.contains("ability.enable")) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cMissing ability.enable in config, using default: §btrue");
        }
        return enabled;
    }

    public double getAbilityShieldRadius() {
        FileConfiguration config = plugin.getConfig();
        double radius = config.getDouble("ability.shield-radius", 3.0);
        if (!config.contains("ability.shield-radius") || radius <= 0) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.shield-radius in config, using default: §b3.0");
            radius = 3.0;
        }
        return radius;
    }

    public double getAbilityMinRadius() {
        FileConfiguration config = plugin.getConfig();
        double minRadius = config.getDouble("ability.min-radius", 3.0);
        if (!config.contains("ability.min-radius") || minRadius < 0) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.min-radius in config, using default: §b3.0");
            minRadius = 3.0;
        }
        return minRadius;
    }

    public double getAbilityMaxRadius() {
        FileConfiguration config = plugin.getConfig();
        double maxRadius = config.getDouble("ability.max-radius", 5.0);
        if (!config.contains("ability.max-radius") || maxRadius <= getAbilityMinRadius()) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.max-radius in config, using default: §b5.0");
            maxRadius = 5.0;
        }
        return maxRadius;
    }

    public double getAbilityDuration() {
        FileConfiguration config = plugin.getConfig();
        double duration = config.getDouble("ability.duration", 5.0);
        if (!config.contains("ability.duration") || duration <= 0) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.duration in config, using default: §b5.0");
            duration = 5.0;
        }
        return duration;
    }

    public double getAbilityCooldown() {
        FileConfiguration config = plugin.getConfig();
        double cooldown = config.getDouble("ability.cooldown", 10.0);
        if (!config.contains("ability.cooldown") || cooldown < 0) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.cooldown in config, using default: §b10.0");
            cooldown = 10.0;
        }
        return cooldown;
    }

    public Sound getAbilitySound() {
        String soundName = plugin.getConfig().getString("ability.sound-name", "ENTITY_ENDER_DRAGON_FLAP").toUpperCase();
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid ability sound name: §f" + soundName + "§c. Using default: §dENTITY_ENDER_DRAGON_FLAP");
            return Sound.ENTITY_ENDER_DRAGON_FLAP;
        }
    }
    public double getAbilityStrength() {
        FileConfiguration config = plugin.getConfig();
        double strength = config.getDouble("ability.strength", 0.3);
        if (!config.contains("ability.strength") || strength < 0) {
            Bukkit.getConsoleSender().sendMessage("§d[§5SuperDragonEgg§d] §cInvalid or missing ability.strength in config, using default: §b0.3");
            strength = 0.3;
        }
        return strength;
    }
}