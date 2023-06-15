package org.fotum.app.utils;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.fotum.app.MainApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    public static Member getGuildMemberById(long guildId, long memberId)
    {
        Guild guild = DiscordObjectsGetters.getGuildById(guildId);
        if (Objects.isNull(guild))
            return null;

        return guild.getMemberById(memberId);
    }

    public static Role getGuildMemberRoleById(long guildId, long memberId, long roleId)
    {
        Member member = DiscordObjectsGetters.getGuildMemberById(guildId, memberId);
        if (Objects.isNull(member))
            return null;

        return member.getRoles()
                .stream()
                .filter((role) -> role.getIdLong() == roleId)
                .findFirst()
                .orElse(null);
    }

    public static List<Long> getGuildMemberCommonRoles(long guildId, long memberId, List<Long> roleIds)
    {
        Member member = DiscordObjectsGetters.getGuildMemberById(guildId, memberId);
        if (Objects.isNull(member))
            return null;

        List<Long> common = new ArrayList<>(roleIds);
        List<Long> memberRoles = member.getRoles()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toList());
        common.retainAll(memberRoles);

        return common.size() > 0 ? common : null;
    }

    public static Role getGuildRoleById(long guildId, long roleId)
    {
        Guild guild = DiscordObjectsGetters.getGuildById(guildId);
        if (Objects.isNull(guild))
            return null;

        return guild.getRoleById(roleId);
    }
}
