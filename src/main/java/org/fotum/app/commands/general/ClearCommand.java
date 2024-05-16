package org.fotum.app.commands.general;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.interfaces.ISlashCommand;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class ClearCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        TextChannel channel = event.getChannel().asTextChannel();
        Member selfMember = event.getGuild().getSelfMember();

        if (!selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.getHook().sendMessage("I need the `Manage Messages` permission to use this command").queue();
            return;
        }

        int amount = event.getOption("amount").getAsInt();
        channel.getIterableHistory()
                .takeAsync(amount)
                .thenApplyAsync(
                        (messages) -> {
                            List<Message> goodMessages = messages.stream()
                                    .filter(
                                            (m) -> !m.getTimeCreated().isAfter(OffsetDateTime.now().plus(2, ChronoUnit.WEEKS))
                                    ).collect(Collectors.toList());

                            channel.purgeMessages(goodMessages);
                            return goodMessages.size();
                        }
                ).whenCompleteAsync(
                        (count, thr) -> event.getHook().sendMessage(String.format("Deleted `%d` messages", count)).queue()
                ).exceptionally(
                        (thr) -> {
                            String cause = "";

                            if (thr.getCause() != null)
                            {
                                cause = " caused by " + thr.getCause().getMessage();
                            }

                            event.getHook().sendMessage(String.format("Error: %s%s", thr.getMessage(), cause)).queue();

                            return 0;
                        }
                );
    }

    @Override
    public String getInvoke()
    {
        return "clear";
    }
}
