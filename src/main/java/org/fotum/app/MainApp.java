package org.fotum.app;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManager;
import org.fotum.app.config.Config;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

// #TODO: Начать документировать и комментировать код
/**
 * 
 * 
 * @author Fotum
 *
 */
@Slf4j
public class MainApp
{
	private static final Random RANDOM = new Random();

	private static void initApp() throws IOException
	{
		Config config = Config.getInstance();

		CommandManager commandManager = new CommandManager();
		Listener listener = new Listener(commandManager);

		log.info("Booting");
		Constants.initConstants();

		DefaultShardManager shardManager = new DefaultShardManager(config.getString("token"));
		shardManager.setActivity(Activity.playing("Black Desert Online"));
		shardManager.setStatus(OnlineStatus.ONLINE);
		shardManager.addEventListener(listener);
		shardManager.start(0);

		log.info("Running");
	}

	public static void main(String... args) throws IOException
	{
		MainApp.initApp();
	}

	public static Color getRandomColor()
	{
		float r = RANDOM.nextFloat();
		float g = RANDOM.nextFloat();
		float b = RANDOM.nextFloat();

		return new Color(r, g, b);
	}
}
