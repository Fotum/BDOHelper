package org.fotum.app.commands.interfaces;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;

public interface ITextCommand {
    void handle(List<String> args, MessageReceivedEvent event);

    String getHelp();

    String getInvoke();

    boolean isVisible();

    default boolean canBePrivate() {
        return false;
    }
}
