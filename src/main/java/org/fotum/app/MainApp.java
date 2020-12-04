package org.fotum.app;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Random;

import org.fotum.app.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManager;

// #TODO: Начать документировать и комментировать код
/**
 * 
 * 
 * @author Fotum
 *
 */
public class MainApp
{
	private static final Random RANDOM = new Random();

	private MainApp() throws IOException, URISyntaxException
	{
		InputStream configRes = this.getClass().getResourceAsStream("/botconfig.json");
		Config config = new Config(configRes);

		CommandManager commandManager = new CommandManager();
		Listener listener = new Listener(commandManager);
		
		Logger logger = LoggerFactory.getLogger(MainApp.class);
		logger.info("Booting");
		Constants.initConstants();

		DefaultShardManager shardManager = new DefaultShardManager(config.getString("token"));
		shardManager.setActivity(Activity.playing("with other bots"));
		shardManager.setStatus(OnlineStatus.ONLINE);
		shardManager.addEventListener(listener);
		shardManager.start(0);

		logger.info("Running");
	}
	
	public static void main(String... args) throws IOException, URISyntaxException
	{
		new MainApp();
	}
	
	public static Color getRandomColor()
	{
		float r = RANDOM.nextFloat();
		float g = RANDOM.nextFloat();
		float b = RANDOM.nextFloat();
		
		return new Color(r, g, b);
	}
}
