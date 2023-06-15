package org.fotum.app.interfaces;

import java.util.Set;

public interface ISiegeQueueHandler
{
    boolean registerPlayer(Long playerId);

    boolean unregisterPlayer(Long playerId);

    int getPlayersMax();

    void setPlayersMax(int newValue);

    Set<Long> getRegisteredPlayers();

    Set<Long> getLatePlayers();

    Set<Long> getUnregisteredPlayers();
}
