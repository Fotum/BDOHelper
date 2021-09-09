package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;

import java.util.ArrayList;
import java.util.List;

public class RemoveManagingRoles implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		Member author = event.getMember();

		if (!author.hasPermission(Permission.MANAGE_SERVER))
		{
			BotUtils.sendMessageToChannel(channel, "You dont have permission to use this command");
			return;
		}

		long guildId = event.getGuild().getIdLong();
		GuildSettings settings = GuildManager.getInstance().getGuildSettings(guildId);
		if (settings == null)
		{
			BotUtils.sendMessageToChannel(channel, "No settings to remove for this guild");
			return;
		}
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}

		settings.setManagingRoles(new ArrayList<Long>());
		BotUtils.sendMessageToChannel(channel, "Managing roles successfully removed");
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
}
