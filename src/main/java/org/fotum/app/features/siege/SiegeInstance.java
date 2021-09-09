package org.fotum.app.features.siege;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import org.fotum.app.MainApp;

import lombok.Getter;
import lombok.Synchronized;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.fotum.app.utils.BotUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class SiegeInstance extends Thread
{
	private boolean isRunning = false;
	private final GuildSettings settings;

	@Getter @Setter
	private LocalDate startDt;
	@Setter
	private String zone;
	@Setter
	private int playersMax;

	private String titleMessage;
	@Setter
	private String descriptionMessage;
	private Long announcerDelay;

	private Set<Long> registredPlayers;
	private Set<Long> latePlayers;

	private Guild guild;
	private TextChannel channel;
	private Message siegeAnnounceMsg;
	private Message messageMention;
	private EmbedBuilder eBuilder;

	public SiegeInstance(Guild guild, TextChannel channel, LocalDate startDt, String zone, int playersMax)
	{
		this.guild = guild;
		this.channel = channel;
		this.startDt = startDt;
		this.zone = zone;
		this.playersMax = playersMax;

		String dayOfWeek = this.startDt.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
		String dateStr = this.startDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

		this.announcerDelay = 5L;
		this.titleMessage = String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone);
		this.descriptionMessage = "Взять с собой Топор трины +18, Карки и Гиганты. Оставляйте заявки на осаду: \"+\"";
		this.siegeAnnounceMsg = null;
		this.messageMention = null;

		this.registredPlayers = new LinkedHashSet<Long>();
		this.latePlayers = new LinkedHashSet<Long>();
		this.eBuilder = new EmbedBuilder().setColor(MainApp.getRandomColor());
		this.settings = GuildManager.getInstance().getGuildSettings(this.guild.getIdLong());
	}

	public SiegeInstance(Guild guild, TextChannel channel, JSONObject instance)
	{
		this(
			guild,
			channel,
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
		this.generateAndSendMentionMessage();

		while (isRunning)
		{
			this.generateAndSendSiegeEmbed();

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
		if (!this.isRunning())
			return;

		this.isRunning = false;

		if (this.messageMention != null)
			this.messageMention.delete().complete();

		if (this.siegeAnnounceMsg != null)
			this.siegeAnnounceMsg.delete().complete();
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

				User userToNotify = this.guild.getMemberById(lateId).getUser();
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
			LinkedHashSet<Long> tmpLate = new LinkedHashSet<Long>();
			ArrayList<Long> asArray = new ArrayList<Long>(this.registredPlayers);
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
		if (!this.settings.getMentionRoles().isEmpty())
		{
			StringBuilder rolesToMention = new StringBuilder();
			for (long roleId : this.settings.getMentionRoles())
			{
				Role toAdd = this.guild.getRoleById(roleId);
				if (toAdd != null)
				{
					rolesToMention.append(toAdd.getAsMention());
				}
			}

			if (rolesToMention.length() != 0)
			{
				this.channel.sendMessage(rolesToMention.toString()).queue(
						(message) -> this.messageMention = message
				);
			}
		}
	}

	private void generateAndSendSiegeEmbed()
	{
		int slotsRemain = playersMax - registredPlayers.size();

		this.eBuilder.setTitle(this.titleMessage);
		this.eBuilder.setDescription(this.descriptionMessage);
		this.eBuilder.clearFields();
		this.eBuilder.addField("Плюсов на осаду", String.valueOf(this.registredPlayers.size()), true);
		this.eBuilder.addField("Осталось слотов", String.valueOf(slotsRemain), true);
		this.eBuilder.addBlankField(true);

		for (Long roleId : this.settings.getPrefixRoles())
		{
			Role prefixRole = this.guild.getRoleById(roleId);
			String fieldText = this.convertPlayersSetByRole(this.registredPlayers, prefixRole);
			if (!fieldText.isEmpty())
				this.addFieldToEmbed(this.eBuilder, prefixRole.getName(), fieldText, true);
		}

		String noRoleList = this.convertPlayersWithoutRole(this.registredPlayers);
		if (!noRoleList.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Без роли", noRoleList, true);

		String lateList = this.latePlayers.stream()
				.map(
						(memberId) -> String.format("%s", this.guild.getMemberById(memberId).getAsMention())
				).collect(Collectors.joining("\n"));

		if (!lateList.isEmpty())
			this.addFieldToEmbed(this.eBuilder, "Опоздашки", lateList, true);

		if (this.siegeAnnounceMsg != null)
		{
			this.siegeAnnounceMsg
					.editMessage(eBuilder.build())
					.queue(null, new ErrorHandler()
							.handle(ErrorResponse.UNKNOWN_MESSAGE, (ex) -> this.siegeAnnounceMsg = null)
					);
		}
		else
		{
			this.channel.sendMessage(eBuilder.build()).queue(
					(message) -> this.siegeAnnounceMsg = message
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

	private String convertPlayersSetByRole(Set<Long> playersSet, Role role)
	{
		List<Member> members = playersSet.stream()
								.map(this.guild::getMemberById)
								.collect(Collectors.toList());

		return members.stream()
					.filter(
						(member) -> {
							List<Role> memberRoles = member.getRoles();
							return memberRoles.contains(role);
						}
					).map(
						(member) -> String.format("%s", member.getAsMention())
					).collect(Collectors.joining("\n"));
	}

	private String convertPlayersWithoutRole(Set<Long> playersSet)
	{
		List<Role> prefixes = settings.getPrefixRoles()
								.stream()
								.map(this.guild::getRoleById)
								.collect(Collectors.toList());
		List<Member> members = playersSet.stream()
								.map(this.guild::getMemberById)
								.collect(Collectors.toList());

		return members.stream()
					.filter(
						(member) -> {
							List<Role> memberRoles = member.getRoles();
							return memberRoles.stream().noneMatch(prefixes::contains);
						}
					).map(
						(member) -> String.format("%s", member.getAsMention())
					).collect(Collectors.joining("\n"));
	}
}
