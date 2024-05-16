package org.fotum.app.modules.bdo.siege;

import lombok.Getter;
import lombok.Setter;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SiegeSettings {
    @Getter
    private final List<Long> mentionRoles;
    @Getter
    private final List<GuildMemberInfo> autoregList;
    @Getter
    private final Map<Long, GuildMemberInfo> registeredMembers;

    @Getter @Setter
    private long listeningChannel;
    @Getter @Setter
    private String teamspeakLink;

    public SiegeSettings() {
        this.listeningChannel = 0L;
        this.teamspeakLink = null;

        this.mentionRoles = new ArrayList<>();
        this.autoregList = new ArrayList<>();
        this.registeredMembers = new HashMap<>();
    }

    public SiegeSettings(@NotNull JSONObject settingsJSON) {
        this();
        this.listeningChannel = settingsJSON.getLong("listening_channel");
        this.teamspeakLink = settingsJSON.optString("teamspeak_link", null);

        JSONArray mentionRolesJSON = settingsJSON.getJSONArray("mention_roles");
        for (int i = 0; i < mentionRolesJSON.length(); i++) {
            this.mentionRoles.add(mentionRolesJSON.getLong(i));
        }

        JSONArray regMembersJSON = settingsJSON.getJSONArray("registered_members");
        for (int i = 0; i < regMembersJSON.length(); i++) {
            GuildMemberInfo memberObj = new GuildMemberInfo(regMembersJSON.getJSONObject(i));
            this.registeredMembers.put(memberObj.getDiscordId(), memberObj);
        }

        JSONArray autoregJSON = settingsJSON.getJSONArray("autoreg_list");
        for (int i = 0; i < autoregJSON.length(); i++) {
            GuildMemberInfo info = this.registeredMembers.computeIfAbsent(autoregJSON.getLong(i), GuildMemberInfo::new);
            this.autoregList.add(info);
        }
    }

    public JSONObject toJSON() {
        JSONObject settings = new JSONObject();

        settings.put("listening_channel", this.listeningChannel);
        settings.put("teamspeak_link", this.teamspeakLink);
        settings.put("mention_roles", this.mentionRoles);
        settings.put("autoreg_list", this.autoregList.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));

        JSONArray regArr = new JSONArray();
        this.registeredMembers.values().stream().map(GuildMemberInfo::toJSON).forEach(regArr::put);

        settings.put("registered_members", regArr);

        return settings;
    }
}
