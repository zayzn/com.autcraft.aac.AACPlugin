package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AACListener implements Listener {
    private final AACPlugin plugin;
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public AACListener(AACPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Tool item (book) to open GUI
     *
     * @param e
     */
    @EventHandler(ignoreCancelled = true)
    public void playerOpensInventoryGUI(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL) return;

        Player player = e.getPlayer();
        InventoryGUI inventoryGUI = plugin.getInventoryGUI();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemStack itemOffHand = player.getInventory().getItemInOffHand();

        // The AAC tool has to be in either the main hand or offhand to work
        if (!inventoryGUI.isItemPanelTool(item) || !inventoryGUI.isItemPanelTool(itemOffHand)) return;

        // If, for whatever reason, the player doesn't have permission to open the gui
        if (!player.hasPermission("aac.open")) {
            player.sendMessage(plugin.getString("error_no_permission"));
            return;
        }

        player.openInventory(inventoryGUI.getGUI(player, 1));

        e.setCancelled(true);
    }

    /**
     * Clicking an item in the GUI inventory screen
     *
     * @param e
     */
    @EventHandler(ignoreCancelled = true)
    public void playerMakesAACSelection(InventoryClickEvent e) {

        // Disable all number key interactions when this menu is open. NO SWITCHING ITEMS!
        if (e.getClick() == ClickType.NUMBER_KEY
                || e.getCurrentItem() == null
                || e.getCurrentItem().getType() == Material.AIR
        ) {
            e.setCancelled(true);
            return;
        }

        Component inventoryTitle = e.getView().title();
        Component GUITitle = Component.text(plugin.getConfig().getString("settings.title"));

        if (!inventoryTitle.equals(GUITitle)) return;

        // Object vars
        Player player = (Player) e.getWhoClicked();
        ItemStack clickedItem = e.getCurrentItem();
        InventoryGUI inventoryGUI = plugin.getInventoryGUI();

        // If the next button is clicked
        if (inventoryGUI.isNextButton(clickedItem)) {
            // Open GUI for the next page
            player.openInventory(inventoryGUI.getGUI(player, inventoryGUI.getNextPage(clickedItem)));
        }
        // If the previous button is clicked
        else if (inventoryGUI.isPreviousButton(clickedItem)) {
            // Open GUI for the previous page
            player.openInventory(inventoryGUI.getGUI(player, inventoryGUI.getPreviousPage(clickedItem)));
        }
        // Otherwise, output to the chat
        else {

            // If it has persistent data, then it has the string to output
            String output = inventoryGUI.getPersistentDataContainer(clickedItem);

            // So long as the output isn't blank, send it to the chat
            if (output != null) {
                if (isInCooldown(player)) {
                    HashMap<String, String> replacements = new HashMap<>();
                    replacements.put("{SECONDS}", "" + getCooldownRemaining(player));

                    player.sendMessage(plugin.errorMessage("error_player_in_cooldown", replacements));
                } else {
                    plugin.toConsole(player.getName() + " is using AAC to generate the following text in chat:");
                    player.chat(output);

                    // Message sent successfully, now apply cooldown
                    addPlayerCooldown(player);
                }
            } else {
                plugin.toConsole("Error: No output was stored because the plugin could not set the persistent data for the panel option.");
            }
        }

        e.setCancelled(true);
        player.updateInventory();

    }

    /**
     * Return true or false if player is still within the cooldown cache
     *
     * @param player
     * @return
     */
    private boolean isInCooldown(Player player) {
        var pid = player.getUniqueId();
        if (cooldown.containsKey(pid)) {
            if (cooldown.get(pid) - System.currentTimeMillis() > 0L) {
                return true;
            } else {
                cooldown.remove(pid);
            }
        }
        return false;
    }

    /**
     * Get the amount of time remaining before next message can be sent
     *
     * @param player
     * @return
     */
    private long getCooldownRemaining(Player player) {
        return TimeUnit.MILLISECONDS.toSeconds(cooldown.get(player.getUniqueId()) - System.currentTimeMillis());
    }

    /**
     * Add player to the cooldown cache
     *
     * @param player
     */
    private void addPlayerCooldown(Player player) {
        int cooldown_in_seconds = plugin.getConfig().getInt("settings.cooldown_in_seconds") * 1000;
        cooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldown_in_seconds);
    }

}
