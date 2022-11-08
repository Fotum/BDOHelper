package org.fotum.app.config;

import java.io.IOException;
import java.io.InputStream;

import org.fotum.app.MainApp;
import org.json.JSONObject;

public class Config extends JSONObject
{
	private static Config instance;
	
	private Config(InputStream inStream)
	{
		super(new ConfigLoader().load(inStream));
	}
	
	public static Config getInstance()
	{
		if (instance == null)
		{
			synchronized(Config.class)
			{
				if (instance == null)
				{
					try
					{
						InputStream configRes = MainApp.class.getResourceAsStream("/botconfig.json");
						instance = new Config(configRes);
						configRes.close();
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
					}
				}
			}
		}

		return instance;
	}
}
