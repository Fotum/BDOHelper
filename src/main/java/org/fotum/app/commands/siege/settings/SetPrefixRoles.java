package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SetPrefixRoles implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		
		if (args.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		if (!PermissionChecker.checkGeneralPermissions(event))
			return;
		
		List<String> mentionedStr = args.stream()
										.map(
											(roleStr) -> roleStr.substring(3, (roleStr.length() - 1))
										).collect(Collectors.toList());
		
		LinkedHashSet<Long> roleIds = new LinkedHashSet<Long>();
		for (String role : mentionedStr)
		{
			long roleId;
			try
			{
				roleId = Long.parseLong(role);
			}
			catch (NumberFormatException ex)
			{
				BotUtils.sendMessageToChannel(channel, "Incorrect role mention given");
				return;
			}
			
			roleIds.add(roleId);
		}

		GuildManager.getInstance().getGuildSettings(guild.getIdLong()).setPrefixRoles(roleIds);
		BotUtils.sendMessageToChannel(channel, "Prefix roles successfully set");
	}

	@Override
	public String getHelp()
	{
		return "Sets a prefix roles for players list\n" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke() + " <@role1> <@role2> ...`";
	}

	@Override
	public String getInvoke()
	{
		return "setprefixes";
	}
}
