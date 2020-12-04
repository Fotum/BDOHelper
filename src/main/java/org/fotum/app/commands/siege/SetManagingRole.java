package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetManagingRole implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		Member author = event.getMember();
		
		if (!author.hasPermission(Permission.MANAGE_SERVER))
		{
			this.sendMessageToChannel(channel, "You dont have permission to use this command");
			return;
		}
		
		if (args.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}
		
		List<Role> roles = event.getMessage().getMentionedRoles();
		if (roles.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect role mention given");
			return;
		}
		
		Long roleId = roles.get(0).getIdLong();
		Long guildId = event.getGuild().getIdLong();
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		SiegeManager.getInstance().removeManagingRole(guildId);
		SiegeManager.getInstance().addManagingRole(guildId, roleId);
		this.sendMessageToChannel(channel, "Managing role successfully set");
	}

	@Override
	public String getHelp()
	{
		return "Sets a role that can edit this siege manager\n" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke() + " <role>`";
	}

	@Override
	public String getInvoke()
	{
		return "setrole";
	}

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
