package org.fotum.app.features.siege;

import java.util.HashMap;
import java.util.Map;

public class SiegeManager
{
	private static SiegeManager INSTANCE = null;
	private SiegeManagerDaemon schedDaemon;
	
	private Map<Long, Long> listeningChannels = new HashMap<Long, Long>();
	private Map<Long, Long> managingRoles = new HashMap<Long, Long>();
	private Map<Long, SiegeInstance> siegeInstances = new HashMap<Long, SiegeInstance>();
	
	private SiegeManager()
	{
		schedDaemon = new SiegeManagerDaemon(this);
		schedDaemon.setDaemon(true);
		schedDaemon.start();
	}

	public SiegeInstance addSiegeInstance(Long guildId)
	{
		if (this.siegeInstances.containsKey(guildId))
			return this.siegeInstances.get(guildId);
		
		SiegeInstance siegeInstance = new SiegeInstance();
		this.siegeInstances.put(guildId, siegeInstance);
		
		return siegeInstance;
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
		if (!this.siegeInstances.containsKey(guildId))
			return;

		this.siegeInstances.get(guildId).unschedule();
		this.siegeInstances.remove(guildId);
	}
	
	public Long getListeningChannel(Long guildId)
	{
		Long result = 0L;
		if (this.listeningChannels.containsKey(guildId))
			result = this.listeningChannels.get(guildId);
		
		return result;
	}
	
	public void addListeningChannel(Long guildId, Long channelId)
	{
		this.listeningChannels.put(guildId, channelId);
	}
	
	public void removeListeningChannel(Long guildId)
	{
		if (this.listeningChannels.containsKey(guildId))
			this.listeningChannels.remove(guildId);
	}
	
	public Long getManagingRole(Long guildId)
	{
		Long result = 0L;
		if (this.managingRoles.containsKey(guildId))
			result = this.managingRoles.get(guildId);
		
		return result;
	}
	
	public void addManagingRole(Long guildId, Long roleId)
	{
		this.managingRoles.put(guildId, roleId);
	}
	
	public void removeManagingRole(Long guildId)
	{
		if (this.managingRoles.containsKey(guildId))
			this.managingRoles.remove(guildId);
	}
	
	public void startDaemon()
	{
		if (this.schedDaemon == null)
			this.schedDaemon = new SiegeManagerDaemon(this);
	
		if (!this.schedDaemon.isAlive())
			this.schedDaemon.start();
	}
	
	public void stopDaemon()
	{
		if (this.schedDaemon != null && this.schedDaemon.isAlive())
			this.schedDaemon.stopDaemon();
		
		this.schedDaemon = null;
	}
	
	public Map<Long, SiegeInstance> getSiegeInstances()
	{
		return this.siegeInstances;
	}
	
	public void setSiegeInstances(Map<Long, SiegeInstance> instances)
	{
		this.siegeInstances = instances;
	}
	
	public Map<Long, Long> getListeningChannels()
	{
		return this.listeningChannels;
	}
	
	public void setListeningChannels(Map<Long, Long> listChans)
	{
		this.listeningChannels = listChans;
	}
	
	public Map<Long, Long> getManagingRoles()
	{
		return this.managingRoles;
	}
	
	public void setManagingRoles(Map<Long, Long> managRoles)
	{
		this.managingRoles = managRoles;
	}

	public static SiegeManager getInstance()
	{
		if (INSTANCE == null)
		{
			synchronized(SiegeManager.class)
			{
				if (INSTANCE == null)
					INSTANCE = new SiegeManager();
			}
		}
		
		return INSTANCE;
	}
}
