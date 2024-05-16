package org.fotum.app.commands.bdo.league;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.league.LeagueInstance;

public class RemoveLeagueCommand implements ISlashCommand {
    private final String longPattern = "\\d+$";

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        String msgLink = event.getOption("message").getAsString();
        long msgId;
        if (msgLink.matches(this.longPattern)) {
            msgId = Long.parseLong(msgLink);
        } else {
            String[] msgLinkSplitted = msgLink.replace("https://discord.com/channels/", "").split("/");
            if (msgLinkSplitted.length != 3) {
                event.getHook().sendMessage("Could not find message by specified message link").queue();
                return;
            }

            msgId = msgLinkSplitted[2].matches(this.longPattern) ? Long.parseLong(msgLinkSplitted[2]) : 0L;
        }

        if (msgId == 0L) {
            event.getHook().sendMessage("Could not find message by specified message link").queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);

        LeagueInstance toRemove = handler.getLeagueInstances()
                .stream()
                .filter((i) -> i.getAnnounceMsgId() == msgId)
                .findFirst()
                .orElse(null);

        if (toRemove == null) {
            event.getHook().sendMessage("Linked message is not a league announcement or it is already expired").queue();
            return;
        }

        handler.removeLeagueInstance(toRemove);
        event.getHook().sendMessage("Successfully complete").queue();
    }

    @Override
    public String getInvoke() {
        return "remleague";
    }
}
