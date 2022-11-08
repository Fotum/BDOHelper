package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class SetupCommand implements ISlashCommand
{

    @Override
    public void handle(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        if (event.getOptions().isEmpty())
        {
            event.getHook().sendMessage("0 parameters specified. Nothing to update").queue();
            return;
        }

        boolean settingsRequired = false;
        Guild guild = event.getGuild();
        GuildManager manager = GuildManager.getInstance();

        // Init settings if this command used first time in current guild
        GuildSettings settings = manager.getGuildSettings(guild.getIdLong());
        if (settings == null)
        {
            settings = new GuildSettings();
            settingsRequired = true;
        }

        // Listening channel
        OptionMapping option = event.getOption("channel");
        if (settingsRequired && option == null)
        {
            event.getHook().sendMessage("You have to pass a text channel mention").queue();
            return;
        }

        if (option != null)
        {
            ChannelType type = option.getChannelType();
            if (type != ChannelType.TEXT)
            {
                event.getHook().sendMessage("Mentioned channel has to be of type TEXT").queue();
                return;
            }

            long channelId = option.getAsChannel().asTextChannel().getIdLong();
            settings.setListeningChannel(channelId);

            if (settingsRequired)
                manager.addGuildSettings(guild.getIdLong(), settings);
        }

        // Mention roles
        option = event.getOption("mention_roles");
        if (option != null)
        {
            if (option.getAsString().equalsIgnoreCase("delete"))
            {
                settings.setMentionRoles(new ArrayList<>());
            }
            else
            {
                List<Role> mentionRoles = option.getMentions().getRoles();
                settings.setMentionRoles(
                        mentionRoles.stream()
                                .map(ISnowflake::getIdLong)
                                .collect(Collectors.toList())
                );
            }
        }

        // Prefix roles
        option = event.getOption("prefix_roles");
        if (option != null)
        {
            if (option.getAsString().equalsIgnoreCase("delete"))
            {
                settings.setPrefixRoles(new LinkedHashSet<>());
            }
            else
            {
                List<Role> prefixRoles = option.getMentions().getRoles();
                settings.setPrefixRoles(
                        prefixRoles.stream()
                                .map(ISnowflake::getIdLong)
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                );
            }
        }

        event.getHook().sendMessage("Successfully updated").queue();
    }

    @Override
    public String getInvoke()
    {
        return "setup";
    }
}
