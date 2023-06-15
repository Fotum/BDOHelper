package org.fotum.app.features.siege;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.fotum.app.features.siege.queuehandlers.DefaultSiegeQueueHandler;
import org.fotum.app.interfaces.ISiegeQueueHandler;
import org.fotum.app.structs.DiscordMemberInfo;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.utils.DiscordObjectsGetters;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SiegeInstance extends Thread
{
	@Getter
	private final GuildSettings settings;

	private boolean isRunning = false;
	private boolean needUpdate = true;
	private boolean needMention = true;

	@Getter @Setter
	private LocalDate startDt;
	@Setter
	private String zone;
	private String titleMessage;
	@Setter
	private String descriptionMessage;
	@Getter
	private boolean buttonsDisabled = false;

	@Getter
	private long siegeAnnounceMsgId;
	private long messageMentionId;
	private final long channelId;
	private final long guildId;

	private final ISiegeQueueHandler queueHandler;
	private final EmbedBuilder eBuilder;

	public SiegeInstance(long guildId, long channelId, LocalDate startDt, String zone, int playersMax)
	{
		this.guildId = guildId;
		this.channelId = channelId;
		this.startDt = startDt;
		this.zone = zone;

		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

		this.titleMessage = String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone);
		this.descriptionMessage = "Взять с собой ловушки, банки, топоры Трины, китовки, гиганты, колбы, пвп и пве обеды на свап и т.д.";
		this.siegeAnnounceMsgId = 0L;
		this.messageMentionId = 0L;

		this.eBuilder = new EmbedBuilder().setColor(BotUtils.getRandomColor());
		this.settings = GuildManager.getInstance().getGuildSettings(guildId);
		this.queueHandler = new DefaultSiegeQueueHandler(this, playersMax);
	}

	public SiegeInstance(long guildId, JSONObject instance)
	{
		this(
			guildId,
			instance.getLong("channel_id"),
			LocalDate.parse(instance.getString("start_dt"), DateTimeFormatter.ofPattern("dd.MM.yyyy")),
			instance.getString("zone"),
			instance.getInt("players_max")
		);

		this.titleMessage = instance.getString("title_message");
		this.descriptionMessage = instance.getString("description_message");

		// Queue handler parameters
		JSONArray registredPlayersJson = instance.getJSONArray("registered_players");
		for (int i = 0; i < registredPlayersJson.length(); i++)
		{
			this.queueHandler.registerPlayer(registredPlayersJson.getLong(i));
		}

		JSONArray latePlayersJson = instance.getJSONArray("late_players");
		for (int i = 0; i < latePlayersJson.length(); i++)
		{
			this.queueHandler.registerPlayer(latePlayersJson.getLong(i));
		}

		JSONArray unregPlayersJson = instance.getJSONArray("unregistered_players");
		for (int i = 0; i < unregPlayersJson.length(); i++)
		{
			this.queueHandler.unregisterPlayer(unregPlayersJson.getLong(i));
		}

		// If we are loaded from settings - no need to mention roles
		this.needMention = false;
	}

	@Override
	public void run()
	{
		this.isRunning = true;

		// Generate mention
		if (this.needMention)
			this.generateAndSendMentionMessage();

		while (this.isRunning)
		{
			try
			{
				Message announceMsg = DiscordObjectsGetters.getMessageById(this.channelId, this.siegeAnnounceMsgId);
				if (announceMsg == null)
					this.needUpdate = true;

				if (!Objects.isNull(DiscordObjectsGetters.getGuildById(this.guildId))
						&& !Objects.isNull(DiscordObjectsGetters.getTextChannelById(this.channelId))
						&& this.needUpdate)
				{
					this.generateAndSendSiegeEmbed();
				}

				TimeUnit.SECONDS.sleep(10L);
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
		if (!this.isRunning())
			return;

		this.isRunning = false;

		Message messageMention = DiscordObjectsGetters.getMessageById(this.channelId, this.messageMentionId);
		if (messageMention != null)
			messageMention.delete().complete();

		Message siegeAnnounceMsg = DiscordObjectsGetters.getMessageById(this.channelId, this.siegeAnnounceMsgId);
		if (siegeAnnounceMsg != null)
			siegeAnnounceMsg.delete().complete();
	}

	@Synchronized
	public void registerPlayer(Long discordId)
	{
		boolean changeDone = this.queueHandler.registerPlayer(discordId);
		if (!this.needUpdate && changeDone)
			this.needUpdate = true;
	}

	@Synchronized
	public void unregisterPlayer(Long discordId)
	{
		boolean changeDone = this.queueHandler.unregisterPlayer(discordId);
		if (!this.needUpdate && changeDone)
			this.needUpdate = true;
	}

	@Synchronized
	public void updateTitle()
	{
		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

		this.titleMessage = String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone);

		if (!this.needUpdate)
			this.needUpdate = true;
	}

	@Synchronized
	public void updatePlayersMax(int newValue)
	{
		this.queueHandler.setPlayersMax(newValue);

		if (!this.needUpdate)
			this.needUpdate = true;
	}

	public JSONObject toJSON()
	{
		JSONObject instance = new JSONObject();

		instance.put("channel_id", this.channelId);
		instance.put("start_dt", this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
		instance.put("zone", this.zone);
		instance.put("title_message", this.titleMessage);
		instance.put("description_message", this.descriptionMessage);

		// Queue handler parameters
		instance.put("players_max", this.queueHandler.getPlayersMax());
		instance.put("registered_players", this.queueHandler.getRegisteredPlayers());
		instance.put("late_players", this.queueHandler.getLatePlayers());
		instance.put("unregistered_players", this.queueHandler.getUnregisteredPlayers());

		return instance;
	}

	@Synchronized
	public void setButtonsDisabled(boolean newValue)
	{
		this.buttonsDisabled = newValue;

		if (!this.needUpdate)
			this.needUpdate = true;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null)
			return false;

		if (!(other instanceof SiegeInstance))
			return false;

		SiegeInstance otherInst = (SiegeInstance) other;
		return otherInst.getStartDt().isEqual(this.getStartDt());
	}

	private void generateAndSendMentionMessage()
	{
		// Sending mention to announce new instance
		if (!this.settings.getMentionRoles().isEmpty())
		{
			StringBuilder rolesToMention = new StringBuilder();
			Iterator<Long> mentionIter = this.settings.getMentionRoles().iterator();
			while (mentionIter.hasNext())
			{
				Role toAdd = DiscordObjectsGetters.getGuildRoleById(this.guildId, mentionIter.next());
				if (toAdd != null)
					rolesToMention.append(toAdd.getAsMention());
				else
					mentionIter.remove();
			}

			if (rolesToMention.length() != 0)
			{
				DiscordObjectsGetters.getTextChannelById(this.channelId)
						.sendMessage(rolesToMention.toString()).queue(
								(message) -> this.messageMentionId = message.getIdLong()
						);
			}
		}

		this.needMention = false;
	}

	@Synchronized
	private void generateAndSendSiegeEmbed()
	{
		// Конвертируем айдишники в объекты класса Member
		Set<Member> registredMembers = this.convertToMembers(this.queueHandler.getRegisteredPlayers());
		Set<Member> unregisteredMembers = this.convertToMembers(this.queueHandler.getUnregisteredPlayers());
		Set<Member> lateMembers = this.convertToMembers(this.queueHandler.getLatePlayers());

		int slotsRemain = this.queueHandler.getPlayersMax() - registredMembers.size();

		// Устанавливаем основные параметры эмбеда
		this.eBuilder.setTitle(this.titleMessage);
		this.eBuilder.setDescription(this.descriptionMessage);
		this.eBuilder.clearFields();
		this.eBuilder.addField("Плюсов на осаду", String.valueOf(registredMembers.size()), true);
		this.eBuilder.addField("Осталось слотов", String.valueOf(slotsRemain), true);
		this.eBuilder.addBlankField(true);

		// Registered players embed field
		String regPlayersFieldText = this.getFieldTextString(registredMembers);
		if (!regPlayersFieldText.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Придут", regPlayersFieldText, true);

		// Unregistered players embed field
		String unregPlayersFieldText = this.getFieldTextString(unregisteredMembers);
		if (!unregPlayersFieldText.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Не придут", unregPlayersFieldText, true);

		// Late players embed field
		String latePlayersFieldText = this.getFieldTextString(lateMembers);
		if (!latePlayersFieldText.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Нет слота", latePlayersFieldText, false);

		// Получаем сообщение об осаде. Если сообщения не существует, то отправляем новое
		// Если сообщение существует, то редактируем его
		Message siegeAnnounceMsg = DiscordObjectsGetters.getMessageById(this.channelId, this.siegeAnnounceMsgId);
		if (siegeAnnounceMsg != null)
		{
			siegeAnnounceMsg
					.editMessageEmbeds(this.eBuilder.build())
					.setActionRow(
						Button.success("button-plus", "➕").withDisabled(this.buttonsDisabled),
						Button.danger("button-minus", "➖").withDisabled(this.buttonsDisabled),
						// Imperium TeamSpeak 3
						Button.link("https://invite.teamspeak.com/dragonel", "TeamSpeak 3"))
					.queue();
		}
		else
		{
			DiscordObjectsGetters.getTextChannelById(this.channelId)
					.sendMessageEmbeds(this.eBuilder.build())
					.setActionRow(
						Button.success("button-plus", "➕").withDisabled(this.buttonsDisabled),
						Button.danger("button-minus", "➖").withDisabled(this.buttonsDisabled),
						// Imperium TeamSpeak 3
						Button.link("https://invite.teamspeak.com/dragonel", "TeamSpeak 3"))
					.queue(
						(message) -> this.siegeAnnounceMsgId = message.getIdLong()
					);
		}

		this.needUpdate = false;
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

	private Set<Member> convertToMembers(Set<Long> memberIds)
	{
		Set<Member> result = new LinkedHashSet<>();

		Iterator<Long> memberIdsIter = memberIds.iterator();
		while (memberIdsIter.hasNext())
		{
			Member member = DiscordObjectsGetters.getGuildMemberById(this.guildId, memberIdsIter.next());
			if (member != null)
				result.add(member);
			else
				memberIdsIter.remove();
		}

		return result;
	}

	private String getFieldTextString(Set<Member> toConvert)
	{
		List<DiscordMemberInfo> regList = this.settings.getRegisteredMembers();
		return toConvert.stream()
				.map(
					(member) -> {
						DiscordMemberInfo info = regList
								.stream()
								.filter(
									(inf) -> inf.getDiscordId() == member.getIdLong()
								).findFirst()
								.orElse(null);

						if (info == null)
							return String.format("%s", member.getAsMention());
						else
							return String.format("%s: %s (%s)", info.getAllegiance(), info.getBdoName(), member.getAsMention());
					}
				)
				.collect(Collectors.joining("\n"));
	}
}
