package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;

public class SetListeningChannel implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();

		if (args.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		if (!PermissionChecker.checkGeneralPermissions(event))
			return;

		List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
		if (mentionedChannels.isEmpty())
		{
			BotUtils.sendMessageToChannel(channel, "Incorrect channel mention given");
			return;
		}

		long channelId = mentionedChannels.get(0).getIdLong();
		long guildId = event.getGuild().getIdLong();
		GuildSettings settings = GuildManager.getInstance().getGuildSettings(guildId);

		GuildManager.getInstance().removeSiegeInstance(guildId);
		settings.setListeningChannel(channelId);

		BotUtils.sendMessageToChannel(channel, "Listening channel successfully set");
	}

	@Override
	public String getHelp()
	{
		return "Sets a channel that manager will listen to\n" + 
				"Usage: `" + this.getInvoke() + " <channel>`";
	}

	@Override
	public String getInvoke()
	{
		return "setchannel";
	}
}
