package org.fotum.app.commands.moderation;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class ClearCommand implements ICommand
{
    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();
        Member member = event.getMember();
        Member selfMember = event.getGuild().getSelfMember();

        if (!member.hasPermission(Permission.MESSAGE_MANAGE))
        {
            BotUtils.sendMessageToChannel(channel, "You need the `Manage Messages` permission to use this command");
            return;
        }

        if (!selfMember.hasPermission(Permission.MESSAGE_MANAGE))
        {
            BotUtils.sendMessageToChannel(channel, "I need the `Manage Messages` permission to use this command");
            return;
        }

        if (args.isEmpty())
        {
            BotUtils.sendMessageToChannel(channel, "Correct usage is: `" + Constants.PREFIX + this.getInvoke() + " <amount to delete>`");
        }

        int amount;
        String arg = args.get(0);

        try
        {
            amount = Integer.parseInt(arg) + 1;
        }
        catch (NumberFormatException ex)
        {
            BotUtils.sendMessageToChannel(channel, String.format("`%s` is not a valid number", arg));
            return;
        }

        if (amount < 2 || amount > 100)
        {
            BotUtils.sendMessageToChannel(channel, "Amount must be at least 1 and at most 100");
            return;
        }

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
                        (count, thr) -> BotUtils.sendMessageToChannel(channel, String.format("Deleted `%d` messages", count - 1))
                ).exceptionally(
                        (thr) -> {
                            String cause = "";

                            if (thr.getCause() != null)
                            {
                                cause = " caused by " + thr.getCause().getMessage();
                            }

                            BotUtils.sendMessageToChannel(channel, String.format("Error: %s%s", thr.getMessage(), cause));

                            return 0;
                        }
                );
    }

    @Override
    public String getHelp()
    {
        return "Cleans specified amount of messages\n" +
            "Usage: `" + Constants.PREFIX + this.getInvoke() + " <amount to delete>`";
    }

    @Override
    public String getInvoke()
    {
        return "clear";
    }
}
