package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.commands.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.BDOChannel;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeInstance;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AddSiegeCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannelIdLong();

        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        SiegeSettings settings = handler.getSiegeSettings();

        LocalDate startDt;
        try {
            String startDtStr = event.getOption("siege_dt").getAsString();
            startDt = LocalDate.parse(startDtStr, Constants.DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
            return;
        }

        if (startDt.isBefore(LocalDate.now())) {
            event.getHook().sendMessage("Can only schedule a siege on a future date").queue();
            return;
        }

        if (handler.getSiegeInstance(channelId, startDt) != null) {
            event.getHook().sendMessage(String.format("Siege announce for date '%s' is already exist in this channel", startDt.format(Constants.DATE_FORMAT))).queue();
            return;
        }

        BDOChannel zone = BDOChannel.valueOf(event.getOption("game_channel").getAsString());
        int playersAmount = event.getOption("slots").getAsInt();

        // Creating new siege instance with given parameters
        SiegeInstance siegeInst = new SiegeInstance(handler, channelId, startDt, zone, playersAmount);
        // If autoreg settings exist for this guild - add players to registred list
        settings.getAutoregList().stream().map(GuildMemberInfo::getDiscordId).forEach(siegeInst::registerPlayer);
        handler.addInstance(siegeInst);

        event.getHook().sendMessage("Successfully complete").queue();
    }

    @Override
    public String getInvoke() {
        return "addsiege";
    }
}
