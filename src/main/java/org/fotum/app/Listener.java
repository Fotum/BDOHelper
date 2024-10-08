package org.fotum.app;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionDisconnectEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.handlers.LoadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class Listener extends ListenerAdapter {
    private final CommandManager manager;

    Listener(CommandManager manager) {
        this.manager = manager;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        MainApp.setApiConnected(true);
        log.info(String.format("Logged in as %#s", event.getJDA().getSelfUser()));
        LoadHandler.runStartupSequence();
    }

    @Override
    public void onSessionDisconnect(@NotNull SessionDisconnectEvent event) {
        MainApp.setApiConnected(false);
    }

    @Override
    public void onSessionResume(@NotNull SessionResumeEvent event) {
        MainApp.setApiConnected(true);
    }

    @Override
    public void onSessionRecreate(@NotNull SessionRecreateEvent event) {
        MainApp.setApiConnected(true);
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        LoadHandler.initializeGuildHandler(event.getGuild());
        LoadHandler.upsertSlashCommands(event.getGuild());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        User author = event.getAuthor();
        String content = event.getMessage().getContentDisplay();
        String attachments = event.getMessage().getAttachments().stream()
                .map(Message.Attachment::getUrl)
                .collect(Collectors.joining("\n"));

        if (!content.isBlank() && !attachments.isBlank())
            content += "\n";

        if (event.isFromType(ChannelType.PRIVATE) && !author.isBot()) {
            if (author.getIdLong() != Constants.OWNER) {
                log.info(String.format("[PRIV] <%#s>: %s%s", author, content, attachments));
                // Forward message to owner
                DiscordObjectsOperations.sendDirectMessage(
                        event.getJDA().getUserById(Constants.OWNER),
                        String.format("[FORWARD] <%#s>: %s%s", author, content, attachments)
                );
            }
        } else if (event.isFromType(ChannelType.TEXT)) {
            Guild guild = event.getGuild();
            TextChannel textChannel = event.getChannel().asTextChannel();

            log.info(String.format("(%s) [%s] <%#s>: %s%s", guild.getName(), textChannel.getName(), author, content, attachments));
        }

        this.manager.handleTextCommand(event);
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        if (event.isFromGuild() && event.isFromType(ChannelType.TEXT)) {
            long guildId = event.getGuild().getIdLong();
            long msgId = event.getMessageIdLong();

            GuildManager.getInstance().getGuildHandler(guildId).checkHandleInstanceDeletion(msgId);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild()) {
            Guild guild = event.getGuild();

            log.info(String.format("(%s) [%s] command was used by <%#s> with options:\n%s",
                    Objects.nonNull(guild) ? guild.getName() : "Unknown",
                    event.getName(),
                    event.getUser(),
                    event.getOptions().stream()
                            .map((opt) -> opt.getName() + ": " + opt.getAsString())
                            .collect(Collectors.joining("\n")))
            );

            this.manager.handleSlashCommand(event);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.isFromGuild())
            this.manager.handleButtonCommand(event);
    }

    @Override
    public void onGenericMessageReaction(@NotNull GenericMessageReactionEvent event) {
        if (event.isFromGuild()) {
            User author = event.getUser();
            if (author == null)
                return;

            log.info(String.format("(%s) [%s] emoji was %s by <%#s> in channel '%s' on message with id '%d'",
                    event.getGuild().getName(),
                    event.getEmoji().getName(),
                    event instanceof MessageReactionAddEvent ? "added" : "removed",
                    author,
                    event.getChannel().getName(),
                    event.getMessageIdLong())
            );

            this.manager.handleMessageReaction(event);
        }
    }
}
