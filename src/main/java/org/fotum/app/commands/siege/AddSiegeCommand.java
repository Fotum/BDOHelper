package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class AddSiegeCommand implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		GuildManager manager = GuildManager.getInstance();
		GuildSettings settings = manager.getGuildSettings(guild.getIdLong());

		if (args.isEmpty() || args.size() < 3)
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		if (!PermissionChecker.checkGeneralPermissions(event))
			return;

		long listeningChannelId = settings.getListeningChannel();
		if (listeningChannelId == 0L)
		{
			BotUtils.sendMessageToChannel(channel, "Siege announcements channel is not configured");
			return;
		}

		LocalDate startDt;
		try
		{
			startDt = LocalDate.parse(args.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (DateTimeParseException ex)
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect date format given, expected format is `dd.mm.yyyy`");
			return;
		}

		if (startDt.isBefore(LocalDate.now()))
		{
			BotUtils.sendMessageToChannel(channel, "Can only schedule a siege on a future date");
			return;
		}

		String zone = Constants.ZONES.get(args.get(1).toLowerCase());
		if (zone == null)
		{
			String correctZones = String.join(", ", Constants.ZONES.keySet());
			BotUtils.sendMessageToChannel(channel, "Incorrect zone identifier given, expected one of the following: `[" + correctZones + "]`");
			return;
		}

		int playersAmount;
		try
		{
			playersAmount = Integer.parseInt(args.get(2));
		}
		catch (NumberFormatException ex)
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect number of players given");
			return;
		}

		// Creating new siege instance with given parameters
		SiegeInstance siegeInst = new SiegeInstance(guild, guild.getTextChannelById(listeningChannelId), startDt, zone, playersAmount);
		// If autoreg settings exist for this guild - add players to registred list
		settings.getAutoregList().forEach(siegeInst::addPlayer);

		manager.removeSiegeInstance(guild.getIdLong());
		manager.addSiegeInstance(guild.getIdLong(), siegeInst);
	}

	@Override
	public String getHelp()
	{
		return "Schedules a siege info on a given date\n"
				+ "Usage: `" + Constants.PREFIX + this.getInvoke() + " [dd.mm.yyyy] [game_channel] [players_amount]`";
	}

	@Override
	public String getInvoke()
	{
		return "addsiege";
	}
}
