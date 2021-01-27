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

public class RemoveSiegeCommand implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		
		Long allowedRoleId = SiegeManager.getInstance().getManagingRole(event.getGuild().getIdLong());
		if (allowedRoleId == null)
		{
			channel.sendMessage("Siege managing role is not configured").queue(
					(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
			);
			return;
		}
		
		boolean authorHasRole = event.getMember().getRoles()
				.stream()
				.anyMatch(
					(role) -> role.getIdLong() == allowedRoleId
				);

		if (!authorHasRole)
		{
			channel.sendMessage("You do not have permissions to use this command").queue(
					(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
			);
			return;
		}

		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}

		SiegeManager.getInstance().removeSiegeInstance(channel.getGuild().getIdLong());

		channel.sendMessage("Siege successfully deleted").queue(
				(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}

	@Override
	public String getHelp()
	{
		return "Unschedules a siege info\n"
				+ "Usage: `" + Constants.PREFIX + this.getInvoke();
	}

	@Override
	public String getInvoke()
	{
		return "remsiege";
	}

}
