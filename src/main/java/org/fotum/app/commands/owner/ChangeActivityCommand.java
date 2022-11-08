package org.fotum.app.commands.owner;

import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.MainApp;
import org.fotum.app.interfaces.ITextCommand;

import java.util.List;
import java.util.Objects;

public class ChangeActivityCommand implements ITextCommand
{
    @Override
    public void handle(List<String> args, MessageReceivedEvent event)
    {
        if (event.getAuthor().getIdLong() != Constants.OWNER)
        {
            return;
        }

        if (args.isEmpty() || args.size() < 2)
        {
            event.getChannel().asTextChannel().sendMessage("Missing arguments").queue();
            return;
        }

        String activityType = args.get(0);
        String activityText = String.join(" ", args.subList(1, args.size()));

        Activity newActivity = null;
        switch (activityType)
        {
            case "playing":
                newActivity = Activity.playing(activityText);
                break;

            case "competing":
                newActivity = Activity.competing(activityText);
                break;

            case "listening":
                newActivity = Activity.listening(activityText);
                break;

            case "watching":
                newActivity = Activity.watching(activityText);
                break;

//            case "streaming":
//                newActivity = Activity.streaming(activityText);
//                break;

            default:
                break;
        }


        if (Objects.isNull(newActivity))
        {
            event.getChannel().asTextChannel().sendMessage("Could not find specified activity: `" + activityType + "`").queue();
            return;
        }

        MainApp.getAPI().setActivity(newActivity);
    }

    @Override
    public String getHelp()
    {
        return "Changes bot's current activity";
    }

    @Override
    public String getInvoke()
    {
        return "activity";
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
