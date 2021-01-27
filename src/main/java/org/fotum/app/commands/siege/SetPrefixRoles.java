package org.fotum.app.commands.siege;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetPrefixRoles implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		
		if (args.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		Long allowedRoleId = SiegeManager.getInstance().getManagingRole(event.getGuild().getIdLong());
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
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		List<String> mentionedStr = args.stream()
										.map(
											(roleStr) -> roleStr.substring(3, (roleStr.length() - 1))
										).collect(Collectors.toList());
		
		LinkedHashSet<Long> roleIds = new LinkedHashSet<Long>();
		for (String role : mentionedStr)
		{
			Long roleId;
			try
			{
				roleId = Long.parseLong(role);
			}
			catch (NumberFormatException ex)
			{
				this.sendMessageToChannel(channel, "Incorrect role mention given");
				return;
			}
			
			roleIds.add(roleId);
		}
		
		SiegeManager.getInstance().addPrefixRoles(guild.getIdLong(), roleIds);
		this.sendMessageToChannel(channel, "Prefix roles successfully set");
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
	
	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
