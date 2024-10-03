package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AACPlugin extends JavaPlugin {
    private final Map<String, String> stringMap = new HashMap<>();
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
        Objects.requireNonNull(getCommand("aac")).setExecutor(new AACCommandHandler(this));

        // Register events
        getServer().getPluginManager().registerEvents(new AACListener(this), this);

        getLogger().info("AAC - Augmentative and Alternative Communication initialized");
    }

    /**
     * Reload information from config.yml
     */
    public void reload() {
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
    public InventoryGUI getInventoryGUI() {
        return this.inventoryGUI;
    }

    /**
     * Retrieve and store strings from config.yml
     */
    public void initializeStringMap() {
        var section = getConfig().getConfigurationSection("strings");
        if (section == null) {
            getLogger().warning("missing configuration section: strings");
            return;
        }
        // Loop over strings section of config.yml
        var loadedStrings = new HashMap<String, String>();
        for (String path : section.getKeys(false)) {
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
    public String getString(String key) {
        return stringMap.get(key);
    }

    public void debug(String string) {
        if (getConfig().getBoolean("settings.debug"))
            toConsole(string);
    }

    /**
     * Send output directly to console regardless of debugging settings
     *
     * @param string
     */
    public void toConsole(String string) {
        getLogger().info(string);
    }

    /**
     * Returns a text Component with the given string from config.yml
     *
     * @param errorString
     * @return
     */
    public Component errorMessage(String errorString) {
        return Component.text(getString(errorString)).color(TextColor.color(190, 0, 0));
    }

    /**
     * Returns a text Component with the given string but also replaces some text, based on replacements hashmap
     *
     * @param errorString
     * @param replacements
     * @return
     */
    public Component errorMessage(String errorString, HashMap<String, String> replacements) {
        String returnMessage = getString(errorString);
        for (Map.Entry<String, String> set : replacements.entrySet()) {
            returnMessage = returnMessage.replace(set.getKey(), set.getValue());
        }

        return Component.text(returnMessage).color(TextColor.color(190, 0, 0));
    }
}
