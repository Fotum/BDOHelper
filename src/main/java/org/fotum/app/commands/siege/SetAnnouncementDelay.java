package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetAnnouncementDelay implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Guild guild = event.getGuild();
		Member selfMember = guild.getSelfMember();
		
		if (args.size() < 2)
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
		
		int offset;
		int delay;
		try
		{
			offset = Integer.valueOf(args.get(0));
			delay = Integer.valueOf(args.get(1));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			this.sendMessageToChannel(channel, "Incorrect number given");
			return;
		}
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		SiegeManager.getInstance().getSiegeInstance(guild.getIdLong()).setAnnouncerOffset(offset);
		SiegeManager.getInstance().getSiegeInstance(guild.getIdLong()).setAnnouncerDelay(delay);
		SiegeManager.getInstance().getSiegeInstance(guild.getIdLong()).reschedule();

		this.sendMessageToChannel(channel, "Successfully updated");
	}

	@Override
	public String getHelp()
	{
		return "Sets delay between announcement messages (in minutes)\n"
				+ "Usage: `" + this.getInvoke() + " <minutes_offset> <minutes_delay>`";
	}

	@Override
	public String getInvoke()
	{
		return "setdelay";
	}
	
	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}

}
