package org.fotum.app.features.siege;

import lombok.extern.slf4j.Slf4j;
import org.fotum.app.features.tictactoe.TicTacToeGame;

import java.time.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimeCheckerDaemon extends Thread
{
	private final GuildManager manager;

	public TimeCheckerDaemon(GuildManager manager)
	{
		log.info("Time checker daemon initialized");
		this.manager = manager;
	}
	
	@Override
	public void run()
	{
		boolean isRunning = true;
		
		while (isRunning)
		{
			// Getting date and time of now to determine when should we unschedule instances
			LocalDateTime dttmNow = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
			LocalDate dateNow = dttmNow.toLocalDate();
			LocalTime timeNow = dttmNow.toLocalTime();
			// Getting current day of week
			DayOfWeek dayOfWeekNow = dttmNow.getDayOfWeek();

			// Calculate instance state change times
			int disableHour = (dayOfWeekNow != DayOfWeek.SATURDAY && dayOfWeekNow != DayOfWeek.SUNDAY) ? 20 : 19;
			LocalTime disableAtTime = LocalTime.of(disableHour, 0);
			LocalTime unschedAtTime = LocalTime.of(disableHour + 1, 0);

			Map<Long, List<SiegeInstance>> instances = this.manager.getSiegeInstances();
			for (Entry<Long, List<SiegeInstance>> guidIdSieges : instances.entrySet())
			{
				List<SiegeInstance> guildSieges = guidIdSieges.getValue();
				Iterator<SiegeInstance> siegesIter = guildSieges.iterator();
				while (siegesIter.hasNext())
				{
					SiegeInstance instance = siegesIter.next();
					LocalDate instDt = instance.getStartDt();

					// Disable plus and minus buttons
					if (dateNow.isEqual(instDt) && timeNow.isAfter(disableAtTime) && !instance.isButtonsDisabled())
						instance.setButtonsDisabled(true);

					// Unschedule siege instance
					if ((dateNow.isEqual(instDt) && timeNow.isAfter(unschedAtTime)) || dateNow.isAfter(instDt))
					{
						instance.stopInstance();
						siegesIter.remove();

						log.info("Unscheduled siege for Guild with ID: " + guidIdSieges.getKey());
					}
				}
			}

			List<TicTacToeGame> games = this.manager.getTicTacToeGames();
			games.removeIf((game) -> game.getState() == 2);
			
			try
			{
				TimeUnit.MINUTES.sleep(1L);
			}
			catch (InterruptedException ex)
			{
				ex.printStackTrace();
				isRunning = false;
			}
		}
	}
}
