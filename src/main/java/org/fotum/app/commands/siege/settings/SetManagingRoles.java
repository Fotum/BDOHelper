package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;

import java.util.List;
import java.util.stream.Collectors;

public class SetManagingRoles implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		
		if (!event.getMember().hasPermission(Permission.MANAGE_SERVER))
		{
			BotUtils.sendMessageToChannel(channel, "You dont have permission to use this command");
			return;
		}
		
		if (args.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}
		
		List<Role> roles = event.getMessage().getMentionedRoles();
		if (roles.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect role mention given");
			return;
		}

		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}

		long guildId = event.getGuild().getIdLong();
		GuildSettings settings = GuildManager.getInstance().getGuildSettings(guildId);
		if (settings == null)
		{
			settings = new GuildSettings();
			GuildManager.getInstance().addGuildSettings(guildId, settings);
		}
		
		List<Long> roleIds = roles.stream()
								.map(ISnowflake::getIdLong)
								.collect(Collectors.toList());

		settings.setManagingRoles(roleIds);
		BotUtils.sendMessageToChannel(channel, "Managing role successfully set");
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
}
