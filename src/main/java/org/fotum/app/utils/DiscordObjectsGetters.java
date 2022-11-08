package org.fotum.app.utils;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.fotum.app.MainApp;

import java.util.Objects;

public class DiscordObjectsGetters
{
    public static Guild getGuildById(long guildId)
    {
        return MainApp.getAPI().getGuildById(guildId);
    }

    public static TextChannel getTextChannelById(long channelId)
    {
        return MainApp.getAPI().getTextChannelById(channelId);
    }

    public static Message getMessageById(long channelId, long messageId)
    {
        TextChannel channel = DiscordObjectsGetters.getTextChannelById(channelId);
        if (Objects.isNull(channel))
            return null;

        Message result;
        try
        {
            result = channel.retrieveMessageById(messageId).complete();
        }
        catch (ErrorResponseException ex)
        {
            result = null;
        }

        return result;
    }

    public static User getUserById(long userId)
    {
        return MainApp.getAPI().getUserById(userId);
    }
}
