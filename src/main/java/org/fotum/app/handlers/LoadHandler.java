package org.fotum.app.handlers;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.fotum.app.MainApp;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.modules.bdo.BDOChannel;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

@Slf4j
public class LoadHandler {
    public static void runStartupSequence() {
        log.info("Loading bot settings");
        SettingsHandler.loadSettingsFromJSON();

        log.info("Starting settings saver daemon");
        SettingsHandler.initializeDaemon();

        GuildManager manager = GuildManager.getInstance();
        for (Guild guild : MainApp.getAPI().getGuilds()) {
            LoadHandler.upsertSlashCommands(guild);

            if (!manager.getGuilds().containsKey(guild.getIdLong()))
                LoadHandler.initializeGuildHandler(guild);
        }

        log.info("Startup sequence complete");
    }

    public static void initializeGuildHandler(Guild guild) {
        log.info(String.format("Initializing guild handler for guild with ID %d", guild.getIdLong()));
        GuildHandler handler = new GuildHandler(guild.getIdLong());
        GuildManager.getInstance().addGuildHandler(handler);
    }

    public static void upsertSlashCommands(Guild guild) {
        log.info(String.format("Upserting slash commands for guild with ID %d", guild.getIdLong()));

        guild.upsertCommand("setup", "Sets up or updates siege settings for current guild")
                .addOption(OptionType.STRING, "mention_roles", "Roles to mention on announcement (Optional)", false)
                .addOption(OptionType.STRING, "ts3_link", "Teamspeak 3 server link (additional button under siege announce)", false)
                .queue();
        guild.upsertCommand("addsiege", "Schedules a siege info on a given date")
                .addOptions(
                        new OptionData(OptionType.STRING, "siege_dt", "Siege date in dd.mm.yyyy format", true),
                        LoadHandler.getBdoChannelsOption(true),
                        new OptionData(OptionType.INTEGER, "slots", "Amount of free slots for this siege", true)
                                .setRequiredRange(1, 200))
                .queue();
        guild.upsertCommand("updsiege", "Updates current siege instance")
                .addOptions(
                        new OptionData(OptionType.STRING, "inst_dt", "Date of the siege to be updated in dd.mm.yyyy format", true),
                        new OptionData(OptionType.INTEGER, "max_slots", "New value for maximum slots", false)
                                .setRequiredRange(1, 200),
                        LoadHandler.getBdoChannelsOption(false))
                .queue();
        guild.upsertCommand("forceadd", "Manually adds mentioned users to players list")
                .addOption(OptionType.STRING, "siege_dt", "Date of the siege to add players in dd.mm.yyyy format", true)
                .addOption(OptionType.STRING, "players", "Players to add", true)
                .queue();
        guild.upsertCommand("forcerem", "Manually removes mentioned users from players list")
                .addOption(OptionType.STRING, "siege_dt", "Date of the siege to remove players in dd.mm.yyyy format", true)
                .addOption(OptionType.STRING, "players", "Players to remove", true)
                .queue();
        guild.upsertCommand("replace", "Moves players from registered list to late list")
                .addOption(OptionType.STRING, "siege_dt", "Date of the siege to replace players in dd.mm.yyyy format", true)
                .addOption(OptionType.STRING, "players", "Players to replace", true)
                .addOption(OptionType.STRING, "message", "Message to replaced members", false)
                .queue();
        guild.upsertCommand("remsiege", "Removes currently scheduled siege")
                .addOption(OptionType.STRING, "siege_dt", "Date of the siege to be removed in dd.mm.yyyy format (if not specified all sieges will be removed)", false)
                .queue();
        guild.upsertCommand("autoreg", "Sets up an autoreg options")
                .addOptions(
                        new OptionData(OptionType.STRING, "action", "Action to be performed by command", true)
                                .addChoice("Add player(s)", "add")
                                .addChoice("Remove player(s)", "rem")
                                .addChoice("Show autoreg players", "list"),
                        new OptionData(OptionType.STRING, "members", "Member(s) to be added or deleted", false))
                .queue();
        guild.upsertCommand("reguser", "Adds member's BDO info")
                .addOption(OptionType.STRING, "bdo_name", "Your BDO family name", true)
                .queue();
        guild.upsertCommand("remuser", "Removes user's BDO info")
                .queue();
        guild.upsertCommand("forcereguser", "Manually adds BDO info for specified discord user")
                .addOptions(
                        new OptionData(OptionType.USER, "discord_user", "Discord mention of player to map", true),
                        new OptionData(OptionType.STRING, "bdo_name", "BDO family name of player", true),
                        new OptionData(OptionType.INTEGER, "priority", "Player's slot priority", false)
                                .setRequiredRange(1, 999))
                .queue();
        guild.upsertCommand("forceremuser", "Manually removes BDO info for specified discord user")
                .addOption(OptionType.USER, "discord_user", "Discord mention of player to remove", true)
                .queue();

        guild.upsertCommand("clear", "Deletes messages from current channel")
                .addOptions(
                        new OptionData(OptionType.INTEGER, "amount", "Amount of messages to delete", true)
                                .setRequiredRange(1, 100))
                .queue();
        guild.upsertCommand("randomize", "Shuffles users who reacted with specified emoji on specified message and outputs them")
                .addOptions(
                        new OptionData(OptionType.STRING, "message", "Message link to watch on", true),
                        new OptionData(OptionType.STRING, "emoji", "Emoji that will be used to get user list", true),
                        new OptionData(OptionType.INTEGER, "amount", "Amount of users to output after shuffling", true)
                                .setMinValue(1L))
                .queue();
        guild.upsertCommand("tictactoe", "Starts TicTacToe game with selected user")
                .addOption(OptionType.USER, "target", "Player with who you want to play", true)
                .queue();
    }

    public static void runShutdownSequence() {
        log.info("Manual shutdown initiated...");
        log.info("Shutting down settings saver daemon");
        SettingsHandler.getSettingsDaemon().interrupt();
        log.info("Shutting down guild handler's daemons");
        GuildManager.getInstance().getGuilds().values().forEach(GuildHandler::stopDaemon);
        log.info("Removing siege instance messages");
        for (GuildHandler handler : GuildManager.getInstance().getGuilds().values()) {
            handler.getInstances().forEach((i) -> {
                        DiscordObjectsOperations.deleteMessageById(i.getChannelId(), i.getMentionMsgId());
                        DiscordObjectsOperations.deleteMessageById(i.getChannelId(), i.getAnnounceMsgId());
                    });
        }
        log.info("Shutting down JDA");
        MainApp.getAPI().shutdown();
        log.info("Saving settings");
        SettingsHandler.saveSettingsToJSON();
        log.info("Shutdown sequence successfully finished, terminating application");
        System.exit(0);
    }

    private static OptionData getBdoChannelsOption(boolean required) {
        OptionData result = new OptionData(OptionType.STRING, "game_channel", "One of predefined channel abbreviations", required);
        for (BDOChannel channel : BDOChannel.values()) {
            result.addChoice(channel.getLabel(), channel.toString());
        }

        return result;
    }
}
