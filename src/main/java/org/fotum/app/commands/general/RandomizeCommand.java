package org.fotum.app.commands.general;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.interfaces.ISlashCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RandomizeCommand implements ISlashCommand {
    private final String longPattern = "\\d+$";

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long channelId = event.getChannelIdLong();

        String msgLink = event.getOption("message").getAsString();
        final int amount = event.getOption("amount").getAsInt();

        long msgId;
        if (msgLink.matches(this.longPattern)) {
            msgId = Long.parseLong(msgLink);
        } else {
            String[] msgLinkSplitted = msgLink.replace("https://discord.com/channels/", "").split("/");
            if (msgLinkSplitted.length != 3) {
                event.reply("Could not find message by specified message link").setEphemeral(true).queue();
                return;
            }

            guildId = msgLinkSplitted[0].matches(this.longPattern) ? Long.parseLong(msgLinkSplitted[0]) : 0L;
            channelId = msgLinkSplitted[1].matches(this.longPattern) ? Long.parseLong(msgLinkSplitted[1]) : 0L;
            msgId = msgLinkSplitted[2].matches(this.longPattern) ? Long.parseLong(msgLinkSplitted[2]) : 0L;
        }

        if (guildId == 0L || channelId == 0L || msgId == 0L) {
            event.reply("Could not find message by specified message link").setEphemeral(true).queue();
            return;
        }

        Guild guild = DiscordObjectsOperations.getGuildById(guildId);
        if (guild == null) {
            event.reply("Could not find guild by specified message link").setEphemeral(true).queue();
            return;
        }

        Message msg = DiscordObjectsOperations.getMessageById(channelId, msgId);
        if (msg == null) {
            event.reply("Could not find message by specified message link").setEphemeral(true).queue();
            return;
        }

        String emojiStr = event.getOption("emoji").getAsString();
        EmojiUnion inputEmoji = Emoji.fromFormatted(emojiStr);

        MessageReaction reaction = msg.getReactions().stream()
                .filter((mr) -> {
                    EmojiUnion msgEmojiUnion = mr.getEmoji();
                    if (msgEmojiUnion.getType() == Emoji.Type.CUSTOM && inputEmoji.getType() == Emoji.Type.CUSTOM) {
                        CustomEmoji inputCustom = inputEmoji.asCustom();
                        CustomEmoji msgCustom = msgEmojiUnion.asCustom();

                        return inputCustom.getName().equals(msgCustom.getName()) && inputCustom.getIdLong() == msgCustom.getIdLong();
                    } else {
                        return inputEmoji.getName().equals(msgEmojiUnion.getName());
                    }
                })
                .findFirst()
                .orElse(null);

        if (reaction == null) {
            event.reply("Could not find specified reaction under linked message").setEphemeral(true).queue();
            return;
        }

        event.deferReply(false).queue();
        reaction.retrieveUsers().queue((result) -> {
            final int toOutput = Math.min(amount, result.size());
            List<User> toShuffle = new ArrayList<>(result);
            Collections.shuffle(toShuffle);

            String randomizedMentions = toShuffle.subList(0, toOutput).stream().map(IMentionable::getAsMention).collect(Collectors.joining(", "));
            event.getHook().sendMessage(randomizedMentions).queue();
        });
    }

    @Override
    public String getInvoke() {
        return "randomize";
    }
}
