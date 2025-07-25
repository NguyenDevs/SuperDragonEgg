package com.NguyenDevs.superDragonEgg.Manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DragonEggRespawnManager implements Listener {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Map<String, Integer> dragonKillCounts = new HashMap<>();
    private final Random random = new Random();

    public DragonEggRespawnManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) {
            return;
        }

        World world = event.getEntity().getWorld();
        if (!configManager.getEnableWorlds().contains(world.getName())) {
            return;
        }

        if (!configManager.isRespawnEggEnabled()) {
            return;
        }

        String worldName = world.getName();
        int killCount = dragonKillCounts.getOrDefault(worldName, 0) + 1;
        dragonKillCounts.put(worldName, killCount);

        if (killCount < 2) {
            return;
        }

        int chance = configManager.getRespawnEggChance();
        if (random.nextInt(100) < chance) {
            Location portalLocation = new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
            portalLocation.getBlock().setType(Material.DRAGON_EGG);
            Bukkit.getConsoleSender().sendMessage(configManager.getMessages().getString("dragon-egg-spawned", "§d[§5SuperDragonEgg§d] §aDragon egg spawned at " + portalLocation.toString()));
        } else {
            Bukkit.getConsoleSender().sendMessage(configManager.getMessages().getString("dragon-egg-not-spawned", "§d[§5SuperDragonEgg§d] §7Dragon egg did not spawn (chance: " + chance + "%)"));
        }
    }

    public void cleanup() {
        dragonKillCounts.clear();
    }
}