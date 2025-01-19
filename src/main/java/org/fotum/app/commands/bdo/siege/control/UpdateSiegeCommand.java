package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.commands.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.BDOChannel;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class UpdateSiegeCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannelIdLong();
        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        if (handler.getInstances().isEmpty()) {
            event.getHook().sendMessage("No siege instances found for this guild").queue();
            return;
        }

        String strSiegeDt = event.getOption("inst_dt").getAsString().trim();
        LocalDate siegeDt;
        try {
            siegeDt = LocalDate.parse(strSiegeDt, Constants.DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
            return;
        }

        SiegeInstance inst = handler.getSiegeInstance(channelId, siegeDt);
        if (inst == null) {
            event.getHook().sendMessage(String.format("Active siege announcement is not found for date `%s` in this channel", strSiegeDt)).queue();
            return;
        }

        OptionMapping newZoneOpt = event.getOption("game_channel");
        OptionMapping newMaxSlotsOpt = event.getOption("max_slots");

        StringBuilder resultBuilder = new StringBuilder();
        if (newZoneOpt != null) {
            if (resultBuilder.length() != 0) {
                resultBuilder.append("\r\n");
            }

            BDOChannel bdoChannel = BDOChannel.valueOf(newZoneOpt.getAsString());
            inst.setZone(bdoChannel);
            resultBuilder.append("`game_channel`: Siege zone successfully updated");
        }

        if (newMaxSlotsOpt != null) {
            if (resultBuilder.length() != 0) {
                resultBuilder.append("\r\n");
            }

            inst.setPlayersMax(newMaxSlotsOpt.getAsInt());
            resultBuilder.append("`max_slots`: Siege maximum players successfully updated");
        }

        event.getHook().sendMessage(resultBuilder.toString()).queue();
    }

    @Override
    public String getInvoke() {
        return "updsiege";
    }
}
