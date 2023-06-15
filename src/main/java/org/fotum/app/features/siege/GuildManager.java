package org.fotum.app.features.siege;

import lombok.Getter;
import org.fotum.app.features.tictactoe.TicTacToeGame;

import java.time.LocalDate;
import java.util.*;

public class GuildManager
{
	private static GuildManager INSTANCE = null;
	private final TimeCheckerDaemon checkerDaemon;

	@Getter
	private final Map<Long, List<SiegeInstance>> siegeInstances = new HashMap<>();
	@Getter
	private final Map<Long, GuildSettings> guildSettings = new HashMap<>();
	@Getter
	private final List<TicTacToeGame> ticTacToeGames = new ArrayList<>();

	private GuildManager()
	{
		this.checkerDaemon = new TimeCheckerDaemon(this);
		this.checkerDaemon.setDaemon(true);
		this.checkerDaemon.start();
	}

	public void addGuildSettings(Long guildId, GuildSettings settings)
	{
		if (!this.guildSettings.containsKey(guildId))
		{
			this.guildSettings.put(guildId, settings);
		}
	}

	public GuildSettings getGuildSettings(Long guildId)
	{
		return this.guildSettings.get(guildId);
	}

	public void addSiegeInstance(Long guildId, SiegeInstance siegeInst)
	{
		if (!this.siegeInstances.containsKey(guildId))
			this.siegeInstances.put(guildId, new ArrayList<>());

		this.removeSiegeInstance(guildId, siegeInst.getStartDt());
		this.siegeInstances.get(guildId).add(siegeInst);
		siegeInst.start();
	}

	public List<SiegeInstance> getGuildSiegeInstances(Long guildId)
	{
		if (!this.siegeInstances.containsKey(guildId))
			return new ArrayList<>();

		return this.siegeInstances.get(guildId);
	}

	public SiegeInstance getGuildSiegeInstance(Long guildId, LocalDate instDt)
	{
		if (!this.siegeInstances.containsKey(guildId))
			return null;

		List<SiegeInstance> instances = this.siegeInstances.get(guildId);
		return instances.stream()
				.filter((inst) -> inst.getStartDt().isEqual(instDt))
				.findFirst()
				.orElse(null);
	}

	public void removeSiegeInstance(Long guildId, LocalDate instDt)
	{
		if (!this.siegeInstances.containsKey(guildId))
			return;

		List<SiegeInstance> instances = this.siegeInstances.get(guildId);
		SiegeInstance oldOne = instances.stream()
				.filter((inst) -> inst.getStartDt().isEqual(instDt))
				.findFirst()
				.orElse(null);

		if (oldOne != null)
		{
			oldOne.stopInstance();
			instances.remove(oldOne);
		}
	}

	public void removeAllGuildSiegeInstances(Long guildId)
	{
		if (!this.siegeInstances.containsKey(guildId))
			return;

		List<SiegeInstance> instances = this.siegeInstances.get(guildId);
		Iterator<SiegeInstance> instancesIter = instances.iterator();
		while (instancesIter.hasNext())
		{
			SiegeInstance inst = instancesIter.next();
			inst.stopInstance();
			instancesIter.remove();
		}
	}

	public void addTicTacToeGame(TicTacToeGame gameInstance)
	{
		this.ticTacToeGames.add(gameInstance);
	}

	public static GuildManager getInstance()
	{
		if (INSTANCE == null)
		{
			synchronized(GuildManager.class)
			{
				if (INSTANCE == null)
					INSTANCE = new GuildManager();
			}
		}

		return INSTANCE;
	}
}
