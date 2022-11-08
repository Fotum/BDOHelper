package org.fotum.app.utils;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.fotum.app.Constants;
import org.fotum.app.MainApp;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.vkfeed.VkCaller;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BotUtils
{
    private static final Random RANDOM = new Random();
    private static SettingsSaverDaemon settingsDaemon;

	public static void runStartupSequence()
	{
		log.info("Loading configs");
		BotUtils.loadSettingsFromJSON();

		log.info("Starting settings saver daemon");
		BotUtils.settingsDaemon = new SettingsSaverDaemon();
		BotUtils.settingsDaemon.setDaemon(true);
		BotUtils.settingsDaemon.start();

		log.info("Upserting slash commands");
		ShardManager api = MainApp.getAPI();
		for (Guild guild : api.getGuilds())
		{
			guild.upsertCommand("setup", "Sets up or updates siege settings for current guild")
					.addOption(OptionType.CHANNEL, "channel", "Channel mention (#channel_name) for announcments", false)
					.addOption(OptionType.STRING, "mention_roles", "Roles to mention on announcement (Optional)", false)
					.addOption(OptionType.STRING, "prefix_roles", "Roles to sort players in announcement's body (Optional)", false)
					.queue();
			guild.upsertCommand("addsiege", "Schedules a siege info on a given date")
					.addOptions(
							new OptionData(OptionType.STRING, "siege_dt", "Siege date in dd.mm.yyyy format", true),
							new OptionData(OptionType.STRING, "game_channel", "One of predefined channel abbriviations", true),
							new OptionData(OptionType.INTEGER, "slots", "Amount of free slots for this siege", true)
									.setRequiredRange(1, 200))
					.queue();
			guild.upsertCommand("remsiege", "Removes currently scheduled siege")
					.queue();
			guild.upsertCommand("updsiege", "Updates current siege instance")
					.addOption(OptionType.STRING, "field_nm", "Field to update date/zone/maxplrs/desc", true)
					.addOption(OptionType.STRING, "field_val", "New value for selected field", true)
					.queue();
			guild.upsertCommand("forceadd", "Manually adds mentioned users to players list")
					.addOption(OptionType.STRING, "players", "Players to add", true)
					.queue();
			guild.upsertCommand("forcerem", "Manually removes mentioned users from players list")
					.addOption(OptionType.STRING, "players", "Players to remove", true)
					.queue();
			guild.upsertCommand("autoreg", "Sets up an autoreg options")
					.addOption(OptionType.STRING, "action", "Action to be performed by command (add/rem/list)", true)
					.addOption(OptionType.STRING, "members", "Members to be added or deleted", false)
					.queue();
			guild.upsertCommand("clear", "Deletes messages from current channel")
					.addOptions(
							new OptionData(OptionType.INTEGER, "amount", "Amount of messages to delete", true)
									.setRequiredRange(1, 100))
					.queue();
		}

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

	public static void sendMessageToChannel(@NotNull MessageChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}

	public static void sendDirectMessage(User user, String content)
	{
		if (Objects.isNull(user))
			return;

		user.openPrivateChannel()
				.flatMap((channel) -> channel.sendMessage(content))
				.queue();
	}

	protected static void shutdownInstances()
	{
		Map<Long, SiegeInstance> instances = GuildManager.getInstance().getSiegeInstances();

		for (Long guildId : instances.keySet()) {
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
			JSONObject guildInfo = new JSONObject();
			guildInfo.put("id", entry.getKey());

			// Creating guild settings JSON
			JSONObject guildSettingsJson = entry.getValue().toJSON();

			// Saving siege instance if exists
			SiegeInstance siegeInstance = GuildManager.getInstance().getSiegeInstance(entry.getKey());
			JSONObject siegeInstanceJson = new JSONObject();

			if (siegeInstance != null)
			{
				siegeInstanceJson = siegeInstance.toJSON();
			}

			// Saving VK caller if exists
			VkCaller vkCaller = GuildManager.getInstance().getVkCaller(entry.getKey());
			JSONObject vkCallerJson = new JSONObject();

			if (vkCaller != null)
			{
				vkCallerJson = vkCaller.toJSON();
			}

			guildInfo.put("settings", guildSettingsJson);
			guildInfo.put("siege_instance", siegeInstanceJson);
			guildInfo.put("vk_caller", vkCallerJson);
			root.put(guildInfo);
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

	protected static void loadSettingsFromJSON()
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
			JSONObject guildInfo = root.getJSONObject(i);

			// Load guild settings from JSON
			long guildId = guildInfo.getLong("id");
			JSONObject guildSettingsJson = guildInfo.getJSONObject("settings");

			GuildSettings guildSettings = new GuildSettings(guildSettingsJson);
			GuildManager.getInstance().getGuildSettings().put(guildId, guildSettings);

			// Load instances from JSON
			JSONObject siegeInstanceJson = guildInfo.getJSONObject("siege_instance");
			if (siegeInstanceJson.length() > 0)
			{
				SiegeInstance instance = new SiegeInstance(guildId, siegeInstanceJson);
				GuildManager.getInstance().addSiegeInstance(guildId, instance);
			}

			// Load VK caller from JSON
			JSONObject vkCallerJson = guildInfo.getJSONObject("vk_caller");
			if (vkCallerJson.length() > 0)
			{
				VkCaller caller = new VkCaller(vkCallerJson);
				GuildManager.getInstance().addVkCaller(guildId, caller);
			}
		}
	}

    public static @NotNull Color getRandomColor()
    {
        float r = RANDOM.nextFloat();
        float g = RANDOM.nextFloat();
        float b = RANDOM.nextFloat();

        return new Color(r, g, b);
    }

    public static @NotNull EmbedBuilder getDefault()
    {
        return new EmbedBuilder()
                .setColor(getRandomColor())
                .setFooter("{BDOHelper}", null)
                .setTimestamp(Instant.now());
    }
}
