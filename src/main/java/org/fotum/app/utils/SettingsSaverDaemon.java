package org.fotum.app.utils;

import java.util.concurrent.TimeUnit;

public class SettingsSaverDaemon extends Thread
{
    @Override
    public void run()
    {
        boolean isRunning = true;

        while (isRunning)
        {
            try
            {
                TimeUnit.HOURS.sleep(1L);
            }
            catch (InterruptedException ex)
            {
                isRunning = false;
            }

            if (isRunning)
                BotUtils.saveSettingsToJSON();
        }
    }
}
