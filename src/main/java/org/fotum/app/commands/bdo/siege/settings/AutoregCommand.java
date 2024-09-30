package org.fotum.app.commands.bdo.siege.settings;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AutoregCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();

        String action = event.getOption("action").getAsString();
        switch (action) {
            case ("add") -> this.addAutoregPlayers(settings, event);
            case ("rem") -> this.remAutoregPlayers(settings, event);
            case ("list") -> this.listAutoregPlayers(settings, event);
            default -> event.getHook().sendMessage("Undefined action, only add/rem/list actions allowed").queue();
        }
    }

    @Override
    public String getInvoke() {
        return "autoreg";
    }

    private void addAutoregPlayers(SiegeSettings settings, SlashCommandInteractionEvent event) {
        OptionMapping opts = event.getOption("members");
        if (opts == null) {
            event.getHook().sendMessage("You have to specify at least one member mention to add").queue();
            return;
        }

        Map<Long, GuildMemberInfo> registeredMembers = settings.getRegisteredMembers();
        List<GuildMemberInfo> autoregList = settings.getAutoregList();

        List<GuildMemberInfo> membersToAdd = opts.getMentions().getMembers()
                .stream()
                .map(ISnowflake::getIdLong)
                .map((id) -> registeredMembers.computeIfAbsent(id, GuildMemberInfo::new))
                .filter((m) -> !autoregList.contains(m))
                .collect(Collectors.toList());

        autoregList.addAll(membersToAdd);
        event.getHook().sendMessage("Successfully added players to autoreg list").queue();
    }

    private void remAutoregPlayers(SiegeSettings settings, SlashCommandInteractionEvent event) {
        OptionMapping opts = event.getOption("members");
        if (opts == null) {
            event.getHook().sendMessage("You have to specify at least one member mention to remove").queue();
            return;
        }

        List<GuildMemberInfo> autoregList = settings.getAutoregList();
        List<Long> membersToRemove = opts.getMentions().getMembers().stream()
                .map(ISnowflake::getIdLong)
                .collect(Collectors.toList());

        for (long memberId : membersToRemove) {
            autoregList.removeIf((m) -> m.getDiscordId() == memberId);
        }

        event.getHook().sendMessage("Successfully removed players from autoreg list").queue();
    }

    private void listAutoregPlayers(SiegeSettings settings, SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        EmbedBuilder builder = DiscordObjectsOperations.getDefault().setTitle("A list of autoreg players:");
        StringBuilder descriptionBuilder = builder.getDescriptionBuilder();

        Iterator<GuildMemberInfo> autoregListIter = settings.getAutoregList().iterator();
        while (autoregListIter.hasNext()) {
            GuildMemberInfo info = autoregListIter.next();
            Member member = DiscordObjectsOperations.getGuildMemberById(guild.getIdLong(), info.getDiscordId());
            if (member != null)
                descriptionBuilder.append(member.getAsMention()).append("\n");
            else
                autoregListIter.remove();
        }

        event.getHook().sendMessageEmbeds(builder.build()).queue();
    }
}
