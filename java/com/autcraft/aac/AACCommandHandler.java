package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AACCommandHandler implements CommandExecutor, TabCompleter {
    private final AACPlugin plugin;

    public AACCommandHandler(AACPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        // No arguments provided - get help text
        if (args.length == 0) {
            // Error: Invalid permission
            if (!commandSender.hasPermission("aac.help")) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            commandSender.sendMessage(Component.text(plugin.getConfig().getString("settings.helptext")).color(TextColor.color(60, 180, 180)));
            return true;
        }


        // Reload the config and re-initalize the panel items
        if (args[0].equalsIgnoreCase("reload")) {
            // Error: Invalid permission
            if (!commandSender.hasPermission("aac.reload")) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // reload all the things
            plugin.reload();

            commandSender.sendMessage(Component.text(plugin.getConfig().getString("settings.reloadtext")).color(TextColor.color(60, 180, 180)));
            return true;
        }


        // Get the knowledge book!
        if (args[0].equalsIgnoreCase("get")) {
            // Error: Invalid permission
            if (!commandSender.hasPermission("aac.get")) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // If player
            if (commandSender instanceof Player player) {
                InventoryGUI inventoryGUI = plugin.getInventoryGUI();

                // Put the item into the player's inventory that will trigger the AAC GUI
                player.getInventory().addItem(inventoryGUI.getTool());
                plugin.debug("Gave AAC tool to " + player.getName());
            } else {
                commandSender.sendMessage(plugin.errorMessage("error_no_console"));
                return true;
            }
        }

        // If the player is trying to give the book to another player
        if (args[0].equalsIgnoreCase("give")) {
            // Error: Invalid permission
            if (!commandSender.hasPermission("aac.give")) {
                commandSender.sendMessage(plugin.errorMessage("error_no_permission"));
                return true;
            }

            // Error: /aac give command ran but no player provided.
            if (args.length == 1) {
                commandSender.sendMessage(plugin.errorMessage("error_player_not_provided"));
                return false;
            }

            // Error: /aac give <player> command ran but player is not online
            if (plugin.getServer().getPlayer(args[1]) == null) {
                commandSender.sendMessage(plugin.errorMessage("error_player_not_online"));
                return true;
            }

            // Get player based on args[1] given in command
            Player player = plugin.getServer().getPlayer(args[1]);
            InventoryGUI inventoryGUI = plugin.getInventoryGUI();

            player.getInventory().addItem(inventoryGUI.getTool());

            commandSender.sendMessage(Component.text(plugin.getString("success_tool_given_to_player")).color(TextColor.color(60, 180, 180)));
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        return switch (args.length) {
            case 0 -> onTabCompleteNoArgs(commandSender);
            case 1 -> onTabCompleteArgs1(commandSender, args);
            case 2 -> onTabCompleteArgs2(commandSender, args);
            default -> null;
        };
    }

    private List<String> onTabCompleteNoArgs(CommandSender sender) {
        var r = new TreeSet<String>();
        if (sender instanceof Player) r.add("get");
        r.add("give");
        r.add("reload");
        return r.stream().toList();
    }

    private List<String> onTabCompleteArgs1(CommandSender sender, String[] args) {
        if ("give".equals(args[0])) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        var options = onTabCompleteNoArgs(sender);
        return options.contains(args[0]) ? Collections.emptyList() : options;
    }

    private List<String> onTabCompleteArgs2(CommandSender sender, String[] args) {
        if (!"give".equals(args[0])) {
            return Collections.emptyList();
        }
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s -> s.startsWith(args[1])).sorted().toList();
    }

}
