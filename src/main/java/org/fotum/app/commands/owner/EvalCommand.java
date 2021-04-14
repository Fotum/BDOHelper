package org.fotum.app.commands.owner;

import groovy.lang.GroovyShell;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.objects.ICommand;

import java.util.List;

public class EvalCommand implements ICommand
{
    private final GroovyShell engine;
    private final String imports;

    public EvalCommand()
    {
        this.engine = new GroovyShell();
        this.imports = "import java.io.*\n"
                + "import java.lang.*\n"
                + "import java.util.*\n"
                + "import java.util.concurrent.*\n"
                + "import net.dv8tion.jda.api.*\n"
                + "import net.dv8tion.jda.api.entities.*\n"
                + "import net.dv8tion.jda.api.entities.impl.*\n"
                + "import net.dv8tion.jda.api.managers.*\n"
                + "import net.dv8tion.jda.api.managers.impl.*\n"
                + "import net.dv8tion.jda.api.utils.*\n";
    }

    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        if (event.getAuthor().getIdLong() != Constants.OWNER)
        {
            return;
        }

        if (args.isEmpty())
        {
            event.getChannel().sendMessage("Missing arguments").queue();
            return;
        }

        try
        {
            engine.setProperty("args", args);
            engine.setProperty("event", event);
            engine.setProperty("message", event.getMessage());
            engine.setProperty("channel", event.getChannel());
            engine.setProperty("jda", event.getJDA());
            engine.setProperty("guild", event.getGuild());
            engine.setProperty("member", event.getMember());

            String script = imports + event.getMessage().getContentRaw().split("\\s+", 2)[1];
            Object out = engine.evaluate(script);

            event.getChannel().sendMessage(out == null ? "Executed without errors" : out.toString()).queue();
        }
        catch (Exception ex)
        {
            event.getChannel().sendMessage(ex.getMessage()).queue();
        }
    }

    @Override
    public String getHelp()
    {
        return "Takes groovy code and evaluates it";
    }

    @Override
    public String getInvoke()
    {
        return "eval";
    }
}
