package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.structs.DiscordMemberInfo;

import java.util.Map;

public class ForceRegUserCommand implements ISlashCommand
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

        long toAdd = event.getOption("discord_user").getAsUser().getIdLong();
        String nameStr = event.getOption("bdo_name").getAsString();
        String allegiance = event.getOption("bdo_allegiance").getAsString();

        DiscordMemberInfo info = new DiscordMemberInfo(toAdd, nameStr, allegiance);

        settings.getRegisteredMembers().remove(info);
        settings.getRegisteredMembers().add(info);

        event.getHook().sendMessage("Player's BDO info successfully updated").queue();
    }

    @Override
    public String getInvoke()
    {
        return "forcereguser";
    }
}
