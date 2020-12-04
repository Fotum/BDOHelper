package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class RemoveManagingRole implements ICommand
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
		
		Long guildId = event.getGuild().getIdLong();
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		SiegeManager.getInstance().removeManagingRole(guildId);
		this.sendMessageToChannel(channel, "Managing role successfully removed");
	}

	@Override
	public String getHelp()
	{
		return "Removes a role that can edit this siege manager\n" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke();
	}

	@Override
	public String getInvoke()
	{
		return "remrole";
	}

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
