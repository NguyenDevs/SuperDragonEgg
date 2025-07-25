package com.NguyenDevs.superDragonEgg.Command;

import com.NguyenDevs.superDragonEgg.SuperDragonEgg;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {

    private final SuperDragonEgg plugin;

    public ReloadCommand(SuperDragonEgg plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("sde")) {
            return false;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            String message = plugin.getConfigManager().getMessages().getString("messages.usage");
            String prefix = plugin.getConfigManager().getMessages().getString("messages.prefix");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + message));
            return true;
        }

        if (!sender.hasPermission("sde.reload")) {
            String message = plugin.getConfigManager().getMessages().getString("messages.no-permission");
            String prefix = plugin.getConfigManager().getMessages().getString("messages.prefix");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + message));
            return true;
        }

        plugin.reloadPlugin();
        String message = plugin.getConfigManager().getMessages().getString("messages.reloaded");
        String prefix = plugin.getConfigManager().getMessages().getString("messages.prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + " " + message));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("sde")) {
            if (args.length == 1) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    completions.add("reload");
                }
            }
        }

        return completions;
    }
}