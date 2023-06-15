package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class AddSiegeCommand implements ISlashCommand
{
	@Override
	public void handle(SlashCommandInteractionEvent event)
	{
		event.deferReply(true).queue();

		Guild guild = event.getGuild();
		GuildManager manager = GuildManager.getInstance();

		GuildSettings settings = manager.getGuildSettings(guild.getIdLong());
		if (settings == null)
		{
			event.getHook().sendMessage("No siege settings configured for this guild").queue();
			return;
		}

		long listeningChannelId = settings.getListeningChannel();
		if (listeningChannelId == 0L)
		{
			event.getHook().sendMessage("Siege announcements channel is not configured").queue();
			return;
		}

		LocalDate startDt;
		String startDtStr = event.getOption("siege_dt").getAsString();
		try
		{
			startDt = LocalDate.parse(startDtStr, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (DateTimeParseException ex)
		{
			event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
			return;
		}

		if (startDt.isBefore(LocalDate.now()))
		{
			event.getHook().sendMessage("Can only schedule a siege on a future date").queue();
			return;
		}

		String zone = event.getOption("game_channel").getAsString();
		int playersAmount = event.getOption("slots").getAsInt();

		// Creating new siege instance with given parameters
		SiegeInstance siegeInst = new SiegeInstance(guild.getIdLong(), listeningChannelId, startDt, zone, playersAmount);
		// If autoreg settings exist for this guild - add players to registred list
		settings.getAutoregList().forEach(siegeInst::registerPlayer);

		manager.addSiegeInstance(guild.getIdLong(), siegeInst);
		event.getHook().sendMessage("Successfully complete").queue();
	}

	@Override
	public String getInvoke()
	{
		return "addsiege";
	}
}
