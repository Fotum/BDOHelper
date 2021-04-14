package org.fotum.app.features.siege;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

public class SiegeManager
{
	private static SiegeManager INSTANCE = null;
	private SiegeManagerDaemon schedDaemon;
	
	@Getter @Setter
	private Map<Long, Long> listeningChannels = new HashMap<Long, Long>();
	@Getter @Setter
	private Map<Long, Long> managingRoles = new HashMap<Long, Long>();
	@Getter
	private Map<Long, SiegeInstance> siegeInstances = new HashMap<Long, SiegeInstance>();
	@Getter @Setter
	private Map<Long, Set<Long>> prefixRoles = new HashMap<Long, Set<Long>>();

	private SiegeManager()
	{
		schedDaemon = new SiegeManagerDaemon(this);
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
		this.managingRoles.remove(guildId);
	}
	
	public void addPrefixRoles(Long guildId, Set<Long> roleIds)
	{
		this.prefixRoles.remove(guildId);
		this.prefixRoles.put(guildId, roleIds);
	}
	
	public Set<Long> getPrefixRolesById(Long guildId)
	{
		if (this.prefixRoles.containsKey(guildId))
			return this.prefixRoles.get(guildId);
		
		return new LinkedHashSet<Long>();
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
