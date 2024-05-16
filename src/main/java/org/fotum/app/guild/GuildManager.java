package org.fotum.app.guild;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class GuildManager {

    @Getter
    private final Map<Long, GuildHandler> guilds;

    private GuildManager() {
        this.guilds = new HashMap<>();
    }

    public void addGuildHandler(GuildHandler handler) {
        if (handler == null || this.guilds.containsKey(handler.getGuildId()))
            return;

        this.guilds.put(handler.getGuildId(), handler);
    }

    public GuildHandler getGuildHandler(long guildId) {
        return this.guilds.get(guildId);
    }

    public void removeGuildHandler(long guildId) {
        this.guilds.remove(guildId);
    }

    public static GuildManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private static final class InstanceHolder {
        private static final GuildManager INSTANCE = new GuildManager();
    }
}
