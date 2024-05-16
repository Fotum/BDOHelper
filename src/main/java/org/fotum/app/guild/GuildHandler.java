package org.fotum.app.guild;

import lombok.Getter;
import org.fotum.app.modules.bdo.UpdaterDaemon;
import org.fotum.app.modules.bdo.league.LeagueInstance;
import org.fotum.app.modules.bdo.siege.SiegeInstance;
import org.fotum.app.modules.bdo.siege.SiegeSettings;
import org.fotum.app.modules.tictactoe.TicTacToeGame;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

public class GuildHandler {
    @Getter
    private final long guildId;
    @Getter
    private final SiegeSettings siegeSettings;
    @Getter
    private final List<SiegeInstance> siegeInstances = new LinkedList<>();
    @Getter
    private final List<LeagueInstance> leagueInstances = new LinkedList<>();
    @Getter
    private final List<TicTacToeGame> ticTacToeGames = new LinkedList<>();

    private UpdaterDaemon updaterDaemon;

    public GuildHandler(long guildId) {
        this.guildId = guildId;
        this.siegeSettings = new SiegeSettings();
    }

    public GuildHandler(JSONObject guildObject) {
        this.guildId = guildObject.getLong("id");
        this.siegeSettings = new SiegeSettings(guildObject.getJSONObject("siege_settings"));

        JSONArray siegeInstancesJSON = guildObject.getJSONArray("siege_instances");
        for (int i = 0; i < siegeInstancesJSON.length(); i++) {
            SiegeInstance instance = new SiegeInstance(this, siegeInstancesJSON.getJSONObject(i));
            this.siegeInstances.add(instance);
        }

        JSONArray leagueInstancesJSON = guildObject.getJSONArray("league_instances");
        for (int i = 0; i < leagueInstancesJSON.length(); i++) {
            LeagueInstance instance = new LeagueInstance(this, leagueInstancesJSON.getJSONObject(i));
            this.leagueInstances.add(instance);
        }

        if (this.siegeInstances.size() > 0)
            this.restartDaemon();
    }

    public void addSiegeInstance(SiegeInstance inst) {
        this.siegeInstances.add(inst);
        this.restartDaemon();
    }

    public SiegeInstance getSiegeInstance(LocalDate siegeDt) {
        return this.siegeInstances.stream()
                .filter((i) -> i.getSiegeDt().isEqual(siegeDt))
                .findFirst()
                .orElse(null);
    }

    public void removeSiegeInstance(SiegeInstance inst) {
        this.updaterDaemon.pushRemoveSiegeInstance(inst);
    }

    public void addLeagueInstance(LeagueInstance inst) {
        this.leagueInstances.add(inst);
        this.restartDaemon();
    }

    public LeagueInstance getLeagueInstance(LocalDateTime startDttm) {
        return this.leagueInstances.stream()
                .filter((i) -> i.getStartDttm().isEqual(startDttm))
                .findFirst()
                .orElse(null);
    }

    public void removeLeagueInstance(LeagueInstance inst) {
        this.updaterDaemon.pushRemoveLeagueInstance(inst);
    }

    public void stopDaemon() {
        if (this.updaterDaemon != null && this.updaterDaemon.isRunning())
            this.updaterDaemon.stopUpdater();
    }

    public JSONObject toJSON() {
        JSONObject settings = new JSONObject();

        JSONArray siegeInstancesJSON = new JSONArray();
        for (SiegeInstance instance : this.siegeInstances) {
            siegeInstancesJSON.put(instance.toJSON());
        }

        JSONArray leagueInstancesJSON = new JSONArray();
        for (LeagueInstance instance : this.leagueInstances) {
            leagueInstancesJSON.put(instance.toJSON());
        }

        settings.put("id", this.guildId);
        settings.put("siege_settings", this.siegeSettings.toJSON());
        settings.put("siege_instances", siegeInstancesJSON);
        settings.put("league_instances", leagueInstancesJSON);

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
