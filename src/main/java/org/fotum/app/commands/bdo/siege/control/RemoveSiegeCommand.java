package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class RemoveSiegeCommand implements ISlashCommand {
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

        OptionMapping siegeDtOpt = event.getOption("siege_dt");
        LocalDate siegeDt = null;
        if (siegeDtOpt != null) {
            String strSiegeDt = siegeDtOpt.getAsString().trim();
            try {
                siegeDt = LocalDate.parse(strSiegeDt, Constants.DATE_FORMAT);
            } catch (DateTimeParseException ex) {
                event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
                return;
            }
        }

        if (siegeDt != null) {
            SiegeInstance toRemove = handler.getSiegeInstance(channelId, siegeDt);
            if (toRemove == null) {
                event.getHook().sendMessage(String.format("No siege for date '%s' found in this channel", siegeDt.format(Constants.DATE_FORMAT))).queue();
                return;
            }

            handler.removeInstance(toRemove);
            event.getHook().sendMessage(String.format("Siege with date `%s` successfully deleted", siegeDtOpt.getAsString().trim())).queue();
        } else {
            for (SiegeInstance toRemove : handler.getInstances()) {
                if (toRemove.getChannelId() == channelId)
                    handler.removeInstance(toRemove);
            }

            event.getHook().sendMessage("All sieges successfully deleted for this channel").queue();
        }
    }

    @Override
    public String getInvoke() {
        return "remsiege";
    }
}
