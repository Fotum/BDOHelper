package org.fotum.app;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.fotum.app.commands.siege.AddPlayerCommand;
import org.fotum.app.commands.siege.AddSiegeCommand;
import org.fotum.app.commands.siege.GetRegisteredPlayers;
import org.fotum.app.commands.siege.RemoveListeningChannel;
import org.fotum.app.commands.siege.RemoveManagingRole;
import org.fotum.app.commands.siege.RemovePlayerCommand;
import org.fotum.app.commands.siege.RemoveSiegeCommand;
import org.fotum.app.commands.siege.SetAnnouncementDelay;
import org.fotum.app.commands.siege.SetAnnouncementMessage;
import org.fotum.app.commands.siege.SetListeningChannel;
import org.fotum.app.commands.siege.SetManagingRole;
import org.fotum.app.commands.siege.SetPrefixRoles;
import org.fotum.app.commands.siege.SetRepeatMessage;
import org.fotum.app.commands.siege.SiegeHelp;
import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
		this.addCommand(new SetAnnouncementDelay());
		this.addCommand(new SetAnnouncementMessage());
		this.addCommand(new SetPrefixRoles());
		this.addCommand(new SetRepeatMessage());
		this.addCommand(new GetRegisteredPlayers());
		
		this.addCommand(new AddPlayerCommand());
		this.addCommand(new RemovePlayerCommand());
		
		this.addCommand(new SiegeHelp(this));
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
