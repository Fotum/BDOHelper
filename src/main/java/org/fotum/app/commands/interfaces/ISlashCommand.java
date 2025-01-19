package org.fotum.app.commands.interfaces;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface ISlashCommand {
    void handle(SlashCommandInteractionEvent event);

    String getInvoke();
}
