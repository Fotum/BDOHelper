package org.fotum.app.modules.bdo.league;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class LeagueInstance {
    @Getter
    private final long channelId;
    private final GuildHandler handler;
    private final EmbedBuilder embedBuilder;
    private final Set<GuildMemberInfo> registeredPlayers;
    @Getter
    private final LocalDateTime startDttm;

    @Getter @Setter
    private volatile boolean needRedraw = true;

    @Getter @Setter
    private long announceMsgId = 0L;

    public LeagueInstance(GuildHandler handler, LocalDateTime startDttm, long channelId) {
        this.channelId = channelId;
        this.handler = handler;
        this.startDttm = startDttm;

        this.registeredPlayers = new LinkedHashSet<>();

        this.embedBuilder = new EmbedBuilder().setColor(DiscordObjectsOperations.getRandomColor());
        this.embedBuilder.setTitle(String.format("Регистрация на лигу %s", this.startDttm.format(Constants.DATE_TIME_FORMAT)));
        this.embedBuilder.setDescription("Проверьте пвп морфы, инкрустацию, релики, забаф и т.д.");
    }

    public LeagueInstance(GuildHandler handler, JSONObject instance) {
        this(
            handler,
            LocalDateTime.parse(instance.getString("start_dttm"), Constants.DATE_TIME_FORMAT),
            instance.getLong("channel_id")
        );

        this.announceMsgId = instance.optLong("message_id");

        JSONArray registeredPlayersJson = instance.getJSONArray("registered_players");
        for (int i = 0; i < registeredPlayersJson.length(); i++) {
            GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().get(registeredPlayersJson.getLong(i));
            this.registeredPlayers.add(player);
        }
    }

    public synchronized void registerPlayer(long discordId) {
        GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().computeIfAbsent(discordId, GuildMemberInfo::new);
        if (this.registeredPlayers.contains(player))
            return;

        boolean changeDone = this.registeredPlayers.add(player);
        if (!this.needRedraw && changeDone)
            this.needRedraw = true;
    }

    public synchronized void unregisterPlayer(long discordId) {
        GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().computeIfAbsent(discordId, GuildMemberInfo::new);
        if (!this.registeredPlayers.contains(player))
            return;

        boolean changeDone = this.registeredPlayers.remove(player);
        if (!this.needRedraw && changeDone)
            this.needRedraw = true;
    }

    public synchronized EmbedBuilder generateLeagueEmbed() {
        this.needRedraw = false;

        this.embedBuilder.clearFields();
        this.addEmbedFields(this.embedBuilder);

        return this.embedBuilder;
    }

    public JSONObject toJSON() {
        JSONObject instance = new JSONObject();

        instance.put("channel_id", this.channelId);
        instance.put("message_id", this.announceMsgId);
        instance.put("start_dttm", this.startDttm);
        instance.put("registered_players", this.registeredPlayers.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));

        return instance;
    }

    private void addEmbedFields(EmbedBuilder builder) {
        Map<String, List<String>> bdoClassMap = new HashMap<>();
        for (GuildMemberInfo info : this.registeredPlayers) {
            String bdoClassLabel = (info.getBdoClass() == null) ? null : info.getBdoClass().getLabel();
            Member member = DiscordObjectsOperations.getGuildMemberById(this.handler.getGuildId(), info.getDiscordId());
            if (member != null)
                bdoClassMap.computeIfAbsent(bdoClassLabel, (k) -> new LinkedList<>()).add(member.getAsMention());
        }

        for (Map.Entry<String, List<String>> entry : bdoClassMap.entrySet()) {
            String fieldName = (entry.getKey() != null) ? entry.getKey() : "";
            String fieldValue = String.join(", ", entry.getValue());

            builder.addField(new MessageEmbed.Field(fieldName, fieldValue, false));
        }
    }
}
