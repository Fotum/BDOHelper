package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class ForceRemPlayersCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannelIdLong();
        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        if (handler.getInstances().isEmpty()) {
            event.getHook().sendMessage("No siege instances found for this guild").queue();
            return;
        }

        String strSiegeDt = event.getOption("siege_dt").getAsString();
        LocalDate siegeDt;
        try {
            siegeDt = LocalDate.parse(strSiegeDt, Constants.DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
            return;
        }

        SiegeInstance siegeInst = handler.getSiegeInstance(channelId, siegeDt);
        if (siegeInst == null) {
            event.getHook().sendMessage(String.format("Active siege announcement is not found for date `%s` in this channel", strSiegeDt)).queue();
            return;
        }

        event.getOption("players").getMentions().getMembers().forEach(
                (member) -> siegeInst.unregisterPlayer(member.getIdLong())
        );

        event.getHook().sendMessage("Player(s) successfully removed").queue();
    }

    @Override
    public String getInvoke() {
        return "forcerem";
    }
}
