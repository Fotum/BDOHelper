package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

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

		List<OptionMapping> opts = event.getOptions();
		LocalDate startDt;
		try
		{
			startDt = LocalDate.parse(opts.get(0).getAsString(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
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

		String zone = Constants.ZONES.get(opts.get(1).getAsString().toLowerCase());
		if (zone == null)
		{
			String correctZones = String.join(", ", Constants.ZONES.keySet());
			event.getHook().sendMessage("Incorrect zone identifier given, expected one of the following: `[" + correctZones + "]`").queue();
			return;
		}

		int playersAmount = opts.get(2).getAsInt();

		// Creating new siege instance with given parameters
		SiegeInstance siegeInst = new SiegeInstance(guild.getIdLong(), listeningChannelId, startDt, zone, playersAmount);
		// If autoreg settings exist for this guild - add players to registred list
		settings.getAutoregList().forEach(siegeInst::addPlayer);

		manager.removeSiegeInstance(guild.getIdLong());
		manager.addSiegeInstance(guild.getIdLong(), siegeInst);
		event.getHook().sendMessage("Successfully complete").queue();
	}

	@Override
	public String getInvoke()
	{
		return "addsiege";
	}
}
