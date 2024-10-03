package com.autcraft.aac;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PlayerHeadUtil {

    private static final Logger logger = LoggerFactory.getLogger(PlayerHeadUtil.class);

    private static final JSONParser PARSER = new JSONParser();

    /**
     * Retrieve a player head just from a player's name
     */
    public static @Nullable ItemStack getSkull(@NotNull String playerName, @NotNull List<Component> lore) {
        String uuid;
        String texture;
        try {
            // Retrieve the player's UUID if at all possible
            // If it fails, that player probably doesn't exist
            uuid = getUUIDFromMojangByName(playerName);
        } catch (Exception e) {
            return null;
        }

        // Try to retrieve the texture from Mojang's Session server
        // If it fails, it means that Mojang's servers are down.
        try {
            texture = getSkinTextureByUUID(UUID.fromString(uuid));
        } catch (Exception e) {
            logger.warn("Unable to get skin for {}", uuid);
            return null;
        }

        // Now run the main function to return the item stack
        return getSkull(UUID.randomUUID(), texture, Component.text(playerName), lore);
    }

    public static @NotNull ItemStack getSkull(@NotNull UUID uuid, @NotNull String texture, @Nullable Component customName, @NotNull List<Component> lore) throws RuntimeException {
        // Create the item stack in advance
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        String url;

        // Depending on the length of the texture string, use the appropriate method to extra the URL
        if (texture.length() > 200) {
            try {
                url = getSkinURLFromMojang(texture);
            } catch (UnsupportedEncodingException | ParseException e) {
                logger.error("Unable to retrieve URL from {}", texture);
                throw new RuntimeException(e);
            }
        } else {
            url = getSkinURLFromString(texture);
        }

        PlayerProfile profile = Bukkit.createProfile(uuid);
        PlayerTextures textures = profile.getTextures();
        URL urlObject;
        try {
            urlObject = URI.create(url).toURL(); // The URL to the skin, for example: https://textures.minecraft.net/texture/18813764b2abc94ec3c3bc67b9147c21be850cdf996679703157f4555997ea63a
        } catch (MalformedURLException e) {
            logger.error("Invalid URL {}", url);
            throw new RuntimeException(e);
        }
        textures.setSkin(urlObject); // Set the skin of the player profile to the URL
        profile.setTextures(textures); // Set the textures back to the profile

        // Set all that data to the itemstack metadata
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(Objects.requireNonNull(uuid)));

        // Display name
        if (customName != null) {
            meta.displayName(customName);
        }

        // Lore
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }

        skull.setItemMeta(meta);

        return skull;
    }

    /**
     * Retrieve the player's UUID from just their name
     */
    public static @NotNull String getUUIDFromMojangByName(@NotNull String name) throws IOException {
        String uuid = null;

        // First obvious method is to just get it from the server itself.
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            return player.getUniqueId().toString();
        }

        // If the server has no record of the player, get it from Mojang's API
        URL url = URI.create("https://api.mojang.com/users/profiles/minecraft/" + name).toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        validateResponse(conn.getResponseCode());

        StringBuilder inline = new StringBuilder();
        Scanner scanner = new Scanner(url.openStream());

        //Write all the JSON data into a string using a scanner
        while (scanner.hasNext()) {
            inline.append(scanner.nextLine());
        }

        //Close the scanner
        scanner.close();

        //Using the JSON simple library parse the string into a json object
        JSONObject data_obj;
        try {
            data_obj = (JSONObject) PARSER.parse(String.valueOf(inline));
        } catch (ParseException e) {
            logger.error("Could not parse API response");
            throw new RuntimeException(e);
        }

        //Get the required object from the above created object
        if (data_obj != null) {
            uuid = data_obj.get("id").toString();
        }

        if (uuid != null) {
            String result;
            result = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, 32);
            uuid = result;
        } else {
            logger.error("could not determine id of {}", name);
            throw new RuntimeException("uuid must not be null");
        }

        return uuid;
    }


    /**
     * If getting a head for a player, we must retrieve the texture from Mojang's Session servers.
     */
    public static @NotNull String getSkinTextureByUUID(@NotNull UUID uuid) throws IOException, ParseException {
        String texture;
        String apiURL = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;

        URL url = URI.create(apiURL).toURL();

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();

        validateResponse(conn.getResponseCode());

        StringBuilder inline = new StringBuilder();
        Scanner scanner = new Scanner(url.openStream());

        //Write all the JSON data into a string using a scanner
        while (scanner.hasNext()) {
            inline.append(scanner.nextLine());
        }

        //Close the scanner
        scanner.close();

        //Using the JSON simple library parse the string into a json object
        JSONParser parse = new JSONParser();
        JSONObject data_obj = (JSONObject) parse.parse(inline.toString());
        JSONArray propertiesArray = (JSONArray) data_obj.get("properties");
        for (JSONObject property : (List<JSONObject>) propertiesArray) {
            String name = (String) property.get("name");
            if (name.equals("textures")) {
                return (String) property.get("value");
            }
        }

        //Get the required object from the above created object
        System.out.println(data_obj.values());
        texture = data_obj.get("properties").toString();

        return texture;
    }

    /**
     * Get Skin URL from string entered into config file
     */
    private static @NotNull String getSkinURLFromString(@NotNull String base64) {
        //String url = Base64.getEncoder().withoutPadding().encodeToString(texture.getBytes());
        Base64.Decoder dec = Base64.getDecoder();
        String decoded = new String(dec.decode(base64));

        // Should be something like this:
        // {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/9631597dce4e4051e8d5a543641966ab54fbf25a0ed6047f11e6140d88bf48f"}}}
        // System.out.println("URL = " + decoded.substring(28, decoded.length() - 4));
        return decoded.substring(28, decoded.length() - 4);
    }

    /**
     * Get Skin URL from long string returned by Mojang's API
     */
    private static @NotNull String getSkinURLFromMojang(@NotNull String base64) throws UnsupportedEncodingException, ParseException {
        String texture = null;
        String decodedBase64 = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        JSONObject base64json = (JSONObject) PARSER.parse(decodedBase64);
        JSONObject textures = (JSONObject) base64json.get("textures");
        if (textures.containsKey("SKIN")) {
            JSONObject skinObject = (JSONObject) textures.get("SKIN");
            texture = (String) skinObject.get("url");
        }
        assert texture != null; // it is assumed that the API response guarantees the field's presence
        return texture;
    }

    private static void validateResponse(int response) {
        if (response != 200) {
            logger.error("API connection not OK with response {}", response);
            throw new RuntimeException();
        }
    }

    private PlayerHeadUtil() {}

}
