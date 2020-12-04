package org.fotum.app.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

class ConfigLoader
{
	String load(InputStream inStream) throws IOException
	{	
		return new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))
					.lines().collect(Collectors.joining("\n"));
	}
}
