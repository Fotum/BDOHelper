package org.fotum.app.commands.bdo.siege.control;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PriorityCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();
        String action = event.getOption("action").getAsString();
        switch (action) {
            case ("set") -> this.setPriority(settings, event);
            case ("reset") -> this.resetPriority(settings, event);
            case ("list") -> this.listPriorities(settings, event);
            default -> event.getHook().sendMessage("Undefined action, only set/reset/list actions allowed").queue();
        }
    }

    @Override
    public String getInvoke() {
        return "priority";
    }

    private void setPriority(SiegeSettings settings, SlashCommandInteractionEvent event) {
        OptionMapping playersOpt = event.getOption("players");
        if (playersOpt == null) {
            event.getHook().sendMessage("You have to specify at least one member mention to set priority").queue();
            return;
        }

        OptionMapping priorityOpt = event.getOption("priority");
        if (priorityOpt == null) {
            event.getHook().sendMessage("You have to specify priority value to set (from 1 to 999)").queue();
            return;
        }

        List<Member> membersToApply = playersOpt.getMentions().getMembers();
        int priority = priorityOpt.getAsInt();

        Map<Long, GuildMemberInfo> membersInfo = settings.getRegisteredMembers();
        for (Member member : membersToApply) {
            GuildMemberInfo memberInfo = membersInfo.computeIfAbsent(member.getIdLong(), GuildMemberInfo::new);
            memberInfo.setPriority(priority);
        }

        event.getHook().sendMessage("Player's siege priority successfully updated").queue();
    }

    private void resetPriority(SiegeSettings settings, SlashCommandInteractionEvent event) {
        OptionMapping playersOpt = event.getOption("players");
        if (playersOpt == null) {
            event.getHook().sendMessage("You have to specify at least one member mention to set priority").queue();
            return;
        }

        List<Member> membersToReset = playersOpt.getMentions().getMembers();
        Map<Long, GuildMemberInfo> membersInfo = settings.getRegisteredMembers();
        for (Member member : membersToReset) {
            GuildMemberInfo memberInfo = membersInfo.computeIfAbsent(member.getIdLong(), GuildMemberInfo::new);
            memberInfo.setPriority(999);
        }

        event.getHook().sendMessage("Priorities successfully reset").queue();
    }

    private void listPriorities(SiegeSettings settings, SlashCommandInteractionEvent event) {
        List<GuildMemberInfo> priorityList = settings.getRegisteredMembers().values().stream()
                .filter((v) -> v.getPriority() != 999)
                .sorted()
                .collect(Collectors.toList());

        EmbedBuilder builder = DiscordObjectsOperations.getDefault().setTitle("Priority list");
        StringBuilder descriptionBuilder = builder.getDescriptionBuilder();
        for (GuildMemberInfo memberInfo : priorityList) {
            Member member = DiscordObjectsOperations.getGuildMemberById(event.getGuild().getIdLong(), memberInfo.getDiscordId());
            if (member == null)
                continue;

            descriptionBuilder.append(member.getAsMention()).append(" - ").append(memberInfo.getPriority()).append("\n");
        }

        event.getHook().sendMessageEmbeds(builder.build()).queue();
    }
}
