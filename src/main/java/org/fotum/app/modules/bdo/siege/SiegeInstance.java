package org.fotum.app.modules.bdo.siege;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.modules.bdo.BDOChannel;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class SiegeInstance {
    private final GuildHandler handler;
    private final EmbedBuilder embedBuilder;
    private final Set<GuildMemberInfo> registeredPlayers;
    private final Queue<GuildMemberInfo> latePlayers;
    private final Set<GuildMemberInfo> unregisteredPlayers;

    @Getter @Setter
    private volatile boolean needRedraw = true;

    @Getter
    private boolean needMention = true;
    @Getter @Setter
    private boolean buttonsDisabled = false;
    @Getter @Setter
    private long siegeAnnounceMsgId;
    @Getter @Setter
    private long messageMentionId;

    @Getter
    private int playersMax;
    private BDOChannel zone;
    @Getter
    private LocalDate siegeDt;

    public SiegeInstance(GuildHandler handler, LocalDate siegeDt, BDOChannel zone, int playersMax) {
        this.handler = handler;
        this.siegeDt = siegeDt;
        this.zone = zone;
        this.playersMax = playersMax;

        this.siegeAnnounceMsgId = 0L;
        this.messageMentionId = 0L;

        String dayOfWeek = this.siegeDt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        String dateStr = this.siegeDt.format(Constants.DATE_FORMAT);

        this.embedBuilder = new EmbedBuilder().setColor(DiscordObjectsOperations.getRandomColor());
        this.embedBuilder.setTitle(String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone.getLabel()));
        this.embedBuilder.setDescription("Проверьте пвп морфы, инкрустацию, релики, забаф, бижу под свап и т.д.");

        this.registeredPlayers = new LinkedHashSet<>();
        this.latePlayers = new PriorityQueue<>(Comparator.comparing(GuildMemberInfo::getPriority));
        this.unregisteredPlayers = new LinkedHashSet<>();
    }

    public SiegeInstance(GuildHandler handler, JSONObject instance) {
        this(
            handler,
            LocalDate.parse(instance.getString("start_dt"), Constants.DATE_FORMAT),
            BDOChannel.valueOf(instance.getString("zone")),
            instance.getInt("players_max")
        );

        JSONArray registeredPlayersJson = instance.getJSONArray("registered_players");
        for (int i = 0; i < registeredPlayersJson.length(); i++) {
            GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().get(registeredPlayersJson.getLong(i));
            this.registeredPlayers.add(player);
        }

        JSONArray latePlayersJson = instance.getJSONArray("late_players");
        for (int i = 0; i < latePlayersJson.length(); i++) {
            GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().get(latePlayersJson.getLong(i));
            this.latePlayers.add(player);
        }

        JSONArray unregisteredPlayersJson = instance.getJSONArray("unregistered_players");
        for (int i = 0; i < unregisteredPlayersJson.length(); i++) {
            GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().get(unregisteredPlayersJson.getLong(i));
            this.unregisteredPlayers.add(player);
        }

        // If we are loaded from settings - no need to mention roles
        this.needMention = false;
    }

    public synchronized void registerPlayer(long discordId) {
        GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().computeIfAbsent(discordId, GuildMemberInfo::new);
        if (this.registeredPlayers.contains(player) || this.latePlayers.contains(player))
            return;

        boolean changeDone;
        this.unregisteredPlayers.remove(player);
        if (this.registeredPlayers.size() < this.playersMax) {
            changeDone = this.registeredPlayers.add(player);
        } else {
            changeDone = this.latePlayers.add(player);
        }

        if (!this.needRedraw && changeDone)
            this.needRedraw = true;
    }

    public synchronized void unregisterPlayer(long discordId) {
        GuildMemberInfo player = this.handler.getSiegeSettings().getRegisteredMembers().computeIfAbsent(discordId, GuildMemberInfo::new);
        if (this.unregisteredPlayers.contains(player))
            return;

        // If player removes himself from registered list -> free slot and notify
        if (this.registeredPlayers.contains(player)) {
            // Remove player from reg list
            this.registeredPlayers.remove(player);
            // Find first late player if any
            GuildMemberInfo late = this.latePlayers.stream()
                    .findFirst()
                    .orElse(null);

            if (late != null) {
                this.latePlayers.remove(late);
                this.registeredPlayers.add(late);

                User userToNotify = DiscordObjectsOperations.getUserById(late.getDiscordId());
                DiscordObjectsOperations.sendDirectMessage(userToNotify, String.format("Для Вас появился слот на осаду и вы были перенесены в список участников.\r\n" +
                                "Ждем Вас на осаде **%s (%s)**.",
                        this.siegeDt.format(Constants.DATE_FORMAT),
                        this.siegeDt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru")))
                );
            }
        } else {
            this.latePlayers.remove(player);
        }

        boolean changeDone = this.unregisteredPlayers.add(player);
        if (!this.needRedraw && changeDone)
            this.needRedraw = true;
    }

    public synchronized void setPlayersMax(int maxPlayers) {
        if (this.playersMax == maxPlayers)
            return;

        this.playersMax = maxPlayers;
        this.rearrangePlayers();

        if (!this.needRedraw)
            this.needRedraw = true;
    }

    public synchronized void setSiegeDt(LocalDate newDate) {
        this.siegeDt = newDate;
        String dayOfWeek = this.siegeDt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        String dateStr = this.siegeDt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        this.embedBuilder.setTitle(String.format("Осада %s (%s) на канале - %s 1", dateStr, dayOfWeek, this.zone.getLabel()));

        if (!this.needRedraw)
            this.needRedraw = true;
    }

    public synchronized void setZone(BDOChannel newZone) {
        this.zone = newZone;
        this.setSiegeDt(this.siegeDt);
    }

    public synchronized String generateMentionMessage() {
        this.needMention = false;

        StringBuilder rolesToMention = new StringBuilder();
        List<Long> mentionRoles = this.handler.getSiegeSettings().getMentionRoles();
        if (!mentionRoles.isEmpty()) {
            Iterator<Long> mentionIter = mentionRoles.iterator();
            while (mentionIter.hasNext()) {
                Role toAdd = DiscordObjectsOperations.getGuildRoleById(this.handler.getGuildId(), mentionIter.next());
                if (toAdd != null) {
                    rolesToMention.append(toAdd.getAsMention());
                } else {
                    mentionIter.remove();
                }
            }
        }

        return rolesToMention.toString();
    }

    public synchronized EmbedBuilder generateSiegeEmbed() {
        this.needRedraw = false;

        int slotsRemain = this.playersMax - this.registeredPlayers.size();
        // Embed parameters
        this.embedBuilder.clearFields();
        this.embedBuilder.addField("Плюсов на осаду", String.valueOf(this.registeredPlayers.size()), true);
        this.embedBuilder.addField("Осталось слотов", String.valueOf(slotsRemain), true);
        this.embedBuilder.addBlankField(true);

        // Registered players embed field
        String regPlayersFieldText = this.getFieldTextString(this.registeredPlayers);
        if (!regPlayersFieldText.isEmpty())
            this.addFieldToEmbed(this.embedBuilder, "Придут", regPlayersFieldText, true);

        // Unregistered players embed field
        String unregPlayersFieldText = this.getFieldTextString(this.unregisteredPlayers);
        if (!unregPlayersFieldText.isEmpty())
            this.addFieldToEmbed(this.embedBuilder, "Не придут", unregPlayersFieldText, true);

        // Late players embed field
        String latePlayersFieldText = this.getFieldTextString(this.latePlayers);
        if (!latePlayersFieldText.isEmpty())
            this.addFieldToEmbed(this.embedBuilder, "Нет слота", latePlayersFieldText, false);

        return this.embedBuilder;
    }

    public JSONObject toJSON() {
        JSONObject instance = new JSONObject();

        instance.put("start_dt", this.siegeDt.format(Constants.DATE_FORMAT));
        instance.put("zone", this.zone.toString());
        instance.put("players_max", this.playersMax);
        instance.put("registered_players", this.registeredPlayers.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));
        instance.put("late_players", this.latePlayers.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));
        instance.put("unregistered_players", this.unregisteredPlayers.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));

        return instance;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (!(other instanceof SiegeInstance))
            return false;

        SiegeInstance otherInst = (SiegeInstance) other;
        return otherInst.getSiegeDt().isEqual(this.siegeDt);
    }

    private void rearrangePlayers() {
        // Finding difference between max and current
        int diff = this.playersMax - this.registeredPlayers.size();

        // If registred < max players then add players from late list
        if (diff > 0 && this.latePlayers.size() > 0) {
            Iterator<GuildMemberInfo> lateIterator = this.latePlayers.iterator();
            while (lateIterator.hasNext() && diff != 0) {
                GuildMemberInfo player = lateIterator.next();
                this.registeredPlayers.add(player);
                lateIterator.remove();
                diff--;

                User userToNotify = DiscordObjectsOperations.getUserById(player.getDiscordId());
                DiscordObjectsOperations.sendDirectMessage(userToNotify, String.format("Для Вас появился слот на осаду и вы были перенесены в список участников.\r\n" +
                                "Ждем Вас на осаде **%s (%s)**.",
                        this.siegeDt.format(Constants.DATE_FORMAT),
                        this.siegeDt.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru")))
                );
            }
        // If registred > max players then add players from registred list to late ones
        } else if (diff < 0) {
            // Get last abs(diff) players to remove from reg
            List<GuildMemberInfo> tmpLateHead = new ArrayList<>(this.registeredPlayers)
                    .subList(
                            Math.max(0, this.registeredPlayers.size() - Math.abs(diff)),
                            this.registeredPlayers.size()
                    );

            // Remove players from reg list
            tmpLateHead.forEach(this.registeredPlayers::remove);
            // New head is abs(diff) players from registered players, add others to them as tail
            tmpLateHead.addAll(this.latePlayers);

            // Reform late players list
            this.latePlayers.clear();
            this.latePlayers.addAll(tmpLateHead);
        }
    }

    private void addFieldToEmbed(EmbedBuilder builder, String fieldNm, String fieldVal, boolean inline) {
        if (fieldVal.length() > MessageEmbed.VALUE_MAX_LENGTH) {
            int subListStart = -1;
            int strLen = 0;
            List<String> tmpVal = Arrays.asList(fieldVal.split("\n"));
            for (int i = 0; i < tmpVal.size() && subListStart == -1; i++) {
                String elem = tmpVal.get(i);
                int elemLen = elem.length();
                strLen += (elemLen + 1);

                if (strLen > MessageEmbed.VALUE_MAX_LENGTH)
                    subListStart = i - 1;
            }

            builder.addField(fieldNm, String.join("\n", tmpVal.subList(0, subListStart)), inline);
            this.addFieldToEmbed(builder, "", String.join("\n", tmpVal.subList(subListStart, tmpVal.size())), inline);
        } else {
            builder.addField(fieldNm, fieldVal, inline);
        }
    }

    private String getFieldTextString(Collection<GuildMemberInfo> toConvert) {
        List<String> resultList = new LinkedList<>();
        Iterator<GuildMemberInfo> memberInfoIterator = toConvert.iterator();
        while (memberInfoIterator.hasNext()) {
            GuildMemberInfo memberInfo = memberInfoIterator.next();
            Member member = DiscordObjectsOperations.getGuildMemberById(this.handler.getGuildId(), memberInfo.getDiscordId());
            if (member == null) {
                memberInfoIterator.remove();
                continue;
            }

            if (memberInfo.getBdoName() != null) {
                resultList.add(String.format("%s (%s)", memberInfo.getBdoName(), member.getAsMention()));
            } else {
                resultList.add(String.format("%s", member.getAsMention()));
            }
        }

        return String.join("\n", resultList);
    }
}
