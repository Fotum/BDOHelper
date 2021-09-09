package org.fotum.app.objects.checkers;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;

import java.util.List;

public class PermissionChecker
{
    public static boolean checkGeneralPermissions(GuildMessageReceivedEvent event)
    {
        Guild guild = event.getGuild();
        TextChannel channel = event.getChannel();
        GuildManager manager = GuildManager.getInstance();
        GuildSettings settings = manager.getGuildSettings(guild.getIdLong());

        if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
        {
            event.getMessage().delete().queue();
        }

        if (settings == null)
        {
            BotUtils.sendMessageToChannel(channel, "No siege settings configured for this guild");
            return false;
        }

        List<Long> managingRoles = settings.getManagingRoles();
        if (managingRoles.isEmpty())
        {
            BotUtils.sendMessageToChannel(channel, "Siege managing roles are not configured");
            return false;
        }

        boolean authorHasRole = event.getMember().getRoles()
                .stream()
                .anyMatch(
                    (role) -> managingRoles.contains(role.getIdLong())
                );

        if (!authorHasRole)
        {
            BotUtils.sendMessageToChannel(channel, "You do not have permissions to use this command");
            return false;
        }

        return true;
    }
}
