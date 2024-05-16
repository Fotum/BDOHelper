package org.fotum.app.handlers;

import lombok.Getter;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class SettingsHandler {
    @Getter
    private static SettingsSaverDaemon settingsDaemon;
    public static void saveSettingsToJSON() {
        JSONArray root = new JSONArray();
        for (GuildHandler handler : GuildManager.getInstance().getGuilds().values()) {
            JSONObject guildInfo = handler.toJSON();
            root.put(guildInfo);
        }

        File outFile = new File(Constants.GUILD_SETTINGS_LOC);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)) {
            writer.write(root.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void loadSettingsFromJSON() {
        JSONArray root = new JSONArray();
        File jsonFile = new File(Constants.GUILD_SETTINGS_LOC).getAbsoluteFile();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8))) {
            root = new JSONArray(new JSONTokener(reader));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        for (int i = 0; i < root.length(); i++) {
            JSONObject guildInfo = root.getJSONObject(i);
            GuildHandler handler = new GuildHandler(guildInfo);
            GuildManager.getInstance().addGuildHandler(handler);
        }
    }

    public static void initializeDaemon() {
        SettingsHandler.settingsDaemon = new SettingsSaverDaemon();
        SettingsHandler.settingsDaemon.setDaemon(true);
        SettingsHandler.settingsDaemon.start();
    }
}
