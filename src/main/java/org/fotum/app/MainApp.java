package org.fotum.app;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.fotum.app.config.Config;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

@Slf4j
public class MainApp {
    @Getter
    @Setter
    private static volatile boolean isApiConnected = false;

    private static ShardManager shardManager = null;

    public static void main(String[] args) {
        MainApp.shardManager = new MainApp().initApp();
    }

    public static ShardManager getAPI()
    {
        return MainApp.shardManager;
    }

    private MainApp() {}

    private ShardManager initApp() {
        log.info("Booting");

        Constants.initConstants();
        Config config = Config.getInstance();

        CommandManager manager = new CommandManager();
        Listener listener = new Listener(manager);

        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT
        );

        DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.create(config.getString("token_dev"), intents);
        builder.setActivity(Activity.watching("you not going to siege"))
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(listener)
                .disableCache(
                        Arrays.stream(CacheFlag.values())
                                .collect(Collectors.toList())
                );

        log.info("Finished booting");
        return builder.build();
    }
}