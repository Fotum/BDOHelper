package org.fotum.app.commands.siege;

import java.util.List;

import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AddPlayerCommand implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		Member author = event.getMember();
		SiegeManager manager = SiegeManager.getInstance();
		
		Long channelId = channel.getIdLong();
		Long guildId = event.getGuild().getIdLong();
		Long registredChannelId = manager.getListeningChannel(guildId);
		
		if (registredChannelId.compareTo(channelId) != 0)
			return;
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		if (manager.getSiegeInstance(guildId) != null)
			manager.getSiegeInstance(guildId).addPlayer(author.getIdLong());
	}

	@Override
	public String getHelp()
	{
		return "";
	}

	@Override
	public String getInvoke()
	{
		return "+";
	}

}
