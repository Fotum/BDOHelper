package org.fotum.app.features.siege.queuehandlers;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISiegeQueueHandler;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.utils.DiscordObjectsGetters;

import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

public class DefaultSiegeQueueHandler implements ISiegeQueueHandler
{
    @Getter
    private int playersMax;

    private final SiegeInstance instance;

    @Getter
    private final Set<Long> registeredPlayers;
    @Getter
    private final Set<Long> latePlayers;
    @Getter
    private final Set<Long> unregisteredPlayers;

    public DefaultSiegeQueueHandler(SiegeInstance instance, int playersMax)
    {
        this.instance = instance;
        this.playersMax = playersMax;

        this.registeredPlayers = new LinkedHashSet<>();
        this.latePlayers = new LinkedHashSet<>();
        this.unregisteredPlayers = new LinkedHashSet<>();
    }

	@Override
	public void setPlayersMax(int newValue)
	{
		this.playersMax = newValue;
		this.rearrangePlayers();
	}

	@Override
    public boolean registerPlayer(Long playerId)
    {
		if (this.registeredPlayers.contains(playerId))
			return false;

		this.unregisteredPlayers.remove(playerId);
		if (this.registeredPlayers.size() < this.playersMax)
			return this.registeredPlayers.add(playerId);
		else
			return this.latePlayers.add(playerId);
    }

	@Override
    public boolean unregisterPlayer(Long playerId)
    {
		if (this.unregisteredPlayers.contains(playerId))
			return false;

		// If player removes himself from registered list -> free slot and notify
		if (this.registeredPlayers.contains(playerId)) {
			// Remove player from reg list
			this.registeredPlayers.remove(playerId);
			// Find first late player if any
			long lateId = this.latePlayers.stream()
					.findFirst()
					.orElse(0L);

			// Add first late player to reg list and notify
			if (lateId > 0L)
			{
				this.latePlayers.remove(lateId);
				this.registeredPlayers.add(lateId);

				User userToNotify = DiscordObjectsGetters.getUserById(lateId);
				BotUtils.sendDirectMessage(userToNotify, String.format("Для Вас появился слот на осаду и вы были перенесены в список участников.\r\n" +
								"Ждем Вас на осаде **%s (%s)**.",
						this.instance.getStartDt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        this.instance.getStartDt().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")))
				);
			}
		}
		else
		{
			this.latePlayers.remove(playerId);
		}

		return this.unregisteredPlayers.add(playerId);
    }

    private void rearrangePlayers()
    {
        // Finding difference between max and current
		int diff = this.playersMax - this.registeredPlayers.size();

		// If registred < max players then add players from late list
		if (diff > 0 && this.latePlayers.size() > 0)
		{
			Iterator<Long> lateIterator = this.latePlayers.iterator();
			while (lateIterator.hasNext() && diff != 0)
			{
				long playerId = lateIterator.next();
				this.registeredPlayers.add(playerId);
				lateIterator.remove();
				diff--;

                User userToNotify = DiscordObjectsGetters.getUserById(playerId);
                BotUtils.sendDirectMessage(userToNotify, String.format("Для Вас появился слот на осаду и вы были перенесены в список участников.\r\n" +
                                "Ждем Вас на осаде **%s (%s)**.",
                        this.instance.getStartDt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        this.instance.getStartDt().getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru")))
                );
			}
		}
		// If registred > max players then add players from registred list to late ones
		else if (diff < 0)
		{
			// Get last abs(diff) players to remove from reg
			LinkedHashSet<Long> tmpLateHead = new LinkedHashSet<>(
				new ArrayList<>(this.registeredPlayers)
						.subList(
								Math.max(0, this.registeredPlayers.size() - Math.abs(diff)),
								this.registeredPlayers.size()
						)
			);

			// Remove players from reg list
			tmpLateHead.forEach(this.registeredPlayers::remove);
			// New head is abs(diff) players from registered players, add others to them as tail
			tmpLateHead.addAll(this.latePlayers);

			// Reform late players list
			this.latePlayers.clear();
			this.latePlayers.addAll(tmpLateHead);
		}
    }
}
