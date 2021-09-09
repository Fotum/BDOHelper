package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;
import java.util.stream.Collectors;

public class SetMentionRoles implements ICommand
{
    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        if (args.isEmpty())
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
            return;
        }

        if (!PermissionChecker.checkGeneralPermissions(event))
            return;

        List<Role> mentionedRoles = event.getMessage().getMentionedRoles();
        if (mentionedRoles.isEmpty())
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect role mentions given");
            return;
        }

        List<Long> roleIds = mentionedRoles.stream()
                                .map(ISnowflake::getIdLong)
                                .collect(Collectors.toList());
        GuildManager.getInstance().getGuildSettings(event.getGuild().getIdLong()).setMentionRoles(roleIds);
        BotUtils.sendMessageToChannel(channel, "Mention roles successfully set");
    }

    @Override
    public String getHelp()
    {
        return "Sets a mention role to use when siege is announced\n" +
                "Usage: `" + Constants.PREFIX + this.getInvoke() + " <@role>`";
    }

    @Override
    public String getInvoke()
    {
        return "setmention";
    }
}
