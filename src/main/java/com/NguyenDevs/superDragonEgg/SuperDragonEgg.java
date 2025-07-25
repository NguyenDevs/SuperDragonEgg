package com.NguyenDevs.superDragonEgg;

import com.NguyenDevs.superDragonEgg.Abilities.DragonShieldAbility;
import com.NguyenDevs.superDragonEgg.Command.ReloadCommand;
import com.NguyenDevs.superDragonEgg.Listener.PlayerListener;
import com.NguyenDevs.superDragonEgg.Manager.ConfigManager;
import com.NguyenDevs.superDragonEgg.Manager.DragonEggRespawnManager;
import com.NguyenDevs.superDragonEgg.Manager.EffectManager;
import com.NguyenDevs.superDragonEgg.Manager.PlayerManager;
import com.NguyenDevs.superDragonEgg.Manager.ParticleManager;
import com.NguyenDevs.superDragonEgg.Utils.LogoUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class SuperDragonEgg extends JavaPlugin {

    private ConfigManager configManager;
    private PlayerManager playerManager;
    private EffectManager effectManager;
    private ParticleManager particleManager;
    private DragonEggRespawnManager dragonEggRespawnManager;
    private DragonShieldAbility dragonShieldAbility;

    @Override
    public void onEnable() {
        LogoUtil.printLogo(this);

        configManager = new ConfigManager(this);
        playerManager = new PlayerManager(this, configManager);
        effectManager = new EffectManager(this, configManager, playerManager);
        particleManager = new ParticleManager(this, configManager, playerManager);
        dragonEggRespawnManager = new DragonEggRespawnManager(this, configManager);
        dragonShieldAbility = new DragonShieldAbility(this, configManager, playerManager);

        configManager.initialize();

        getServer().getPluginManager().registerEvents(new PlayerListener(playerManager), this);
        getServer().getPluginManager().registerEvents(dragonEggRespawnManager, this);

        Objects.requireNonNull(getCommand("sde")).setExecutor(new ReloadCommand(this));

        effectManager.startEffectTask();
        if (configManager.isParticlesEnabled()) {
            particleManager.startParticleTask();
        }
        playerManager.checkOnlinePlayers();

        Bukkit.getConsoleSender().sendMessage(configManager.getMessages().getString("plugin-enabled", "§d[§5SuperDragonEgg§d] §aPlugin enabled successfully!"));
    }

    @Override
    public void onDisable() {
        if (effectManager != null) {
            effectManager.cleanup();
        }
        if (particleManager != null) {
            particleManager.cleanup();
        }
        if (dragonEggRespawnManager != null) {
            dragonEggRespawnManager.cleanup();
        }
        if (dragonShieldAbility != null) {
            dragonShieldAbility.cleanup();
        }
        Bukkit.getConsoleSender().sendMessage(configManager.getMessages().getString("plugin-disabled", "§d[§5SuperDragonEgg§d] §cPlugin disabled!"));
    }

    public void reloadPlugin() {
        if (effectManager != null) {
            effectManager.cleanup();
        }
        if (particleManager != null) {
            particleManager.cleanup();
        }
        if (dragonEggRespawnManager != null) {
            dragonEggRespawnManager.cleanup();
        }
        if (dragonShieldAbility != null) {
            dragonShieldAbility.cleanup();
        }

        configManager.reloadConfig();

        playerManager.checkOnlinePlayers();

        effectManager.startEffectTask();
        if (configManager.isParticlesEnabled()) {
            particleManager.startParticleTask();
        } else {
            particleManager.stopParticleTasks();
        }

        Bukkit.getConsoleSender().sendMessage(configManager.getMessages().getString("plugin-reloaded", "§d[§5SuperDragonEgg§d] §aPlugin reloaded successfully!"));
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}