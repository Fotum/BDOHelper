package org.fotum.app.modules.bdo.siege;

import lombok.Getter;
import lombok.Setter;
import org.fotum.app.modules.bdo.GuildMemberInfo;
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
    private String teamspeakLink;

    public SiegeSettings() {
        this.teamspeakLink = null;

        this.mentionRoles = new ArrayList<>();
        this.autoregList = new ArrayList<>();
        this.registeredMembers = new HashMap<>();
    }

    public SiegeSettings(JSONObject settingsJSON) {
        this();

        if (settingsJSON != null) {
            this.teamspeakLink = settingsJSON.optString("teamspeak_link", null);

            JSONArray mentionRolesJSON = settingsJSON.optJSONArray("mention_roles", new JSONArray());
            for (int i = 0; i < mentionRolesJSON.length(); i++) {
                this.mentionRoles.add(mentionRolesJSON.getLong(i));
            }

            JSONArray regMembersJSON = settingsJSON.optJSONArray("registered_members", new JSONArray());
            for (int i = 0; i < regMembersJSON.length(); i++) {
                GuildMemberInfo memberObj = new GuildMemberInfo(regMembersJSON.getJSONObject(i));
                this.registeredMembers.put(memberObj.getDiscordId(), memberObj);
            }

            JSONArray autoregJSON = settingsJSON.optJSONArray("autoreg_list", new JSONArray());
            for (int i = 0; i < autoregJSON.length(); i++) {
                GuildMemberInfo info = this.registeredMembers.computeIfAbsent(autoregJSON.getLong(i), GuildMemberInfo::new);
                this.autoregList.add(info);
            }
        }
    }

    public JSONObject toJSON() {
        JSONObject settings = new JSONObject();

        settings.putOpt("teamspeak_link", this.teamspeakLink);
        settings.putOpt("mention_roles", this.mentionRoles.isEmpty() ? null : this.mentionRoles);
        settings.putOpt("autoreg_list", this.autoregList.isEmpty() ? null : this.autoregList.stream().map(GuildMemberInfo::getDiscordId).collect(Collectors.toList()));
        settings.putOpt("registered_members", this.registeredMembers.isEmpty() ? null : this.registeredMembers.values().stream().map(GuildMemberInfo::toJSON).collect(Collectors.toList()));

        return settings.isEmpty() ? null : settings;
    }
}
