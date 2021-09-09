package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;

public class RemoveListeningChannel implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		long guildId = event.getGuild().getIdLong();
		TextChannel channel = event.getChannel();
		GuildSettings settings = GuildManager.getInstance().getGuildSettings(guildId);

		if (!PermissionChecker.checkGeneralPermissions(event))
			return;

		GuildManager.getInstance().removeSiegeInstance(guildId);
		settings.setListeningChannel(0L);

		BotUtils.sendMessageToChannel(channel, "Listening channel successfully removed");
	}

	@Override
	public String getHelp()
	{
		return "Removes a channel that manager will listen to\n" + 
				"Usage: `" + this.getInvoke();
	}

	@Override
	public String getInvoke()
	{
		return "remchannel";
	}
}
