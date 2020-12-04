package org.fotum.app.features.siege;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.fotum.app.MainApp;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class SiegeInstance implements Runnable
{
	private final ScheduledThreadPoolExecutor scheduler;

	private LocalDate startDt;
	private int playersMax;
	private int slotsRemain;
	private String zone;

	private String titleMessage;
	private String descriptionMessage;
	private int announcerDelay;
	private int announcerOffset;

	private Set<Long> registredPlayers;
	private Set<Long> latePlayers;
	private Set<Long> prefixRoles;
	private ScheduledFuture<?> schedFuture = null;

	private TextChannel channel;
	private Message messageToEdit;
	private Message messageMention;
	private EmbedBuilder eBuilder;

	public SiegeInstance()
	{
		this.announcerOffset = 0;
		this.announcerDelay = 5;
		this.titleMessage = "Осада в %s (%s) на канале - %s 1";
		this.descriptionMessage = "Взять с собой Топор трины +18, Карки и Гиганты. Оставляйте заявки на осаду: \"+\"";
		this.messageToEdit = null;
		this.messageMention = null;

		this.registredPlayers = new LinkedHashSet<Long>();
		this.latePlayers = new LinkedHashSet<Long>();
		this.prefixRoles = new LinkedHashSet<Long>();

		this.scheduler = new ScheduledThreadPoolExecutor(1);
		this.scheduler.setRemoveOnCancelPolicy(true);

		this.eBuilder = new EmbedBuilder().setColor(MainApp.getRandomColor());
	}

	public void run()
	{
		// HEAP
		if (this.channel.getGuild().getIdLong() == 251095848568619008L && this.messageMention == null)
		{
			this.channel.sendMessage("<@&607655302871121931>").queue(
				(message) -> this.messageMention = message
			);
		}

		this.slotsRemain = playersMax - registredPlayers.size();
		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		String announce = String.format(this.titleMessage, dateStr, dayOfWeek, this.zone);

		this.eBuilder.setTitle(announce);
		this.eBuilder.setDescription(this.descriptionMessage);
		this.eBuilder.clearFields();
		this.eBuilder.addField("Плюсов на осаду", String.valueOf(this.registredPlayers.size()), true);
		this.eBuilder.addField("Осталось слотов", String.valueOf(this.slotsRemain), true);
		this.eBuilder.addBlankField(true);

		for (Long roleId : this.prefixRoles)
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
			this.messageToEdit.editMessage(eBuilder.build()).queue();
		}
		else
		{
			this.channel.sendMessage(eBuilder.build()).queue(
				(message) -> this.messageToEdit = message
			);
		}
	}

	public void schedule()
	{
		if (LocalDateTime.now().isAfter(this.startDt.atTime(19, 50)))
			return;

		if (this.schedFuture != null && !this.schedFuture.isDone())
			return;

		if (this.messageToEdit != null)
		{
			TextChannel prevChannel = this.messageToEdit.getTextChannel();
			if (prevChannel != null && prevChannel.getIdLong() != this.channel.getIdLong())
				this.messageToEdit.delete().complete();
		}

		try
		{
			this.schedFuture = scheduler.scheduleAtFixedRate(this, this.announcerOffset, this.announcerDelay, TimeUnit.SECONDS);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public void unschedule()
	{
		if (this.schedFuture != null && !this.schedFuture.isDone())
			this.schedFuture.cancel(false);
		
		if (this.messageMention != null)
			this.messageMention.delete().complete();
	}

	public void reschedule()
	{	
		this.unschedule();
		this.schedule();
	}

	public void reinit()
	{
		this.unschedule();

		this.startDt = null;
		this.zone = null;
		this.playersMax = 0;
		this.channel = null;

		this.registredPlayers.clear();
		this.latePlayers.clear();
		this.schedFuture = null;
	}

	public void addPlayer(Long discordId)
	{
		if (this.schedFuture != null && this.schedFuture.isDone())
			return;
		
		if (this.registredPlayers.contains(discordId) || this.latePlayers.contains(discordId))
			return;
		
		if (this.registredPlayers.size() < this.playersMax)
			this.registredPlayers.add(discordId);
		else
			this.latePlayers.add(discordId);
	}

	public void removePlayer(Long discordId)
	{
		if (this.schedFuture != null && this.schedFuture.isDone())
			return;
		
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

	public LocalDate getStartDt()
	{
		return startDt;
	}

	public void setStartDt(LocalDate startDt)
	{
		this.startDt = startDt;
	}

	public int getPlayersMax()
	{
		return playersMax;
	}

	public void setPlayersMax(int playersMax)
	{
		this.playersMax = playersMax;
	}

	public int getSlotsRemain()
	{
		return slotsRemain;
	}

	public void setSlotsRemain(int slotsRemain)
	{
		this.slotsRemain = slotsRemain;
	}

	public String getZone()
	{
		return zone;
	}

	public void setZone(String zone)
	{
		this.zone = zone;
	}

	public String getTitleMessage()
	{
		return titleMessage;
	}

	public void setTitleMessage(String titleMessage)
	{
		this.titleMessage = titleMessage;
	}

	public String getDescriptionMessage()
	{
		return descriptionMessage;
	}

	public void setDescriptionMessage(String descriptionMessage)
	{
		this.descriptionMessage = descriptionMessage;
	}

	public int getAnnouncerDelay()
	{
		return announcerDelay;
	}

	public void setAnnouncerDelay(int announcerDelay)
	{
		this.announcerDelay = announcerDelay;
	}

	public int getAnnouncerOffset()
	{
		return announcerOffset;
	}

	public void setAnnouncerOffset(int announcerOffset)
	{
		this.announcerOffset = announcerOffset;
	}

	public Set<Long> getRegistredPlayers()
	{
		return registredPlayers;
	}

	public void setRegistredPlayers(Set<Long> registredPlayers)
	{
		this.registredPlayers = registredPlayers;
	}

	public Set<Long> getLatePlayers()
	{
		return latePlayers;
	}

	public void setLatePlayers(Set<Long> latePlayers)
	{
		this.latePlayers = latePlayers;
	}

	public TextChannel getChannel()
	{
		return channel;
	}

	public void setChannel(TextChannel channel)
	{
		this.channel = channel;
	}

	public Message getMessageToEdit()
	{
		return messageToEdit;
	}

	public void setMessageToEdit(Message messageToEdit)
	{
		if (this.messageToEdit != null)
		{
			try
			{
				this.messageToEdit.delete().complete();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}
		
		this.messageToEdit = messageToEdit;
	}

	public Set<Long> getPrefixRoles()
	{
		return prefixRoles;
	}

	public void setPrefixRoles(Set<Long> prefixRoles)
	{
		this.prefixRoles = prefixRoles;
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
		List<Role> prefixes = this.prefixRoles.stream()
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
