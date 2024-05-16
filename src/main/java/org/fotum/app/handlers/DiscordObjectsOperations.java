package org.fotum.app.handlers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.fotum.app.MainApp;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordObjectsOperations {
    private static final Random RANDOM = new Random();

    public static Guild getGuildById(long guildId) {
        return MainApp.getAPI().getGuildById(guildId);
    }

    public static TextChannel getTextChannelById(long channelId) {
        return MainApp.getAPI().getTextChannelById(channelId);
    }

    public static Message getMessageById(long channelId, long messageId) {
        TextChannel channel = DiscordObjectsOperations.getTextChannelById(channelId);
        if (Objects.isNull(channel))
            return null;

        Message result;
        try {
            result = channel.retrieveMessageById(messageId).complete();
        } catch (ErrorResponseException ex) {
            result = null;
        }

        return result;
    }

    public static void deleteMessageById(long channelId, long messageId) {
        Message target = DiscordObjectsOperations.getMessageById(channelId, messageId);
        if (target != null)
            target.delete().complete();
    }

    public static User getUserById(long userId) {
        return MainApp.getAPI().getUserById(userId);
    }

    public static Member getGuildMemberById(long guildId, long memberId) {
        Guild guild = DiscordObjectsOperations.getGuildById(guildId);
        if (Objects.isNull(guild))
            return null;

        return guild.getMemberById(memberId);
    }

    public static Role getGuildMemberRoleById(long guildId, long memberId, long roleId) {
        Member member = DiscordObjectsOperations.getGuildMemberById(guildId, memberId);
        if (Objects.isNull(member))
            return null;

        return member.getRoles()
                .stream()
                .filter((role) -> role.getIdLong() == roleId)
                .findFirst()
                .orElse(null);
    }

    public static java.util.List<Long> getGuildMemberCommonRoles(long guildId, long memberId, java.util.List<Long> roleIds) {
        Member member = DiscordObjectsOperations.getGuildMemberById(guildId, memberId);
        if (Objects.isNull(member))
            return null;

        java.util.List<Long> common = new ArrayList<>(roleIds);
        List<Long> memberRoles = member.getRoles()
                .stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toList());
        common.retainAll(memberRoles);

        return common.size() > 0 ? common : null;
    }

    public static Role getGuildRoleById(long guildId, long roleId) {
        Guild guild = DiscordObjectsOperations.getGuildById(guildId);
        if (Objects.isNull(guild))
            return null;

        return guild.getRoleById(roleId);
    }

    public static void sendMessageToChannel(@NotNull MessageChannel channel, String msg) {
        channel.sendMessage(msg).queue(
                (message) -> message.delete().queueAfter(5L, TimeUnit.SECONDS)
        );
    }

    public static void sendDirectMessage(User user, String content) {
        if (Objects.isNull(user))
            return;

        user.openPrivateChannel()
                .flatMap((channel) -> channel.sendMessage(content))
                .queue();
    }

    public static @NotNull EmbedBuilder getDefault() {
        return new EmbedBuilder()
                .setColor(getRandomColor())
                .setFooter("{BDOHelper}", null)
                .setTimestamp(Instant.now());
    }

    public static @NotNull Color getRandomColor() {
        float r = RANDOM.nextFloat();
        float g = RANDOM.nextFloat();
        float b = RANDOM.nextFloat();

        return new Color(r, g, b);
    }
}
