package org.fotum.app.config;

import java.io.IOException;
import java.io.InputStream;

import org.fotum.app.MainApp;
import org.json.JSONObject;

public class Config extends JSONObject
{
	private static Config instance;
	
	private Config(InputStream inStream) throws IOException
	{
		super(new ConfigLoader().load(inStream));
	}
	
	public static Config getInstance() throws IOException
	{
		if (instance == null)
		{
			synchronized(Config.class)
			{
				if (instance == null)
				{
					InputStream configRes = MainApp.class.getResourceAsStream("/botconfig.json");
					instance = new Config(configRes);
				}
			}
		}

		return instance;
	}
}
