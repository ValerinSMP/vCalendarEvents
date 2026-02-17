package com.marti.vcalendarevents.commands;

import com.marti.vcalendarevents.vCalendarEvents;
import com.marti.vcalendarevents.gui.CalendarGUI;
import com.marti.vcalendarevents.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final vCalendarEvents plugin;

    public MainCommand(vCalendarEvents plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                new CalendarGUI(plugin, player).open();
            } else {
                MessageUtils.sendMessage(sender, "general.player-only");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vcalendarevents.admin")) {
                MessageUtils.sendMessage(sender, "general.no-permission");
                return true;
            }
            plugin.getConfigManager().reloadConfigs();
            plugin.getEventManager().loadEvents();
            MessageUtils.sendMessage(sender, "general.config-reloaded");
            return true;
        }

        if (args[0].equalsIgnoreCase("open")) {
            if (sender instanceof Player player) {
                new CalendarGUI(plugin, player).open();
            } else {
                MessageUtils.sendMessage(sender, "general.player-only");
            }
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("vcalendarevents.admin")) {
                completions.add("reload");
            }
            completions.add("open");
        }
        return completions;
    }
}
