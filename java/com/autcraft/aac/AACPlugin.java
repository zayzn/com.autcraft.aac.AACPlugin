package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class AACPlugin extends JavaPlugin {
    private final Map<String, String> stringMap = new HashMap<>();
    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();
    private InventoryGUI inventoryGUI;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        // Initialize our Inventory GUI
        inventoryGUI = new InventoryGUI(this, "AAC");

        // Set the strings
        initializeStringMap();

        // Set commands
        getCommand("aac").setExecutor(new AACCommandHandler(this));

        // Register events
        getServer().getPluginManager().registerEvents(new AACListener(this), this);

        getLogger().info("AAC - Augmentative and Alternative Communication initialized");
    }

    /**
     * Reload information from config.yml
     */
    public void reload(){
        reloadConfig();
        initializeStringMap();
        inventoryGUI.reload();
        saveConfig();
    }


    /**
     * Reference to the inventory GUI
     *
     * @return
     */
    public InventoryGUI getInventoryGUI(){
        return this.inventoryGUI;
    }

    /**
     * Retrieve and store strings from config.yml
     *
     */
    public void initializeStringMap(){
        var section = getConfig().getConfigurationSection("strings");
        if(section == null) {
            getLogger().warning("missing configuration section: strings");
            return;
        }
        // Loop over strings section of config.yml
        var loadedStrings = new HashMap<String, String>();
        for( String path : section.getKeys(false) ){
            loadedStrings.put(path, getConfig().getString("strings." + path, "String not found: " + path));
        }
        this.stringMap.clear();
        this.stringMap.putAll(loadedStrings);
        debug("Strings initialized from config.");
    }

    /**
     * Return the string from config.yml corresponding to "key"
     *
     * @return
     */
    public String getString(String key){
        return stringMap.get(key);
    }

    /**
     * Return true or false if player is still within the cooldown cache
     *
     * @param player
     * @return
     */
    public boolean isInCooldown(Player player){
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
    public long getCooldownRemaining(Player player){
        return TimeUnit.MILLISECONDS.toSeconds(cooldown.get(player.getUniqueId()) - System.currentTimeMillis());
    }

    /**
     * Add player to the cooldown cache
     *
     * @param player
     */
    public void addPlayerCooldown(Player player){
        int cooldown_in_seconds = getConfig().getInt("settings.cooldown_in_seconds") * 1000;
        cooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldown_in_seconds);
    }

    public void debug(String string){
        if( getConfig().getBoolean("settings.debug") )
            toConsole(string);
    }

    /**
     * Send output directly to console regardless of debugging settings
     *
     * @param string
     */
    public void toConsole(String string){
        getLogger().info(string);
    }

    /**
     * Returns a text Component with the given string from config.yml
     *
     * @param errorString
     * @return
     */
    public Component errorMessage(String errorString){
        return Component.text(getString(errorString)).color(TextColor.color(190, 0, 0));
    }

    /**
     * Returns a text Component with the given string but also replaces some text, based on replacements hashmap
     *
     * @param errorString
     * @param replacements
     * @return
     */
    public Component errorMessage(String errorString, HashMap<String, String> replacements){
        String returnMessage = getString(errorString);
        for( Map.Entry<String, String> set : replacements.entrySet() ){
            returnMessage = returnMessage.replace(set.getKey(), set.getValue());
        }

        return Component.text(returnMessage).color(TextColor.color(190, 0, 0));
    }
}
