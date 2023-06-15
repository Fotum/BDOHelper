package org.fotum.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class Constants
{
	public static final String PREFIX = "~";
	public static final long OWNER = <OWNER_ID>;
	public static final long SUB_OWNER = <SUB_OWNER_ID>;
	public static final String SETTINGS_LOC = System.getProperty("user.dir") + File.separator + "settings";
	public static final String GUILD_SETTINGS_LOC = Constants.SETTINGS_LOC + File.separator + "guild_settings.json";
	
	static void initConstants()
	{
		// Create settings dir if not exists
		File settingsDir = new File(Constants.SETTINGS_LOC);
		if (!settingsDir.exists())
		{
			settingsDir.mkdirs();
		}

		// Create guild_settings.json file if not exists
		File guildSettingsFile = new File(Constants.GUILD_SETTINGS_LOC);
		if (!guildSettingsFile.exists() || !guildSettingsFile.isFile())
		{
			String jsonBody = "[]";
			try (OutputStreamWriter writer =
						 new OutputStreamWriter(new FileOutputStream(guildSettingsFile), StandardCharsets.UTF_8))
			{
				writer.write(jsonBody);
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
