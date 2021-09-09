package org.fotum.app.features.siege;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class GuildManager
{
	private static GuildManager INSTANCE = null;
	private GuildManagerDaemon schedDaemon;

	@Getter
	private Map<Long, SiegeInstance> siegeInstances = new HashMap<Long, SiegeInstance>();
	@Getter @Setter
	private Map<Long, GuildSettings> guildSettings = new HashMap<Long, GuildSettings>();

	private GuildManager()
	{
		schedDaemon = new GuildManagerDaemon(this);
		schedDaemon.setDaemon(true);
		schedDaemon.start();
	}

	public void addSiegeInstance(Long guildId, SiegeInstance siegeInst)
	{
		if (!this.siegeInstances.containsKey(guildId))
		{
			this.siegeInstances.put(guildId, siegeInst);
			siegeInst.start();
		}
	}

	public SiegeInstance getSiegeInstance(Long guildId)
	{
		SiegeInstance result = null;
		if (this.siegeInstances.containsKey(guildId))
			result = this.siegeInstances.get(guildId);
		
		return result;
	}

	public void removeSiegeInstance(Long guildId)
	{
		if (this.siegeInstances.containsKey(guildId))
		{
			this.siegeInstances.get(guildId).stopInstance();
			this.siegeInstances.remove(guildId);
		}
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
		GuildSettings result = null;
		if (this.guildSettings.containsKey(guildId))
			result = this.guildSettings.get(guildId);

		return result;
	}
	
	public void startDaemon()
	{
		if (this.schedDaemon == null)
			this.schedDaemon = new GuildManagerDaemon(this);
	
		if (!this.schedDaemon.isAlive())
			this.schedDaemon.start();
	}
	
	public void stopDaemon()
	{
		if (this.schedDaemon != null && this.schedDaemon.isAlive())
			this.schedDaemon.stopDaemon();
		
		this.schedDaemon = null;
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
