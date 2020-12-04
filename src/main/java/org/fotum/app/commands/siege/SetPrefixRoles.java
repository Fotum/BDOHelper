package org.fotum.app.commands.siege;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetPrefixRoles implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
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
		
		List<Role> mentionedRoles = event.getMessage().getMentionedRoles();
		if (mentionedRoles.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect role mentions given");
			return;
		}
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		Long guildId = event.getGuild().getIdLong();
		Set<Long> roleIds = mentionedRoles
								.stream()
								.map((role) -> role.getIdLong())
								.collect(Collectors.toSet());
		
		SiegeInstance inst = SiegeManager.getInstance().getSiegeInstance(guildId);
		if (inst == null)
		{
			this.sendMessageToChannel(channel, "Siege instance not found for current guild");
			return;
		}
		
		inst.setPrefixRoles(roleIds);
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
