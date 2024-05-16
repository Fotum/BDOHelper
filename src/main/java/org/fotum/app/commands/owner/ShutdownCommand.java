package org.fotum.app.commands.owner;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.handlers.LoadHandler;
import org.fotum.app.interfaces.ITextCommand;

import java.util.List;

@Slf4j
public class ShutdownCommand implements ITextCommand {
    @Override
<<<<<<< Updated upstream
    public void handle(List<String> args, MessageReceivedEvent event)
    {
=======
    public void handle(List<String> args, MessageReceivedEvent event) {
>>>>>>> Stashed changes
        if (event.getAuthor().getIdLong() != Constants.OWNER)
            return;

        LoadHandler.runShutdownSequence();
    }

    @Override
    public String getHelp() {
        return "Shuts down bot application";
    }

    @Override
    public String getInvoke() {
        return "shutdown";
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public boolean canBePrivate() {
        return true;
    }
}
