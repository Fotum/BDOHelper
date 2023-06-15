package org.fotum.app.structs;

import lombok.Getter;
import org.json.JSONObject;

public class DiscordMemberInfo
{
    @Getter
    private final long discordId;
    @Getter
    private final String bdoName;
    @Getter
    private final String allegiance;

    public DiscordMemberInfo(long discordId, String bdoName, String allegiance)
    {
        this.discordId = discordId;
        this.bdoName = bdoName;
        this.allegiance = allegiance;
    }

    public DiscordMemberInfo(JSONObject instance)
    {
        this.discordId = instance.getLong("discord_id");
        this.bdoName = instance.getString("ingame_name");
        this.allegiance = instance.getString("allegiance");
    }

    public JSONObject toJSON()
    {
        JSONObject thisJson = new JSONObject();
        thisJson.put("discord_id", this.discordId);
        thisJson.put("ingame_name", this.bdoName);
        thisJson.put("allegiance", this.allegiance);

        return thisJson;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null)
            return false;

        if (!(other instanceof DiscordMemberInfo))
            return false;

        DiscordMemberInfo otherInfo = (DiscordMemberInfo) other;
        return this.discordId == otherInfo.getDiscordId();
    }
}
