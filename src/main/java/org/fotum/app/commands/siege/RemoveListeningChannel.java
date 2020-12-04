package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class RemoveListeningChannel implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();

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
		
		Long guildId = event.getGuild().getIdLong();
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		if (SiegeManager.getInstance().getSiegeInstance(guildId) != null)
			SiegeManager.getInstance().getSiegeInstance(guildId).unschedule();
		SiegeManager.getInstance().removeListeningChannel(guildId);

		this.sendMessageToChannel(channel, "Listening channel successfully removed");
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

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
