package org.fotum.app.commands.bdo.league;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.league.LeagueInstance;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class AddLeagueCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannelIdLong();
        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);

        LocalDateTime startDttm;
        try {
            String startDttmStr = event.getOption("start_dttm").getAsString();
            startDttm = LocalDateTime.parse(startDttmStr, Constants.DATE_TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            event.getHook().sendMessage("Incorrect datetime format given, expected format is `dd.mm.yyyy hh:mm`").queue();
            return;
        }

        if (startDttm.isBefore(LocalDateTime.now())) {
            event.getHook().sendMessage("Can only schedule a league on a future date").queue();
            return;
        }

        LeagueInstance sameDttmInst = handler.getLeagueInstance(startDttm);
        if (sameDttmInst != null && sameDttmInst.getChannelId() == channelId) {
            event.getHook().sendMessage(String.format("League for date '%s' in this channel is already exists", startDttm.format(Constants.DATE_TIME_FORMAT))).queue();
            return;
        }

        LeagueInstance leagueInst = new LeagueInstance(handler, startDttm, channelId);
        handler.addLeagueInstance(leagueInst);

        event.getHook().sendMessage("Successfully complete").queue();
    }

    @Override
    public String getInvoke() {
        return "addleague";
    }
}
