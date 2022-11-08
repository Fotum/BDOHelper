package org.fotum.app.features.siege;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
	private final GuildSettings settings;

	private boolean isRunning = false;

	@Getter @Setter
	private LocalDate startDt;
	@Setter
	private String zone;
	@Setter
	private int playersMax;
	private String titleMessage;
	@Setter
	private String descriptionMessage;

	private long siegeAnnounceMsgId;
	private long messageMentionId;
	private final long channelId;
	private final long guildId;

	private final Set<Long> registredPlayers;
	private Set<Long> latePlayers;

	private TextChannel channel;
	private Guild guild;
	private final EmbedBuilder eBuilder;

	public SiegeInstance(long guildId, long channelId, LocalDate startDt, String zone, int playersMax)
	{
		this.guildId = guildId;
		this.channelId = channelId;
		this.startDt = startDt;
		this.zone = zone;
		this.playersMax = playersMax;

		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

		this.titleMessage = String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone);
		this.descriptionMessage = "Взять с собой ловушки, банки, топоры Трины, китовки, гиганты, колбы, пвп и пве обеды на свап и т.д.";
		this.siegeAnnounceMsgId = 0L;
		this.messageMentionId = 0L;

		this.registredPlayers = new LinkedHashSet<>();
		this.latePlayers = new LinkedHashSet<>();
		this.eBuilder = new EmbedBuilder().setColor(BotUtils.getRandomColor());
		this.settings = GuildManager.getInstance().getGuildSettings(guildId);
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

		JSONArray registredPlayersJson = instance.getJSONArray("registred_players");
		for (int i = 0; i < registredPlayersJson.length(); i++)
		{
			this.registredPlayers.add(registredPlayersJson.getLong(i));
		}

		JSONArray latePlayersJson = instance.getJSONArray("late_players");
		for (int i = 0; i < latePlayersJson.length(); i++)
		{
			this.latePlayers.add(latePlayersJson.getLong(i));
		}
	}

	@Override
	public void run()
	{
		this.isRunning = true;

		while (this.isRunning)
		{
			try
			{
				if (this.initApiObjects())
				{
					this.generateAndSendMentionMessage();
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
				long lateId = latePlayersIter.next();

				this.registredPlayers.add(lateId);
				latePlayersIter.remove();

				User userToNotify = DiscordObjectsGetters.getUserById(lateId);
				BotUtils.sendDirectMessage(userToNotify, String.format("Для Вас появился слот на осаду и вы были перенесены в список участников.\r\n" +
						"Ждем Вас на осаде **%s (%s)**.",
						this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
						this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")))
				);
			}
		}
		else
		{
			this.latePlayers.remove(discordId);
		}
	}

	public void updateTitle()
	{
		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

		this.titleMessage = String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone);
	}

	@Synchronized
	public void rearrangePlayers()
	{
		int diff = this.playersMax - this.registredPlayers.size();

		// If registred < max players then add players from late list to registred ones
		if (diff > 0 && this.latePlayers.size() > 0)
		{
			Iterator<Long> lateIterator = this.latePlayers.iterator();
			while (lateIterator.hasNext() && diff != 0)
			{
				Long player = lateIterator.next();
				this.registredPlayers.add(player);
				lateIterator.remove();
				diff--;
			}
		}
		// If registred > max players then add players from registred list to late ones
		else if (diff < 0)
		{
			LinkedHashSet<Long> tmpLate = new LinkedHashSet<>();
			ArrayList<Long> asArray = new ArrayList<>(this.registredPlayers);
			Collections.reverse(asArray);

			for (int i = 0; i < Math.abs(diff); i++)
			{
				tmpLate.add(asArray.get(i));
			}

			this.registredPlayers.removeAll(tmpLate);
			tmpLate.addAll(this.latePlayers);
			this.latePlayers = tmpLate;
		}
	}

	@Synchronized
	public JSONObject toJSON()
	{
		JSONObject instance = new JSONObject();

		instance.put("channel_id", this.channelId);
		instance.put("start_dt", this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
		instance.put("zone", this.zone);
		instance.put("players_max", this.playersMax);
		instance.put("title_message", this.titleMessage);
		instance.put("description_message", this.descriptionMessage);
		instance.put("registred_players", this.registredPlayers);
		instance.put("late_players", this.latePlayers);

		return instance;
	}

	private void generateAndSendMentionMessage()
	{
		// Sending mention to announce new instance
		if (!this.settings.getMentionRoles().isEmpty() && this.messageMentionId == 0L)
		{
			StringBuilder rolesToMention = new StringBuilder();
			Iterator<Long> mentionIter = this.settings.getMentionRoles().iterator();
			while (mentionIter.hasNext())
			{
				Role toAdd = this.guild.getRoleById(mentionIter.next());
				if (toAdd != null)
					rolesToMention.append(toAdd.getAsMention());
				else
					mentionIter.remove();
			}

			if (rolesToMention.length() != 0)
			{
				this.channel.sendMessage(rolesToMention.toString()).queue(
						(message) -> this.messageMentionId = message.getIdLong()
				);
			}
		}
	}

	private void generateAndSendSiegeEmbed()
	{
		// Конвертируем айдишники в объекты класса Member
		Set<Member> registredMembers = this.convertToMembers(this.registredPlayers);
		Set<Member> lateMembers = this.convertToMembers(this.latePlayers);
		// Конвертируем айдишники в объекты класса Role
		Set<Role> prefixRoles = this.convertToRoles(this.settings.getPrefixRoles());

		int slotsRemain = this.playersMax - registredMembers.size();

		// Устанавливаем основные параметры эмбеда
		this.eBuilder.setTitle(this.titleMessage);
		this.eBuilder.setDescription(this.descriptionMessage);
		this.eBuilder.clearFields();
		this.eBuilder.addField("Плюсов на осаду", String.valueOf(registredMembers.size()), true);
		this.eBuilder.addField("Осталось слотов", String.valueOf(slotsRemain), true);
		this.eBuilder.addBlankField(true);

		// Итерируемся по префикс ролям (заголовки полей) и создаем поле эмбеда
		// со списком участников, которые имеют роль prefixRole
		for (Role role : prefixRoles)
		{
			String fieldText = registredMembers.stream()
					.filter((member) -> member.getRoles().contains(role))
					.map((member) -> String.format("%s", member.getAsMention()))
					.collect(Collectors.joining("\n"));

			if (!fieldText.isEmpty())
				this.addFieldToEmbed(this.eBuilder, role.getName(), fieldText, true);
		}

		// Генерируем список мемберов без префиксных ролей
		String noRoleList = registredMembers.stream()
				.filter((member) -> Collections.disjoint(member.getRoles(), prefixRoles))
				.map((member) -> String.format("%s", member.getAsMention()))
				.collect(Collectors.joining("\n"));

		if (!noRoleList.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Без роли", noRoleList, true);

		// Генерируем лист опоздашек
		String lateList = lateMembers.stream()
				.map((member) -> String.format("%s", member.getAsMention()))
				.collect(Collectors.joining("\n"));

		if (!lateList.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Опоздашки", lateList, true);

		// Получаем сообщение об осаде. Если сообщения не существует, то отправляем новое
		// Если сообщение существует, то редактируем его
		Message siegeAnnounceMsg = DiscordObjectsGetters.getMessageById(this.channelId, this.siegeAnnounceMsgId);
		if (siegeAnnounceMsg != null)
		{
			siegeAnnounceMsg
				.editMessageEmbeds(this.eBuilder.build())
				.queue();
		}
		else
		{
			this.channel.sendMessageEmbeds(this.eBuilder.build())
				.setActionRow(
					Button.success("button-plus", "➕"),
					Button.danger("button-minus", "➖"),
					// HEAP TeamSpeak 3
//					Button.link("https://invite.teamspeak.com/176.31.211.104/?port=9500", "TeamSpeak 3"))
					// p2w TeamSpeak 3
					Button.link("https://invite.teamspeak.com/dragonel", "TeamSpeak 3"))
				.queue(
					(message) -> this.siegeAnnounceMsgId = message.getIdLong()
				);
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

	private Set<Member> convertToMembers(Set<Long> memberIds)
	{
		Set<Member> result = new LinkedHashSet<>();

		Iterator<Long> memberIdsIter = memberIds.iterator();
		while (memberIdsIter.hasNext())
		{
			Member member = this.guild.getMemberById(memberIdsIter.next());
			if (member != null)
				result.add(member);
			else
				memberIdsIter.remove();
		}

		return result;
	}

	private Set<Role> convertToRoles(Set<Long> roleIds)
	{
		Set<Role> result = new LinkedHashSet<>();

		Iterator<Long> roleIdsIter = roleIds.iterator();
		while (roleIdsIter.hasNext())
		{
			Role role = this.guild.getRoleById(roleIdsIter.next());
			if (role != null)
				result.add(role);
			else
				roleIdsIter.remove();
		}

		return result;
	}

	private boolean initApiObjects()
	{
		this.guild = DiscordObjectsGetters.getGuildById(this.guildId);
		this.channel = DiscordObjectsGetters.getTextChannelById(this.channelId);

		return !Objects.isNull(this.guild) && !Objects.isNull(this.channel);
	}
}
