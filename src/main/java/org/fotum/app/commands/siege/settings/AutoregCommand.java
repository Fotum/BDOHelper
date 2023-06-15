package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.utils.BotUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AutoregCommand implements ISlashCommand
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

        String action = event.getOption("action").getAsString();
        switch (action)
        {
            case ("add"):
                this.addAutoregPlayers(event.getOption("members"), event);
                break;

            case ("rem"):
                this.remAutoregPlayers(event.getOption("members"), event);
                break;

            case ("list"):
                this.listAutoregPlayers(event);
                break;

            default:
                event.getHook().sendMessage("Undefined action, only add/rem/list actions allowed").queue();
                break;
        }
    }

    @Override
    public String getInvoke()
    {
        return "autoreg";
    }

    private void addAutoregPlayers(OptionMapping opts, SlashCommandInteractionEvent event)
    {
        if (opts == null)
        {
            event.getHook().sendMessage("You have to specify at least one member mention to add").queue();
            return;
        }

        Long guildId = event.getGuild().getIdLong();
        List<Long> membersInList = GuildManager.getInstance().getGuildSettings(guildId).getAutoregList();

        List<Long> membersToAdd = opts.getMentions().getMembers()
                .stream()
                .map(ISnowflake::getIdLong)
                .filter((memberId) -> !membersInList.contains(memberId))
                .collect(Collectors.toList());

        membersInList.addAll(membersToAdd);
        event.getHook().sendMessage("Successfully added players to autoreg list").queue();
    }

    private void remAutoregPlayers(OptionMapping opts, SlashCommandInteractionEvent event)
    {
        if (opts == null)
        {
            event.getHook().sendMessage("You have to specify at least one member mention to remove").queue();
            return;
        }

        Long guildId = event.getGuild().getIdLong();
        List<Long> membersInList = GuildManager.getInstance().getGuildSettings(guildId).getAutoregList();

        List<Long> membersToRemove = opts.getMentions().getMembers().stream()
                .map(ISnowflake::getIdLong)
                .filter(membersInList::contains)
                .collect(Collectors.toList());

        membersInList.removeAll(membersToRemove);

        event.getHook().sendMessage("Successfully removed players from autoreg list").queue();
    }

    private void listAutoregPlayers(SlashCommandInteractionEvent event)
    {
        Guild guild = event.getGuild();
        GuildManager manager = GuildManager.getInstance();

        EmbedBuilder builder = BotUtils.getDefault().setTitle("A list of autoreg players:");
        StringBuilder descriptionBuilder = builder.getDescriptionBuilder();

        Iterator<Long> autoregListIter = manager.getGuildSettings(guild.getIdLong()).getAutoregList().iterator();
        while (autoregListIter.hasNext())
        {
            Member member = guild.getMemberById(autoregListIter.next());
            if (!Objects.isNull(member))
                descriptionBuilder.append(member.getAsMention()).append("\n");
            else
                autoregListIter.remove();
        }

        event.getHook().sendMessageEmbeds(builder.build()).queue();
    }
}
