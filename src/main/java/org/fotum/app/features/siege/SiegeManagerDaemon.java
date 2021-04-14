package org.fotum.app.features.siege;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SiegeManagerDaemon extends Thread
{
	private boolean isRunning;
	private SiegeManager manager;
	
	public SiegeManagerDaemon(SiegeManager manager)
	{
		log.info("Siege daemon initialized");
		this.manager = manager;
	}
	
	@Override
	public void run()
	{
		this.isRunning = true;
		
		while (isRunning)
		{
			Map<Long, SiegeInstance> instances = manager.getSiegeInstances();
			for (Entry<Long, SiegeInstance> instEntry : instances.entrySet())
			{
				LocalDateTime dtNow = LocalDateTime.now();
				LocalDate instDt = instEntry.getValue().getStartDt();
				LocalDateTime unschedAt = instDt.atTime(20, 0);
				
				if (dtNow.isAfter(unschedAt))
				{
					instEntry.getValue().stopInstance();
					instances.remove(instEntry.getKey());

					log.info("Unscheduled siege for Guild with ID: " + instEntry.getKey());
				}
			}
			
			try
			{
				Thread.sleep(60000L);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
				this.isRunning = false;
			}
		}
	}
	
	public synchronized void stopDaemon()
	{
		this.isRunning = false;
	}
}
