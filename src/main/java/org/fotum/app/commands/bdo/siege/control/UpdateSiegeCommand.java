package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.BDOChannel;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class UpdateSiegeCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        if (handler.getSiegeInstances().isEmpty()) {
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

        SiegeInstance inst = handler.getSiegeInstance(siegeDt);
        if (inst == null) {
            event.getHook().sendMessage(String.format("Active siege announcement is not found for date `%s`", strSiegeDt)).queue();
            return;
        }

        OptionMapping newSiegeDtOpt = event.getOption("siege_date");
        OptionMapping newZoneOpt = event.getOption("game_channel");
        OptionMapping newMaxSlotsOpt = event.getOption("max_slots");

        if (newSiegeDtOpt == null && newMaxSlotsOpt == null && newZoneOpt == null) {
            event.getHook().sendMessage("No parameters for update given").queue();
            return;
        }

        StringBuilder resultBuilder = new StringBuilder();
        if (newSiegeDtOpt != null) {
            LocalDate newDate = this.parseDate(newSiegeDtOpt.getAsString());
            resultBuilder.append("`siege_date`: ");

            if (newDate == null) {
                resultBuilder.append("Incorrect date format given, expected format is `dd.mm.yyyy`");
            } else if (newDate.isBefore(LocalDate.now())) {
                resultBuilder.append("Can only change siege date on a future date");
            } else {
                SiegeInstance checkInstance = handler.getSiegeInstance(newDate);
                if (checkInstance != null) {
                    resultBuilder.append(String.format("Siege on `%s` date is already exist", newDate.format(Constants.DATE_FORMAT)));
                } else {
                    inst.setSiegeDt(newDate);
                    resultBuilder.append("Siege date successfully updated");
                }
            }
        }

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

    private LocalDate parseDate(String val) {
        LocalDate result;
        try {
            result = LocalDate.parse(val, Constants.DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            return null;
        }

        return result;
    }
}
