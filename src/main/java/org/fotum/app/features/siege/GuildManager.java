package org.fotum.app.features.siege;

import lombok.Getter;
import org.fotum.app.features.audio.handlers.ChannelVoiceRecorder;
import org.fotum.app.features.vkfeed.VkCaller;

import java.util.HashMap;
import java.util.Map;

public class GuildManager
{
	private static GuildManager INSTANCE = null;
	private final GuildManagerDaemon schedDaemon;

	@Getter
	private final Map<Long, SiegeInstance> siegeInstances = new HashMap<>();
	@Getter
	private final Map<Long, GuildSettings> guildSettings = new HashMap<>();
	@Getter
	private final Map<Long, VkCaller> vkCallers = new HashMap<>();
	@Getter
	private final Map<Long, ChannelVoiceRecorder> voiceRecorders = new HashMap<>();

	private GuildManager()
	{
		schedDaemon = new GuildManagerDaemon(this);
		schedDaemon.setDaemon(true);
		schedDaemon.start();
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
		{
			this.siegeInstances.put(guildId, siegeInst);
			siegeInst.start();
		}
	}

	public SiegeInstance getSiegeInstance(Long guildId)
	{
		return this.siegeInstances.get(guildId);
	}

	public void removeSiegeInstance(Long guildId)
	{
		if (this.siegeInstances.containsKey(guildId))
		{
			this.siegeInstances.get(guildId).stopInstance();
			this.siegeInstances.remove(guildId);
		}
	}

	public void addVkCaller(Long guildId, VkCaller caller)
	{
		if (!this.vkCallers.containsKey(guildId))
		{
			this.vkCallers.put(guildId, caller);
			caller.start();
		}
	}

	public VkCaller getVkCaller(Long guildId)
	{
		return this.vkCallers.get(guildId);
	}

	public void removeVkCaller(Long guildId)
	{
		if (this.vkCallers.containsKey(guildId))
		{
			this.vkCallers.get(guildId).stopVkCaller();
			this.vkCallers.remove(guildId);
		}
	}

	public void addVoiceRecorder(Long guildId, ChannelVoiceRecorder recorder)
	{
		if (!this.voiceRecorders.containsKey(guildId))
			this.voiceRecorders.put(guildId, recorder);
	}

	public void removeVoiceRecorder(Long guildId)
	{
		if (this.voiceRecorders.containsKey(guildId))
		{
			this.voiceRecorders.get(guildId).finish();
			this.voiceRecorders.remove(guildId);
		}
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
