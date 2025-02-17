package org.fotum.app.modules.bdo;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Comparator;

public class GuildMemberInfo implements Comparable<GuildMemberInfo> {
    @Getter
    private final long discordId;

    @Getter @Setter
    private int priority;
    @Getter @Setter
    private String bdoName;

    public GuildMemberInfo(long discordId) {
        this.discordId = discordId;

        this.priority = 999;
        this.bdoName = null;
    }

    public GuildMemberInfo(JSONObject instance) {
        this.discordId = instance.getLong("discord_id");
        this.bdoName = instance.optString("ingame_name", null);
        this.priority = instance.optInt("priority", 999);
    }

    public JSONObject toJSON() {
        JSONObject thisJson = new JSONObject();
        thisJson.put("discord_id", this.discordId);
        thisJson.put("ingame_name", this.bdoName);
        if (this.priority != 999)
            thisJson.put("priority", this.priority);

        return thisJson;
    }

    @Override
    public int compareTo(@NotNull GuildMemberInfo other) {
        return Comparator.comparing(GuildMemberInfo::getPriority).compare(this, other);
    }
}
