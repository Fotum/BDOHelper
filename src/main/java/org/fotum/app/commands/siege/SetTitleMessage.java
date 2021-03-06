package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetTitleMessage implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		
		if (args.isEmpty())
		{
			this.sendMessageToChannel(channel, "Incorrect number of arguments given");
			return;
		}

		Long allowedRoleId = SiegeManager.getInstance().getManagingRole(guild.getIdLong());
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
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		SiegeInstance siegeInst = SiegeManager.getInstance().getSiegeInstance(guild.getIdLong());
		if (siegeInst == null)
		{
			this.sendMessageToChannel(channel, "Active siege is not found for current guild");
			return;
		}

		String title = String.join(" ", args);
		siegeInst.setTitleMessage(title);
		this.sendMessageToChannel(channel, "Title successfully set");
	}

	@Override
	public String getHelp()
	{
		return "Sets title message for siege announcement message";
	}

	@Override
	public String getInvoke()
	{
		return "settitle";
	}
	
	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
