package org.fotum.app.modules.bdo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.modules.bdo.league.LeagueInstance;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UpdaterDaemon extends Thread {
    private final GuildHandler handler;
    private final Set<SiegeInstance> siegeRemoveList;
    private final Set<LeagueInstance> leagueRemoveList;

    @Getter
    private volatile boolean isRunning = false;

    public UpdaterDaemon(GuildHandler handler) {
        this.handler = handler;
        this.siegeRemoveList = new HashSet<>();
        this.leagueRemoveList = new HashSet<>();

        log.info(String.format("Instance updater daemon initialized for guild with id %d", handler.getGuildId()));
    }

    @Override
    public void run() {
        this.isRunning = true;

        while (this.isRunning) {
            List<SiegeInstance> siegeInstances = this.handler.getSiegeInstances();
            List<LeagueInstance> leagueInstances = this.handler.getLeagueInstances();
            if (siegeInstances.isEmpty() && leagueInstances.isEmpty()) {
                this.stopUpdater();
                return;
            }

            try {
                // Getting date and time of now to determine when should we unschedule instances
                LocalDateTime dttmNow = LocalDateTime.now(ZoneId.of("Europe/Moscow"));
                LocalDate dateNow = dttmNow.toLocalDate();
                LocalTime timeNow = dttmNow.toLocalTime();
                // Getting current day of week
                DayOfWeek dayOfWeekNow = dttmNow.getDayOfWeek();

                // Calculate instance state change times
                int disableHour = (dayOfWeekNow != DayOfWeek.SATURDAY) ? 20 : 19;
                LocalTime disableAtTime = LocalTime.of(disableHour, 0);
                LocalTime unschedAtTime = LocalTime.of(disableHour + 1, 0);

                Iterator<SiegeInstance> siegeInstanceIterator = siegeInstances.iterator();
                while (siegeInstanceIterator.hasNext()) {
                    SiegeInstance instance = siegeInstanceIterator.next();

                    LocalDate instDt = instance.getSiegeDt();
                    boolean needMention = instance.isNeedMention();
                    boolean needRedraw = instance.isNeedRedraw();
                    boolean disableButtons = dateNow.isEqual(instDt) && timeNow.isAfter(disableAtTime);

                    if (disableButtons && !instance.isButtonsDisabled()) {
                        needRedraw = true;
                        instance.setButtonsDisabled(true);
                    }

                    // Unschedule siege instance
                    if ((dateNow.isEqual(instDt) && timeNow.isAfter(unschedAtTime)) || dateNow.isAfter(instDt) || this.siegeRemoveList.contains(instance)) {
                        long channelId = this.handler.getSiegeSettings().getListeningChannel();
                        DiscordObjectsOperations.deleteMessageById(channelId, instance.getMessageMentionId());
                        DiscordObjectsOperations.deleteMessageById(channelId, instance.getSiegeAnnounceMsgId());

                        siegeInstanceIterator.remove();
                        this.siegeRemoveList.remove(instance);

                        log.info(String.format("Unscheduled siege for guild with ID %d and date %s",
                                this.handler.getGuildId(),
                                instDt.format(Constants.DATE_FORMAT))
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

    public void pushRemoveSiegeInstance(SiegeInstance instance) {
        this.siegeRemoveList.add(instance);
    }

    public void pushRemoveLeagueInstance(LeagueInstance instance) {
        this.leagueRemoveList.add(instance);
    }

    public void stopUpdater() {
        this.isRunning = false;
        log.info(String.format("Stopped siege instance updater daemon for guild with id %d", this.handler.getGuildId()));
    }

    private void updateSiegeInstance(SiegeInstance instance, boolean needMention, boolean needRedraw, boolean disableButtons) throws InterruptedException {
        if (needMention) {
            String mentionMessage = instance.generateMentionMessage();
            if (!mentionMessage.isBlank()) {
                long channelId = this.handler.getSiegeSettings().getListeningChannel();
                DiscordObjectsOperations.getTextChannelById(channelId)
                        .sendMessage(mentionMessage)
                        .queue((message) -> instance.setMessageMentionId(message.getIdLong()));
            }
        }

        if (needRedraw) {
            long channelId = this.handler.getSiegeSettings().getListeningChannel();

            Message siegeEmbedMessage = DiscordObjectsOperations.getMessageById(channelId, instance.getSiegeAnnounceMsgId());
            EmbedBuilder builder = instance.generateSiegeEmbed();

            List<Button> buttons = new ArrayList<>();
            buttons.add(Button.success("siege-button-plus", "➕").withDisabled(disableButtons));
            buttons.add(Button.danger("siege-button-minus", "➖").withDisabled(disableButtons));
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
                        .queue((message) -> instance.setSiegeAnnounceMsgId(message.getIdLong()));
            }

            TimeUnit.MILLISECONDS.sleep(200);
        }
    }
}
