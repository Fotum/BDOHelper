package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.commands.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReplacePlayersCommand implements ISlashCommand {
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

        Set<Long> registeredPlayersIds = siegeInst.getRegisteredPlayers()
                .stream()
                .map(GuildMemberInfo::getDiscordId)
                .collect(Collectors.toSet());
        if (siegeInst.getLatePlayers().isEmpty() || registeredPlayersIds.isEmpty()) {
            event.getHook().sendMessage(String.format("There is no queue or any registered members yet for siege with date '%s' in this channel", strSiegeDt)).queue();
            return;
        }

        List<Long> toReplace = event.getOption("players").getMentions()
                .getMembers()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toList());
        if (toReplace.isEmpty()) {
            event.getHook().sendMessage("No member mentions found").queue();
            return;
        }

        Iterator<Long> toReplaceIterator = toReplace.iterator();
        while (toReplaceIterator.hasNext()) {
            Long playerId = toReplaceIterator.next();
            if (!registeredPlayersIds.contains(playerId))
                toReplaceIterator.remove();
            else
                siegeInst.unregisterPlayer(playerId);
        }
        toReplace.forEach(siegeInst::registerPlayer);

        OptionMapping msgToPlayersOpt = event.getOption("message");
        if (msgToPlayersOpt != null) {
            String msgToPlayers = msgToPlayersOpt.getAsString();
            for (long userId : toReplace) {
                User toSend = DiscordObjectsOperations.getUserById(userId);
                DiscordObjectsOperations.sendDirectMessage(toSend, msgToPlayers);
            }
        }

        event.getHook().sendMessage("Player(s) successfully replaced").queue();
    }

    @Override
    public String getInvoke() {
        return "replace";
    }
}
