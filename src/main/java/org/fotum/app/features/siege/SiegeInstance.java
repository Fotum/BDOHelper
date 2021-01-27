package org.fotum.app.features.siege;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.MainApp;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class SiegeInstance extends Thread
{
	private boolean isRunning = false;
	private final SiegeManager manager = SiegeManager.getInstance();
	private Long guildId;

	@Getter
	private LocalDate startDt;
	private int playersMax;
	private String zone;

	@Setter
	private String titleMessage;
	@Setter
	private String descriptionMessage;
	private Long announcerDelay;

	private Set<Long> registredPlayers;
	private Set<Long> latePlayers;

	private TextChannel channel;
	private Message messageToEdit;
	private Message messageMention;
	private EmbedBuilder eBuilder;
	
	public SiegeInstance(Long guildId, TextChannel channel, LocalDate startDt, String zone, int playersMax)
	{
		this.guildId = guildId;
		this.channel = channel;
		this.startDt = startDt;
		this.zone = zone;
		this.playersMax = playersMax;
		
		this.announcerDelay = 5L;
		this.titleMessage = "Осада %s (%s) на канале - %s 1";
		this.descriptionMessage = "Взять с собой Топор трины +18, Карки и Гиганты. Оставляйте заявки на осаду: \"+\"";
		this.messageToEdit = null;
		this.messageMention = null;

		this.registredPlayers = new LinkedHashSet<Long>();
		this.latePlayers = new LinkedHashSet<Long>();
		this.eBuilder = new EmbedBuilder().setColor(MainApp.getRandomColor());
	}
	
	@Override
	public void run()
	{
		this.isRunning = true;
		// HEAP
		if (this.guildId == 251095848568619008L)
		{
			this.channel.sendMessage("<@&607655302871121931>").queue(
				(message) -> this.messageMention = message
			);
		}

		while (isRunning)
		{
			int slotsRemain = playersMax - registredPlayers.size();
			String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
			String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
			String announce = String.format(this.titleMessage, dateStr, dayOfWeek, this.zone);

			this.eBuilder.setTitle(announce);
			this.eBuilder.setDescription(this.descriptionMessage);
			this.eBuilder.clearFields();
			this.eBuilder.addField("Плюсов на осаду", String.valueOf(this.registredPlayers.size()), true);
			this.eBuilder.addField("Осталось слотов", String.valueOf(slotsRemain), true);
			this.eBuilder.addBlankField(true);

			for (Long roleId : manager.getPrefixRolesById(this.guildId))
			{
				Role prefixRole = this.channel.getGuild().getRoleById(roleId);
				String fieldText = this.convertPlayersSetByRole(this.registredPlayers, prefixRole);
				if (!fieldText.isEmpty())
					this.addFieldToEmbed(this.eBuilder, prefixRole.getName(), fieldText, true);
			}

			String noRoleList = this.convertPlayersWithoutRole(this.registredPlayers);
			if (!noRoleList.isEmpty())
				this.addFieldToEmbed(this.eBuilder, "Без роли", noRoleList, true);

			String lateList = this.latePlayers.stream()
								.map(
									(memberId) -> String.format("%s", this.channel.getGuild().getMemberById(memberId).getAsMention())
								).collect(Collectors.joining("\n"));

			if (!lateList.isEmpty())
				this.addFieldToEmbed(this.eBuilder, "Опоздашки", lateList, true);

			if (this.messageToEdit != null)
			{
				this.messageToEdit
					.editMessage(eBuilder.build())
					.queue(null, new ErrorHandler()
									.handle(ErrorResponse.UNKNOWN_MESSAGE, (ex) -> this.messageToEdit = null)
					);
			}
			else
			{
				this.channel.sendMessage(eBuilder.build()).queue(
					(message) -> this.messageToEdit = message
				);
			}
			
			try
			{
				TimeUnit.SECONDS.sleep(announcerDelay);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
				this.stopInstance();
			}
		}
	}
	
	@Synchronized
	public boolean isRunning()
	{
		return this.isRunning;
	}
	
	@Synchronized
	public void stopInstance()
	{
		if (!this.isRunning)
			return;
		
		this.isRunning = false;
		
		if (this.messageMention != null)
			this.messageMention.delete().queue(null, new ErrorHandler()
															.handle(ErrorResponse.UNKNOWN_MESSAGE, (ex) -> ex.printStackTrace())
			);
		
		if (this.messageToEdit != null)
			this.messageToEdit.delete().queue(null, new ErrorHandler()
															.handle(ErrorResponse.UNKNOWN_MESSAGE, (ex) -> ex.printStackTrace())
			);
	}

	@Synchronized
	public void addPlayer(Long discordId)
	{
		if (this.registredPlayers.contains(discordId) || this.latePlayers.contains(discordId))
			return;
		
		if (this.registredPlayers.size() < this.playersMax)
			this.registredPlayers.add(discordId);
		else
			this.latePlayers.add(discordId);
	}

	@Synchronized
	public void removePlayer(Long discordId)
	{
		if (this.registredPlayers.contains(discordId))
		{
			this.registredPlayers.remove(discordId);
			Iterator<Long> latePlayersIter = this.latePlayers.iterator();
			if (latePlayersIter.hasNext())
			{
				this.registredPlayers.add(latePlayersIter.next());
				latePlayersIter.remove();
			}
		}
		else if (this.latePlayers.contains(discordId))
		{
			this.latePlayers.remove(discordId);
		}
	}

	private void addFieldToEmbed(EmbedBuilder builder, String fieldNm, String fieldVal, boolean inline)
	{
		if (fieldVal.length() > MessageEmbed.VALUE_MAX_LENGTH)
		{
			int subListStart = -1;
			int strLen = 0;
			List<String> tmpVal = Arrays.asList(fieldVal.split("\n"));
			for (int i = 0; i < tmpVal.size() && subListStart == -1; i++)
			{
				String elem = tmpVal.get(i);
				int elemLen = elem.length();
				strLen += (elemLen + 1);
				
				if (strLen > MessageEmbed.VALUE_MAX_LENGTH)
					subListStart = i - 1;
			}
			
			builder.addField(fieldNm, String.join("\n", tmpVal.subList(0, subListStart)), inline);
			this.addFieldToEmbed(builder, "", String.join("\n", tmpVal.subList(subListStart, tmpVal.size())), inline);
		}
		else
		{
			builder.addField(fieldNm, fieldVal, inline);
		}
	}

	private String convertPlayersSetByRole(Set<Long> playersSet, Role role)
	{
		Guild guild = this.channel.getGuild();
		List<Member> members = playersSet.stream()
								.map((memId) -> guild.getMemberById(memId))
								.collect(Collectors.toList());

		String filtered = members.stream()
							.filter(
								(member) -> {
									List<Role> memberRoles = member.getRoles();
									return memberRoles.contains(role);
								}
							).map(
								(member) -> String.format("%s", member.getAsMention())
							).collect(Collectors.joining("\n"));

		return filtered;
	}

	private String convertPlayersWithoutRole(Set<Long> playersSet)
	{
		Guild guild = this.channel.getGuild();
		List<Role> prefixes = manager.getPrefixRolesById(this.guildId)
								.stream()
								.map((roleId) -> guild.getRoleById(roleId))
								.collect(Collectors.toList());
		List<Member> members = playersSet.stream()
								.map((memId) -> guild.getMemberById(memId))
								.collect(Collectors.toList());

		String filtered = members.stream()
				.filter(
					(member) -> {
						List<Role> memberRoles = member.getRoles();
						return !memberRoles.stream().anyMatch((memberRole) -> prefixes.contains(memberRole));
					}
				).map(
					(member) -> String.format("%s", member.getAsMention())
				).collect(Collectors.joining("\n"));

		return filtered;
	}
}
