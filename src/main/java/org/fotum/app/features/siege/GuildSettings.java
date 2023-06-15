package org.fotum.app.features.siege;

import lombok.Getter;
import lombok.Setter;
import org.fotum.app.structs.DiscordMemberInfo;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GuildSettings
{
    @Getter @Setter
    private long listeningChannel;
    @Getter
    private final List<Long> mentionRoles;
    @Getter
    private final List<Long> autoregList;
    @Getter
    private final List<DiscordMemberInfo> registeredMembers;

    public GuildSettings()
    {
        this.listeningChannel = 0L;

        this.mentionRoles = new ArrayList<>();
        this.autoregList = new ArrayList<>();
        this.registeredMembers = new ArrayList<>();
    }

    public GuildSettings(@NotNull JSONObject settingsJSON)
    {
        this();
        this.listeningChannel = settingsJSON.getLong("listening_channel");

        this.jsonArrayToLongColl(settingsJSON.getJSONArray("mention_roles"), this.mentionRoles);
        this.jsonArrayToLongColl(settingsJSON.getJSONArray("autoreg_list"), this.autoregList);

        this.parseInitRegisteredMembers(settingsJSON.getJSONArray("registered_members"));
    }

    public JSONObject toJSON()
    {
        JSONObject settings = new JSONObject();

        settings.put("listening_channel", this.listeningChannel);
        settings.put("mention_roles", this.mentionRoles);
        settings.put("autoreg_list", this.autoregList);

        JSONArray regArr = new JSONArray();
        for (DiscordMemberInfo info : this.registeredMembers)
            regArr.put(info.toJSON());

        settings.put("registered_members", regArr);

        return settings;
    }

    private void jsonArrayToLongColl(JSONArray jsonArr, Collection<Long> coll)
    {
        for (int i = 0; i < jsonArr.length(); i++)
        {
            coll.add(jsonArr.getLong(i));
        }
    }

    private void parseInitRegisteredMembers(JSONArray registeredMembers)
    {
        for (int i = 0; i < registeredMembers.length(); i++)
        {
            DiscordMemberInfo memberObj = new DiscordMemberInfo(registeredMembers.getJSONObject(i));
            this.registeredMembers.add(memberObj);
        }
    }
}
