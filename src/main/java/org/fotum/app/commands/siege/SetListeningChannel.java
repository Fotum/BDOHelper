package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetListeningChannel implements ICommand
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

		List<TextChannel> mentionedChannels = event.getMessage().getMentionedChannels();
		if (mentionedChannels.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect channel mention given");
			return;
		}

		Long channelId = mentionedChannels.get(0).getIdLong();
		Long guildId = event.getGuild().getIdLong();

		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}


		SiegeManager.getInstance().removeSiegeInstance(guildId);
		SiegeManager.getInstance().removeListeningChannel(guildId);
		SiegeManager.getInstance().addListeningChannel(guildId, channelId);

		this.sendMessageToChannel(channel, "Listening channel successfully set");
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

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
