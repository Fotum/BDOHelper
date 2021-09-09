package org.fotum.app.features.siege;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class GuildSettings
{
    @Getter @Setter
    private long listeningChannel;
    @Getter @Setter
    private long vkFeedChannel;
    @Getter @Setter
    private List<Long> mentionRoles;
    @Getter @Setter
    private List<Long> managingRoles;
    @Getter @Setter
    private Set<Long> prefixRoles;
    @Getter @Setter
    private List<Long> autoregList;

    public GuildSettings()
    {
        this.listeningChannel = 0L;
        this.vkFeedChannel = 0L;

        this.mentionRoles = new ArrayList<Long>();
        this.managingRoles = new ArrayList<Long>();
        this.prefixRoles = new LinkedHashSet<Long>();
        this.autoregList = new ArrayList<Long>();
    }

    public GuildSettings(JSONObject settingsJSON)
    {
        this.listeningChannel = settingsJSON.getLong("listening_channel");
        this.vkFeedChannel = settingsJSON.getLong("vk_feed_channel");

        this.mentionRoles = (List<Long>) this.jsonArrayToCollection(settingsJSON.getJSONArray("mention_roles"), new ArrayList<Long>());
        this.managingRoles = (List<Long>) this.jsonArrayToCollection(settingsJSON.getJSONArray("managing_roles"), new ArrayList<Long>());
        this.prefixRoles = (Set<Long>) this.jsonArrayToCollection(settingsJSON.getJSONArray("prefix_roles"), new LinkedHashSet<Long>());
        this.autoregList = (List<Long>) this.jsonArrayToCollection(settingsJSON.getJSONArray("autoreg_list"), new ArrayList<Long>());
    }

    public JSONObject toJSON()
    {
        JSONObject settings = new JSONObject();

        settings.put("listening_channel", this.listeningChannel);
        settings.put("vk_feed_channel", this.vkFeedChannel);
        settings.put("managing_roles", this.managingRoles);
        settings.put("mention_roles", this.mentionRoles);
        settings.put("prefix_roles", this.prefixRoles);
        settings.put("autoreg_list", this.autoregList);

        return settings;
    }

    private Collection<Long> jsonArrayToCollection(JSONArray jsonArr, Collection<Long> coll)
    {
        for (int i = 0; i < jsonArr.length(); i++)
        {
            coll.add(jsonArr.getLong(i));
        }

        return coll;
    }
}
