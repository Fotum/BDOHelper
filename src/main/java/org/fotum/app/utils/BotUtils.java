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
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.*;
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
			BotUtils.upsertBotCommands(guild);

		log.info("Startup successfully finished");
	}

	public static void upsertBotCommands(Guild guild)
	{
		log.info(String.format("Upserting slash commands for guild with ID %d", guild.getIdLong()));

		guild.upsertCommand("setup", "Sets up or updates siege settings for current guild")
				.addOption(OptionType.CHANNEL, "channel", "Channel mention (#channel_name) for announcments", false)
				.addOption(OptionType.STRING, "mention_roles", "Roles to mention on announcement (Optional)", false)
				.queue();
		guild.upsertCommand("addsiege", "Schedules a siege info on a given date")
				.addOptions(
						new OptionData(OptionType.STRING, "siege_dt", "Siege date in dd.mm.yyyy format", true),
						new OptionData(OptionType.STRING, "game_channel", "One of predefined channel abbriviations", true)
								.addChoice("Unknown", "Неизвестно")
								.addChoice("Balenos", "Баленос")
								.addChoice("Valencia", "Валенсия")
								.addChoice("Serendia", "Серендия")
								.addChoice("Calpheon", "Кальфеон")
								.addChoice("Mediah", "Медия")
								.addChoice("Kamasylvia", "Камасильвия"),
						new OptionData(OptionType.INTEGER, "slots", "Amount of free slots for this siege", true)
								.setRequiredRange(1, 200))
				.queue();
		guild.upsertCommand("remsiege", "Removes currently scheduled siege")
				.addOption(OptionType.STRING, "siege_dt", "Date of the siege to be removed in dd.mm.yyyy format (if not specified all sieges will be removed)", false)
				.queue();
		guild.upsertCommand("updsiege", "Updates current siege instance")
				.addOptions(
						new OptionData(OptionType.STRING, "siege_dt", "Date of the siege to be updated in dd.mm.yyyy format", true),
						new OptionData(OptionType.STRING, "field_nm", "Field to update", true)
								.addChoice("Siege date in dd.mm.yyyy format", "date")
								.addChoice("Siege zone", "zone")
								.addChoice("Max slots", "maxplrs"),
						new OptionData(OptionType.STRING, "field_val", "New value for selected field", true))
				.queue();
		guild.upsertCommand("forceadd", "Manually adds mentioned users to players list")
				.addOption(OptionType.STRING, "siege_dt", "Date of the siege to add players in dd.mm.yyyy format", true)
				.addOption(OptionType.STRING, "players", "Players to add", true)
				.queue();
		guild.upsertCommand("forcerem", "Manually removes mentioned users from players list")
				.addOption(OptionType.STRING, "siege_dt", "Date of the siege to remove players in dd.mm.yyyy format", true)
				.addOption(OptionType.STRING, "players", "Players to remove", true)
				.queue();
		guild.upsertCommand("autoreg", "Sets up an autoreg options")
				.addOptions(
						new OptionData(OptionType.STRING, "action", "Action to be performed by command", true)
								.addChoice("Add player(s)", "add")
								.addChoice("Remove player(s)", "rem")
								.addChoice("Show autoreg players", "list"),
						new OptionData(OptionType.STRING, "members", "Member(s) to be added or deleted", false))
				.queue();
		guild.upsertCommand("reguser", "Adds member's BDO info")
				.addOptions(
						new OptionData(OptionType.STRING, "bdo_name", "Your BDO family name", true),
						new OptionData(OptionType.STRING, "bdo_allegiance", "Your BDO allegiance main or alliance (M/A)", true)
								.addChoice("Main", "M")
								.addChoice("Alliance", "A"))
				.queue();
		guild.upsertCommand("remuser", "Removes user's BDO info")
				.queue();
		guild.upsertCommand("forcereguser", "Manually adds BDO info for specified discord user")
				.addOptions(
						new OptionData(OptionType.USER, "discord_user", "Discord mention of player to map", true),
						new OptionData(OptionType.STRING, "bdo_name", "BDO family name of player", true),
						new OptionData(OptionType.STRING, "bdo_allegiance", "Player's BDO allegiance main or alliance (M/A)", true)
								.addChoice("Main", "M")
								.addChoice("Alliance", "A"))
				.queue();
		guild.upsertCommand("forceremuser", "Manually removes BDO info for specified discord user")
				.addOption(OptionType.USER, "discord_user", "Discord mention of player to remove", true)
				.queue();

		guild.upsertCommand("clear", "Deletes messages from current channel")
				.addOptions(
						new OptionData(OptionType.INTEGER, "amount", "Amount of messages to delete", true)
								.setRequiredRange(1, 100))
				.queue();
		guild.upsertCommand("tictactoe", "Starts TicTacToe game with selected user")
				.addOptions(
						new OptionData(OptionType.USER, "target", "Player with who you want to play", true),
						new OptionData(OptionType.INTEGER, "size", "Board size (default is 3)", false)
								.setRequiredRange(3, 4))
				.queue();
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

	public static LocalDate convertStrToDate(String strDate)
	{
		try
		{
			return LocalDate.parse(strDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (DateTimeParseException ex)
		{
			return null;
		}
	}

	protected static void shutdownInstances()
	{
		GuildManager.getInstance().getSiegeInstances()
				.values()
				.stream()
				.flatMap(Collection::stream)
				.forEach(SiegeInstance::stopInstance);
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

			// Creating guild settings JSON
			JSONObject guildSettingsJson = entry.getValue().toJSON();

			// Saving siege instances if any
			List<SiegeInstance> siegeInstances = GuildManager.getInstance().getGuildSiegeInstances(entry.getKey());
			JSONArray siegeInstancesJson = new JSONArray();
			for (SiegeInstance inst : siegeInstances)
				siegeInstancesJson.put(inst.toJSON());

			// Add fields to settings JSON
			guildInfo.put("id", entry.getKey());
			guildInfo.put("settings", guildSettingsJson);
			guildInfo.put("siege_instances", siegeInstancesJson);
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
			JSONArray siegeInstancesJson = guildInfo.getJSONArray("siege_instances");
			for (int j = 0; j < siegeInstancesJson.length(); j++)
			{
				JSONObject siegeInstanceJson = siegeInstancesJson.getJSONObject(j);
				SiegeInstance siegeInstance = new SiegeInstance(guildId, siegeInstanceJson);
				GuildManager.getInstance().addSiegeInstance(guildId, siegeInstance);
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
