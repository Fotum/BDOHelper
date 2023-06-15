package org.fotum.app.commands.owner;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.MainApp;
import org.fotum.app.interfaces.ITextCommand;
import org.fotum.app.utils.BotUtils;

import java.util.List;

@Slf4j
public class ShutdownCommand implements ITextCommand
{
    @Override
    public void handle(List<String> args, MessageReceivedEvent event)
    {
        if (event.getAuthor().getIdLong() != Constants.OWNER)
            return;

        log.info("Manual shutdown initiated...");
        BotUtils.runShutdownSequence();
        log.info("Shutdown sequence successfully finished, shutting down JDA");
        MainApp.getAPI().shutdown();
        System.exit(0);
    }

    @Override
    public String getHelp()
    {
        return "Shuts down bot application";
    }

    @Override
    public String getInvoke()
    {
        return "helperoff";
    }

    @Override
    public boolean isVisible()
    {
        return false;
    }

    @Override
    public boolean canBePrivate()
    {
        return true;
    }
}
