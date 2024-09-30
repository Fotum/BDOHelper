package org.fotum.app.modules.bdo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UpdaterDaemon extends Thread {
    private final GuildHandler handler;
    private final Set<SiegeInstance> removeList;

    @Getter
    private volatile boolean isRunning = false;

    public UpdaterDaemon(GuildHandler handler) {
        this.handler = handler;
        this.removeList = new HashSet<>();

        log.info(String.format("Instance updater daemon initialized for guild with id %d", handler.getGuildId()));
    }

    @Override
    public void run() {
        this.isRunning = true;

        while (this.isRunning) {
            List<SiegeInstance> instances = this.handler.getInstances();
            if (instances.isEmpty()) {
                this.stopUpdater();
                return;
            }

            try {
                // Getting date and time of now to determine when should we unschedule instances
                LocalDateTime dttmNow = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
                Iterator<SiegeInstance> instancesIterator = instances.iterator();
                while (instancesIterator.hasNext()) {
                    SiegeInstance instance = instancesIterator.next();

                    boolean needMention = instance.isNeedMention();
                    boolean needRedraw = instance.isNeedRedraw();
                    boolean disableButtons = !dttmNow.isBefore(instance.getDisableAtDttm());

                    if (disableButtons && !instance.isButtonsDisabled()) {
                        needRedraw = true;
                        instance.setButtonsDisabled(true);
                    }

                    // Unschedule siege instance
                    if (!dttmNow.isBefore(instance.getUnscheduleAtDttm()) || this.removeList.contains(instance)) {
                        DiscordObjectsOperations.deleteMessageById(instance.getChannelId(), instance.getMentionMsgId());
                        DiscordObjectsOperations.deleteMessageById(instance.getChannelId(), instance.getAnnounceMsgId());

                        instancesIterator.remove();
                        this.removeList.remove(instance);

                        log.info(String.format("Unscheduled siege for guild with ID %d and date %s",
                                this.handler.getGuildId(),
                                instance.getSiegeDt().format(Constants.DATE_FORMAT))
                        );
                    } else {
                        // Update instance
                        this.updateSiegeInstance(instance, needMention, needRedraw, disableButtons);
                    }
                }
            } catch (InterruptedException ex) {
                this.stopUpdater();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void pushRemoveInstance(SiegeInstance instance) {
        this.removeList.add(instance);
    }

    public void stopUpdater() {
        this.isRunning = false;
        log.info(String.format("Stopped siege instance updater daemon for guild with id %d", this.handler.getGuildId()));
    }

    private void updateSiegeInstance(SiegeInstance instance, boolean needMention, boolean needRedraw, boolean disableButtons) throws InterruptedException {
        if (needMention) {
            String mentionMessage = instance.generateMentionMessage();
            if (!mentionMessage.isBlank()) {
                DiscordObjectsOperations.getTextChannelById(instance.getChannelId())
                        .sendMessage(mentionMessage)
                        .queue((message) -> instance.setMentionMsgId(message.getIdLong()));
            }
        }

        if (needRedraw) {
            long channelId = instance.getChannelId();

            Message siegeEmbedMessage = DiscordObjectsOperations.getMessageById(channelId, instance.getAnnounceMsgId());
            EmbedBuilder builder = instance.generateSiegeEmbed();

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.success("button-plus", "➕").withDisabled(disableButtons));
            buttons.add(Button.danger("button-minus", "➖").withDisabled(disableButtons));
            String teamspeakLink = this.handler.getSiegeSettings().getTeamspeakLink();
            if (teamspeakLink != null) {
                buttons.add(Button.link(teamspeakLink, "TeamSpeak 3"));
            }

            if (siegeEmbedMessage != null) {
                siegeEmbedMessage.editMessageEmbeds(builder.build())
                        .setActionRow(buttons)
                        .queue();
            } else {
                DiscordObjectsOperations.getTextChannelById(channelId)
                        .sendMessageEmbeds(builder.build())
                        .setActionRow(buttons)
                        .queue((message) -> instance.setAnnounceMsgId(message.getIdLong()));
            }

            TimeUnit.MILLISECONDS.sleep(200);
        }
    }
}
