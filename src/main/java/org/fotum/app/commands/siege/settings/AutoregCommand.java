package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.EmbedCreator;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;
import java.util.stream.Collectors;

public class AutoregCommand implements ICommand
{

    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        if (args.isEmpty())
        {
            BotUtils.sendMessageToChannel(event.getChannel(), "Incorrect number of arguments given");
            return;
        }

        switch (args.remove(0).toLowerCase())
        {
            case ("add"):
                this.addAutoregPlayers(args, event);
                break;

            case ("rem"):
                this.remAutoregPlayers(args, event);
                break;

            case ("list"):
                this.listAutoregPlayers(event);
                break;

            default:
                BotUtils.sendMessageToChannel(event.getChannel(), "Incorrect parameters given");
                break;
        }
    }

    @Override
    public String getHelp()
    {
        return "Adds player to autoreg list, players from this list will be " +
                "automatically registed to upcoming sieges\n" +
                "Usage: `" + Constants.PREFIX + this.getInvoke() + " [add/rem/list] <@player1>, <@player2> ...`";
    }

    @Override
    public String getInvoke()
    {
        return "autoreg";
    }

    private void addAutoregPlayers(List<String> args, GuildMessageReceivedEvent event)
    {
        if (!PermissionChecker.checkGeneralPermissions(event))
            return;

        TextChannel channel = event.getChannel();

        if (args.size() < 1)
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
            return;
        }

        Long guildId = event.getGuild().getIdLong();
        List<Long> membersInList = GuildManager.getInstance().getGuildSettings(guildId).getAutoregList();

        membersInList.addAll(
                event.getMessage().getMentionedMembers().stream()
                        .map(ISnowflake::getIdLong)
                        .filter((member) -> !membersInList.contains(member))
                        .collect(Collectors.toList())
        );

        BotUtils.sendMessageToChannel(channel, "Successfully added players to autoreg list");
    }

    private void remAutoregPlayers(List<String> args, GuildMessageReceivedEvent event)
    {
        if (!PermissionChecker.checkGeneralPermissions(event))
            return;

        TextChannel channel = event.getChannel();

        if (args.size() < 1)
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
            return;
        }

        Long guildId = event.getGuild().getIdLong();
        List<Long> membersInList = GuildManager.getInstance().getGuildSettings(guildId).getAutoregList();

        membersInList.removeAll(
                event.getMessage().getMentionedMembers().stream()
                    .map(ISnowflake::getIdLong)
                    .collect(Collectors.toList())
        );

        BotUtils.sendMessageToChannel(channel, "Successfully removed players from autoreg list");
    }

    private void listAutoregPlayers(GuildMessageReceivedEvent event)
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
            return;
        }


        EmbedBuilder builder = EmbedCreator.getDefault().setTitle("A list of autoreg players:");
        StringBuilder descriptionBuilder = builder.getDescriptionBuilder();

        manager.getGuildSettings(guild.getIdLong()).getAutoregList().stream()
                .map(guild::getMemberById)
                .forEach(
                    (member) -> descriptionBuilder.append(member.getAsMention()).append("\n")
                );

        channel.sendMessage(builder.build()).queue();
        builder.clear();
    }
}
