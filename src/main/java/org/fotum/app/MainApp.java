package org.fotum.app;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.fotum.app.config.Config;

import java.util.EnumSet;

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
    private static ShardManager shardManager = null;

	public static void main(String... args)
    {
		shardManager = new MainApp().initApp();
	}

	public static ShardManager getAPI()
    {
        return shardManager;
    }

	private MainApp() {}

    private ShardManager initApp()
    {
        log.info("Booting");

        Config config = Config.getInstance();

        CommandManager commandManager = new CommandManager();
        Listener listener = new Listener(commandManager);
        Constants.initConstants();

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.MESSAGE_CONTENT
        );

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(config.getString("token_prod"), intents);
        builder//.setActivity(Activity.playing("Black Desert Online"))
                .setActivity(Activity.watching("you not going to siege"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(listener)
                .disableCache(
                    CacheFlag.EMOJI,
                    CacheFlag.ACTIVITY,
                    CacheFlag.STICKER,
                    CacheFlag.SCHEDULED_EVENTS
                );

        log.info("Finished booting");
        return builder.build();
    }
}
