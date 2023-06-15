package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;

public class RemUserCommand implements ISlashCommand
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

        long userId = event.getUser().getIdLong();
        settings.getRegisteredMembers()
                .removeIf(
                    (member) -> member.getDiscordId() == userId
                );

        event.getHook().sendMessage("Player's BDO info successfully removed").queue();
    }

    @Override
    public String getInvoke()
    {
        return "remuser";
    }
}
