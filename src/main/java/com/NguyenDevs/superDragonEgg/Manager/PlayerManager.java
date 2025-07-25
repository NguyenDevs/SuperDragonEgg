package com.NguyenDevs.superDragonEgg.Manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private final Set<Player> playersWithDragonEgg = new HashSet<>();
    private final Map<Entity, BukkitRunnable> glowTasks = new HashMap<>();

    public PlayerManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void checkOnlinePlayers() {
        playersWithDragonEgg.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasDragonEgg(player) && configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                playersWithDragonEgg.add(player);
            }
        }
    }

    public void addPlayer(Player player) {
        if (hasDragonEgg(player) && configManager.getEnableWorlds().contains(player.getWorld().getName())) {
            playersWithDragonEgg.add(player);
        }
    }

    public void removePlayer(Player player) {
        playersWithDragonEgg.remove(player);
        if (glowTasks.containsKey(player)) {
            glowTasks.get(player).cancel();
            glowTasks.remove(player);
        }
        removeFromGlowTeam(player);
    }

    public boolean hasDragonEgg(Player player) {
        if (!player.hasPermission("sde.use")) {
            return false;
        }

        // Chỉ kiểm tra trong hotbar (slot 0-8)
        for (int i = 0; i <= 8; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.DRAGON_EGG) {
                return true;
            }
        }
        return false;
    }

    public boolean isPlayerWithDragonEgg(Entity entity) {
        if (entity instanceof Player) {
            Player player = (Player) entity;
            return hasDragonEgg(player) && configManager.getEnableWorlds().contains(player.getWorld().getName());
        }
        return false;
    }

    public void updatePlayersWithDragonEgg() {
        playersWithDragonEgg.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasDragonEgg(player) && configManager.getEnableWorlds().contains(player.getWorld().getName())) {
                playersWithDragonEgg.add(player);
            }
        }
    }

    private ChatColor dyeColorToChatColor(DyeColor dyeColor) {
        switch (dyeColor) {
            case RED: return ChatColor.RED;
            case BLUE: return ChatColor.BLUE;
            case GREEN: return ChatColor.GREEN;
            case YELLOW: return ChatColor.YELLOW;
            case PURPLE: return ChatColor.LIGHT_PURPLE;
            case ORANGE: return ChatColor.GOLD;
            case PINK: return ChatColor.LIGHT_PURPLE;
            case CYAN: return ChatColor.AQUA;
            case LIME: return ChatColor.GREEN;
            case GRAY: return ChatColor.GRAY;
            case LIGHT_GRAY: return ChatColor.GRAY;
            case BROWN: return ChatColor.GOLD;
            case LIGHT_BLUE: return ChatColor.BLUE;
            case MAGENTA: return ChatColor.LIGHT_PURPLE;
            case BLACK: return ChatColor.BLACK;
            case WHITE: return ChatColor.WHITE;
            default: return ChatColor.WHITE;
        }
    }

    public void addToGlowTeam(Entity entity, DyeColor color) {
        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamName = "glow_" + color.name().toLowerCase();
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            team.setColor(dyeColorToChatColor(color));
        }

        team.addEntry(player.getName());

        // Đặt player vào team để có glowing effect
        player.setScoreboard(scoreboard);
        player.setGlowing(true);
    }

    public void removeFromGlowTeam(Entity entity) {
        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("glow_")) {
                team.removeEntry(player.getName());
            }
        }

        // Tắt glowing effect
        player.setGlowing(false);
    }

    public void startGlowEffect(Entity entity, DyeColor glowColor, int duration) {
        if (!(entity instanceof Player)) return;

        if (glowTasks.containsKey(entity)) {
            glowTasks.get(entity).cancel();
            removeFromGlowTeam(entity);
        }

        addToGlowTeam(entity, glowColor);

        BukkitRunnable glowTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeFromGlowTeam(entity);
                glowTasks.remove(entity);
            }
        };

        glowTask.runTaskLater(plugin, duration);
        glowTasks.put(entity, glowTask);
    }

    public void cleanupGlowTasks() {
        for (Map.Entry<Entity, BukkitRunnable> entry : glowTasks.entrySet()) {
            entry.getValue().cancel();
            removeFromGlowTeam(entry.getKey());
        }
        glowTasks.clear();
    }

    public Set<Player> getPlayersWithDragonEgg() {
        return new HashSet<>(playersWithDragonEgg);
    }
}