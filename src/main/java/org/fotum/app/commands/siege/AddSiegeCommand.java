package org.fotum.app.commands.siege;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AddSiegeCommand implements ICommand
{
	private static final HashMap<String, String> zones;
	static
	{
		zones = new HashMap<String, String>();
		zones.put("bal", "Баленос");
		zones.put("val", "Валенсия");
		zones.put("ser", "Серендия");
		zones.put("kal", "Кальфеон");
		zones.put("med", "Медия");
	}

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		SiegeManager manager = SiegeManager.getInstance();
		TextChannel channel = event.getChannel();
		Guild guild = event.getGuild();

		if (args.isEmpty() || args.size() < 3)
		{
			this.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		Long allowedRoleId = SiegeManager.getInstance().getManagingRole(guild.getIdLong());
		if (allowedRoleId == null)
		{
			this.sendMessageToChannel(channel, "Siege managing role is not configured");
			return;
		}

		boolean authorHasRole = event.getMember().getRoles()
				.stream()
				.anyMatch(
					(role) -> role.getIdLong() == allowedRoleId
				);

		if (!authorHasRole)
		{
			this.sendMessageToChannel(channel, "You do not have permissions to use this command");
			return;
		}

		Long listeningChannelId = SiegeManager.getInstance().getListeningChannel(guild.getIdLong());
		if (listeningChannelId == null)
		{
			this.sendMessageToChannel(channel, "Siege announcements channel is not configured");
			return;
		}

		LocalDate startDt;
		try
		{
			startDt = LocalDate.parse(args.get(0), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			this.sendMessageToChannel(channel, "Incorrect date format given, expected format is `dd.mm.yyyy`");
			return;
		}

		LocalDate now = LocalDate.now();
		if (startDt.isBefore(now))
		{
			this.sendMessageToChannel(channel, "Can only schedule a siege on a future date");
			return;
		}

		String zone = zones.get(args.get(1).toLowerCase());
		if (zone == null)
		{
			String correctZones = zones.keySet()
					.stream()
					.collect(Collectors.joining(", "));
			this.sendMessageToChannel(channel, "Incorrect zone identifier given, expected one of `[" + correctZones + "]`");
			return;
		}

		int playersAmount;
		try
		{
			playersAmount = Integer.valueOf(args.get(2));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			this.sendMessageToChannel(channel, "Incorrect number of players given");
			return;
		}

		Member selfMember = event.getGuild().getSelfMember();
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}

		SiegeInstance siegeInst = manager.addSiegeInstance(guild.getIdLong());
		siegeInst.reinit();
		siegeInst.setChannel(guild.getTextChannelById(listeningChannelId));
		siegeInst.setStartDt(startDt);
		siegeInst.setZone(zone);
		siegeInst.setPlayersMax(playersAmount);
		siegeInst.schedule();
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

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
