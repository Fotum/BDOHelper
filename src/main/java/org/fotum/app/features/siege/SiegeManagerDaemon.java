package org.fotum.app.features.siege;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiegeManagerDaemon extends Thread
{
	private boolean isRunning;
	private SiegeManager manager;
	
	Logger logger = LoggerFactory.getLogger(SiegeManagerDaemon.class);
	
	public SiegeManagerDaemon(SiegeManager manager)
	{
		logger.info("Siege daemon initialized");
		this.manager = manager;
	}
	
	public void run()
	{
		this.isRunning = true;
		
		while (isRunning)
		{
			Map<Long, SiegeInstance> instances = manager.getSiegeInstances();
			for (SiegeInstance inst : instances.values())
			{
				LocalDateTime dtNow = LocalDateTime.now();
				LocalDate instDt = inst.getStartDt();
				LocalDateTime unschedAt = instDt.atTime(20, 00);
				
				if (dtNow.isAfter(unschedAt))
				{
					inst.stopInstance();
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
