package org.fotum.app;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.commands.owner.EvalCommand;
import org.fotum.app.commands.siege.*;
import org.fotum.app.objects.ICommand;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager
{
	private final Map<String, ICommand> commands = new HashMap<String, ICommand>();
	
	public CommandManager()
	{
		this.addCommand(new AddSiegeCommand());
		this.addCommand(new RemoveSiegeCommand());
		this.addCommand(new SetListeningChannel());
		this.addCommand(new RemoveListeningChannel());
		this.addCommand(new SetManagingRole());
		this.addCommand(new RemoveManagingRole());
		// this.addCommand(new SetTitleMessage());
		this.addCommand(new SetPrefixRoles());
		// this.addCommand(new SetDescriptionMessage());
		
		this.addCommand(new AddPlayerCommand());
		this.addCommand(new RemovePlayerCommand());
		
		this.addCommand(new ForceAddPlayersCommand());
		this.addCommand(new ForceRemPlayersCommand());
		
		this.addCommand(new SiegeHelp(this));
		this.addCommand(new EvalCommand());
	}
	
	public Collection<ICommand> getCommands()
	{
		return this.commands.values();
	}
	
	public ICommand getCommand(String name)
	{
		return this.commands.get(name);
	}
	
	void handleCommand(GuildMessageReceivedEvent event)
	{
		final String[] split = event.getMessage().getContentRaw().replaceFirst("(?i)" + Pattern.quote(Constants.PREFIX), "").split("\\s+");
		final String invoke = split[0].toLowerCase();
		
		if (commands.containsKey(invoke))
		{
			final List<String> args = Arrays.asList(split).subList(1, split.length);
			commands.get(invoke).handle(args, event);
		}
	}
	
	void handleAudCommand(GuildMessageReceivedEvent event, String invoke)
	{
		commands.get(invoke).handle(null, event);
	}

	private void addCommand(ICommand command)
	{
		if (!this.commands.containsKey(command.getInvoke()))
		{
			this.commands.put(command.getInvoke(), command);
		}
	}
}
