package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.ArrayList;
import java.util.List;

public class RemoveMentionRoles implements ICommand
{
    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        if (!PermissionChecker.checkGeneralPermissions(event))
            return;

        GuildManager.getInstance().getGuildSettings(event.getGuild().getIdLong()).setMentionRoles(new ArrayList<Long>());
        BotUtils.sendMessageToChannel(event.getChannel(), "Mention roles successfully removed");
    }

    @Override
    public String getHelp()
    {
        return "Removes a mention role that is used when siege is announced";
    }

    @Override
    public String getInvoke()
    {
        return "remmention";
    }
}
