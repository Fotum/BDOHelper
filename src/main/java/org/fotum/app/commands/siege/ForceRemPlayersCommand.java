package org.fotum.app.commands.siege;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ForceRemPlayersCommand implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		Guild guild = event.getGuild();
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		
		if (args.isEmpty())
		{
			this.sendMessageToChannel(channel, "You have to mention at least one player");
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
		
		SiegeInstance siegeInst = SiegeManager.getInstance().getSiegeInstance(guild.getIdLong());
		if (siegeInst == null)
		{
			this.sendMessageToChannel(channel, "Active siege is not found for current guild");
			return;
		}
		
		if (selfMember.hasPermission(Permission.MESSAGE_MANAGE))
		{
			event.getMessage().delete().queue();
		}
		
		List<String> mentionedStr = args.stream()
				.map(
					(memberStr) -> memberStr.substring(3, (memberStr.length() - 1))
				).collect(Collectors.toList());
		
		for (String member : mentionedStr)
		{
			try
			{
				Long memberId = Long.parseLong(member);
				siegeInst.removePlayer(memberId);
			}
			catch (NumberFormatException ex) {}
		}

		this.sendMessageToChannel(channel, "Player(s) successfully removed");
	}

	@Override
	public String getHelp()
	{
		return "Forcely remove player(s) from participants list\n" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke() + " <@player1> <@player2> ...`";
	}

	@Override
	public String getInvoke()
	{
		return "forcerem";
	}

	private void sendMessageToChannel(TextChannel channel, String msg)
	{
		channel.sendMessage(msg).queue(
				 (message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
		);
	}
}
