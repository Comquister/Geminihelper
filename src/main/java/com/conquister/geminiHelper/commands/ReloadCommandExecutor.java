package com.conquister.geminiHelper.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

import com.conquister.geminiHelper.GeminiHelper;

public class ReloadCommandExecutor implements CommandExecutor {
    private final GeminiHelper plugin;

    public ReloadCommandExecutor(GeminiHelper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("geminihelper.admin")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando.");
            return true;
        }

        plugin.reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Configurações do GeminiHelper recarregadas com sucesso!");
        return true;
    }
}