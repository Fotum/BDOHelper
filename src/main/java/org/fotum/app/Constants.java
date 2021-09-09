package org.fotum.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Constants
{
	public static final HashMap<String, String> ZONES;
	static
	{
		ZONES = new HashMap<String, String>();
		ZONES.put("tbd", "Неизвестно");
		ZONES.put("bal", "Баленос");
		ZONES.put("val", "Валенсия");
		ZONES.put("ser", "Серендия");
		ZONES.put("kal", "Кальфеон");
		ZONES.put("med", "Медия");
	}

	public static final String PREFIX = "~";
	public static final long OWNER = <OWNER_ID>;
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
