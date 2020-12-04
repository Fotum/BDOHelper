package org.fotum.app.commands.siege;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.SiegeManager;
import org.fotum.app.objects.EmbedCreator;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GetRegisteredPlayers implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		TextChannel channel = event.getChannel();
		Member selfMember = event.getGuild().getSelfMember();
		SiegeManager manager = SiegeManager.getInstance();
		SiegeInstance inst = manager.getSiegeInstance(event.getGuild().getIdLong());

		if (inst == null)
			return;
		
		Long allowedRoleId = manager.getManagingRole(event.getGuild().getIdLong());
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

		String registredPlayers = this.convertFromSet(event.getGuild(), inst.getRegistredPlayers());
		String latePlayers = this.convertFromSet(event.getGuild(), inst.getLatePlayers());

		if (registredPlayers.isEmpty() && latePlayers.isEmpty())
		{
			channel.sendMessage("Both lists of participants are empty").queue(
					(message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
			);
			return;
		}

		EmbedBuilder embed = EmbedCreator.getDefault()
				.setTitle("Список записавшихся на осаду")
				.addField("Зарегистрированы", registredPlayers, false);

		if (!latePlayers.isEmpty())
			embed.addField("Опоздашки", latePlayers, false);

		channel.sendMessage(embed.build()).queue();
	}

	@Override
	public String getHelp()
	{
		return "Returns a list of registred and late players" + 
				"Usage: `" + Constants.PREFIX + this.getInvoke();
	}

	@Override
	public String getInvoke()
	{
		return "getlist";
	}

	private String convertFromSet(Guild guild, Set<Long> players)
	{
		String result = "";

		if (players == null || players.isEmpty())
			return result;

		ArrayList<String> members = new ArrayList<String>();
		players.stream()
			.forEach((player) ->
				{
					Member member = guild.getMemberById(player);
					String memberFmt = String.format("%s", member.getAsMention());
					members.add(memberFmt);
				}
			);

		result = String.join("\n", members);
		return result;
	}
}
