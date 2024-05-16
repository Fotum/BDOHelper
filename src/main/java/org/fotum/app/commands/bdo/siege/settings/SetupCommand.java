package org.fotum.app.commands.bdo.siege.settings;

import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

import java.util.List;
import java.util.stream.Collectors;

public class SetupCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (event.getOptions().isEmpty()) {
            event.getHook().sendMessage("No parameters specified. Nothing to update").queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();
        // Listening channel
        OptionMapping channelOption = event.getOption("channel");
        if (channelOption != null) {
            ChannelType type = channelOption.getChannelType();
            if (type != ChannelType.TEXT) {
                event.getHook().sendMessage("Mentioned channel has to be of type TEXT").queue();
                return;
            }

            long channelId = channelOption.getAsChannel().asTextChannel().getIdLong();
            settings.setListeningChannel(channelId);
        }

        // Mention roles
        OptionMapping mentionOption = event.getOption("mention_roles");
        if (mentionOption != null) {
            if (mentionOption.getAsString().equalsIgnoreCase("delete")) {
                settings.getMentionRoles().clear();
            } else {
                List<Role> mentionRoles = mentionOption.getMentions().getRoles();
                settings.getMentionRoles().clear();
                settings.getMentionRoles()
                        .addAll(
                                mentionRoles.stream()
                                        .map(ISnowflake::getIdLong)
                                        .collect(Collectors.toList())
                        );
            }
        }

        // Teamspeak 3 link
        OptionMapping teamspeakOption = event.getOption("ts3_link");
        if (teamspeakOption != null) {
            String link = teamspeakOption.getAsString().trim();
            settings.setTeamspeakLink(link);
        }

        event.getHook().sendMessage("Successfully updated").queue();
    }

    @Override
    public String getInvoke() {
        return "setup";
    }
}
