package org.fotum.app.commands.interfaces;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface IButtonCommand {
    void handle(ButtonInteractionEvent event);

    String getCommandId();
}
