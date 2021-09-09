package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;
import java.util.stream.Collectors;

public class ForceRemPlayersCommand implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		
		if (args.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "You have to mention at least one player");
			return;
		}

		if (!PermissionChecker.checkGeneralPermissions(event))
			return;
		
		SiegeInstance siegeInst = GuildManager.getInstance().getSiegeInstance(guild.getIdLong());
		if (siegeInst == null)
		{
			BotUtils.sendMessageToChannel(channel, "Active siege is not found for current guild");
			return;
		}
		
		List<String> mentionedStr = args.stream()
				.map(
					(memberStr) -> memberStr.substring(3, (memberStr.length() - 1))
				).collect(Collectors.toList());
		
		for (String member : mentionedStr)
		{
			try
			{
				Long memberId = Long.parseLong(member);
				siegeInst.removePlayer(memberId);
			}
			catch (NumberFormatException ex) {}
		}

		BotUtils.sendMessageToChannel(channel, "Player(s) successfully removed");
	}

	@Override
	public String getHelp()
	{
		return "Forcely remove player(s) from participants list\n" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke() + " <@player1> <@player2> ...`";
	}

	@Override
	public String getInvoke()
	{
		return "forcerem";
	}
}
