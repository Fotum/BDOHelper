package org.fotum.app.features.siege;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GuildManagerDaemon extends Thread
{
	private final GuildManager manager;
	
	public GuildManagerDaemon(GuildManager manager)
	{
		log.info("Siege daemon initialized");
		this.manager = manager;
	}
	
	@Override
	public void run()
	{
		boolean isRunning = true;
		
		while (isRunning)
		{
			Map<Long, SiegeInstance> instances = manager.getSiegeInstances();
			for (Entry<Long, SiegeInstance> instEntry : instances.entrySet())
			{
				LocalDateTime dtNow = LocalDateTime.now();
				LocalDate instDt = instEntry.getValue().getStartDt();
				DayOfWeek instDayOfWeek = instEntry.getValue().getStartDt().getDayOfWeek();

				int unschedHour = 21;
				if (instDayOfWeek == DayOfWeek.SATURDAY || instDayOfWeek == DayOfWeek.SUNDAY)
					unschedHour = 20;

				LocalDateTime unschedAt = instDt.atTime(unschedHour, 0);

				
				if (dtNow.isAfter(unschedAt))
				{
					instEntry.getValue().stopInstance();
					instances.remove(instEntry.getKey());

					log.info("Unscheduled siege for Guild with ID: " + instEntry.getKey());
				}
			}
			
			try
			{
				TimeUnit.MINUTES.sleep(5L);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
				isRunning = false;
			}
		}
	}
}
