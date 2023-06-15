package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;

import java.util.Map;

public class ForceRemUserCommand implements ISlashCommand
{
    @Override
    public void handle(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        Guild guild = event.getGuild();
        GuildManager manager = GuildManager.getInstance();

        GuildSettings settings = manager.getGuildSettings(guild.getIdLong());
        if (settings == null)
        {
            event.getHook().sendMessage("No siege settings configured for this guild").queue();
            return;
        }

        Long toRemove = event.getOption("discord_user").getAsUser().getIdLong();
        settings.getRegisteredMembers()
                .removeIf(
                        (member) -> member.getDiscordId() == toRemove
                );

        event.getHook().sendMessage("Player's BDO info successfully removed").queue();
    }

    @Override
    public String getInvoke()
    {
        return "forceremuser";
    }
}
