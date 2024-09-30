package org.fotum.app.guild;

import lombok.Getter;
import org.fotum.app.Constants;
import org.fotum.app.modules.bdo.UpdaterDaemon;
import org.fotum.app.modules.bdo.siege.SiegeInstance;
import org.fotum.app.modules.bdo.siege.SiegeSettings;
import org.fotum.app.modules.tictactoe.TicTacToeGame;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

public class GuildHandler {
    @Getter
    private final long guildId;
    @Getter
    private final SiegeSettings siegeSettings;
    @Getter
    private final List<SiegeInstance> instances = new LinkedList<>();
    @Getter
    private final List<TicTacToeGame> ticTacToeGames = new LinkedList<>();

    private UpdaterDaemon updaterDaemon;

    public GuildHandler(long guildId) {
        this.guildId = guildId;
        this.siegeSettings = new SiegeSettings();
    }

    public GuildHandler(JSONObject guildObject) {
        this.guildId = guildObject.getLong("id");
        this.siegeSettings = new SiegeSettings(guildObject.optJSONObject("siege_settings"));

        JSONArray instancesJSON = guildObject.optJSONArray("instances", new JSONArray());
        for (int i = 0; i < instancesJSON.length(); i++) {
            JSONObject instObject = instancesJSON.getJSONObject(i);
            SiegeInstance instance = new SiegeInstance(this, instObject);

            this.instances.add(instance);
        }

        if (this.instances.size() > 0)
            this.restartDaemon();
    }

    public void addInstance(SiegeInstance inst) {
        this.instances.add(inst);
        this.restartDaemon();
    }

    public SiegeInstance getSiegeInstance(long channelId, LocalDate siegeDt) {
        long siegeId = siegeDt.atStartOfDay(Constants.ZONE_ID).toInstant().toEpochMilli();

        return this.instances.stream()
                .filter((i) -> i.getChannelId() == channelId && i.getInstanceId() == siegeId)
                .findFirst()
                .orElse(null);
    }

    public void removeInstance(SiegeInstance instance) {
        this.updaterDaemon.pushRemoveInstance(instance);
    }

    public void checkHandleInstanceDeletion(long msgId) {
        this.instances.stream()
                .filter((i) -> i.getAnnounceMsgId() == msgId)
                .findFirst()
                .ifPresent((i) -> i.setNeedRedraw(true));
    }

    public void stopDaemon() {
        if (this.updaterDaemon != null && this.updaterDaemon.isRunning())
            this.updaterDaemon.stopUpdater();
    }

    public JSONObject toJSON() {
        JSONObject settings = new JSONObject();
        settings.put("id", this.guildId);
        settings.putOpt("siege_settings", this.siegeSettings.toJSON());

        JSONArray instancesJSON = new JSONArray();
        for (SiegeInstance instance : this.instances) {
            instancesJSON.put(instance.toJSON());
        }

        settings.putOpt("instances", instancesJSON.isEmpty() ? null : instancesJSON);

        return settings;
    }

    private void restartDaemon() {
        if (this.updaterDaemon == null || !this.updaterDaemon.isRunning()) {
            this.updaterDaemon = new UpdaterDaemon(this);
            this.updaterDaemon.setDaemon(true);
            this.updaterDaemon.start();
        }
    }
}
