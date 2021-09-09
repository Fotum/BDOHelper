package org.fotum.app.utils;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.GuildManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BotUtils
{
	private static SettingsSaverDaemon settingsDaemon;

	public static void runStartupSequence(JDA jda)
	{
		log.info("Loading configs");
		BotUtils.loadSettingsFromJSON(jda);

		log.info("Starting settings saver daemon");
		BotUtils.settingsDaemon = new SettingsSaverDaemon();
		BotUtils.settingsDaemon.setDaemon(true);
		BotUtils.settingsDaemon.start();
		log.info("Startup successfully finished");
	}

	public static void runShutdownSequence()
	{
		log.info("Shutting down settings saver daemon");
		BotUtils.settingsDaemon.interrupt();
		log.info("Shutting down siege instances");
		BotUtils.shutdownInstances();
		log.info("Saving configs");
		BotUtils.saveSettingsToJSON();
	}

	public static void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}

	public static void sendDirectMessage(User user, String content)
	{
		user.openPrivateChannel()
				.flatMap((channel) -> channel.sendMessage(content))
				.queue();
	}

	protected static void shutdownInstances()
	{
		Map<Long, SiegeInstance> instances = GuildManager.getInstance().getSiegeInstances();
		Iterator<Long> instancesIter = instances.keySet().iterator();

		while (instancesIter.hasNext())
		{
			Long guildId = instancesIter.next();
			GuildManager.getInstance().getSiegeInstance(guildId).stopInstance();
		}
	}

	protected static void saveSettingsToJSON()
	{
		Map<Long, GuildSettings> settings = GuildManager.getInstance().getGuildSettings();
		if (settings == null)
			return;

		// Convert settings to JSON
		JSONArray root = new JSONArray();
		for (Map.Entry<Long, GuildSettings> entry : settings.entrySet())
		{
			JSONObject guildSettingsJson = entry.getValue().toJSON();
			guildSettingsJson.put("id", entry.getKey());

			// Saving siege instance if exists
			SiegeInstance siegeInstance = GuildManager.getInstance().getSiegeInstance(entry.getKey());
			JSONObject siegeInstanceJson = new JSONObject();

			if (siegeInstance != null)
			{
				siegeInstanceJson = siegeInstance.toJSON();
			}

			guildSettingsJson.put("siege_instance", siegeInstanceJson);
			root.put(guildSettingsJson);
		}

		File outFile = new File(Constants.GUILD_SETTINGS_LOC);
		try
		{
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8);
			writer.write(root.toString());
			writer.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	protected static void loadSettingsFromJSON(JDA jda)
	{
		JSONArray root = new JSONArray();
		try
		{
			File jsonFile = new File(Constants.GUILD_SETTINGS_LOC).getAbsoluteFile();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8));
			root = new JSONArray(new JSONTokener(reader));
			reader.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}

		for (int i = 0; i < root.length(); i++)
		{
			// Load guild settings from JSON
			JSONObject guildSettingsJson = root.getJSONObject(i);
			long guildId = guildSettingsJson.getLong("id");

			GuildSettings guildSettings = new GuildSettings(guildSettingsJson);
			GuildManager.getInstance().getGuildSettings().put(guildId, guildSettings);

			// Load instances from JSON
			JSONObject siegeInstanceJson = guildSettingsJson.getJSONObject("siege_instance");
			if (siegeInstanceJson.length() > 0)
			{
				Guild guild = jda.getGuildById(guildId);
				TextChannel channel = guild.getTextChannelById(guildSettings.getListeningChannel());
				SiegeInstance instance = new SiegeInstance(guild, channel, siegeInstanceJson);
				GuildManager.getInstance().addSiegeInstance(guildId, instance);
			}
		}
	}
}
