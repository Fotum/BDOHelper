package org.fotum.app.config;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONObject;

public class Config extends JSONObject
{
	private static Config instance;
	
	public Config(InputStream inStream) throws IOException
	{
		super(new ConfigLoader().load(inStream));
		instance = this;
	}
	
	public static Config getInstance()
	{
		return instance;
	}
}
