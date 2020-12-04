package org.fotum.app;

import java.io.File;

public class Constants
{
	public static final String PREFIX = "~";
	public static final long OWNER = 217576948195524608L;
	public static final String SETTINGS_LOC = System.getProperty("user.dir") + File.separator + "settings";
	
	static void initConstants()
	{
		File settingsDir = new File(Constants.SETTINGS_LOC);
		if (!settingsDir.exists())
		{
			settingsDir.mkdirs();
		}
	}
}
