package org.fotum.app;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.commands.moderation.ClearCommand;
import org.fotum.app.commands.owner.EvalCommand;
import org.fotum.app.commands.siege.*;
import org.fotum.app.commands.siege.settings.*;
import org.fotum.app.objects.ICommand;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager
{
	private final Map<String, ICommand> commands = new HashMap<String, ICommand>();
	
	public CommandManager()
	{
		// Siege managing commands
		this.addCommand(new AddSiegeCommand());
		this.addCommand(new RemoveSiegeCommand());
		this.addCommand(new SetListeningChannel());
		this.addCommand(new RemoveListeningChannel());
		this.addCommand(new SetManagingRoles());
		this.addCommand(new RemoveManagingRoles());
		this.addCommand(new SetPrefixRoles());
		this.addCommand(new SetMentionRoles());
		this.addCommand(new RemoveMentionRoles());
		this.addCommand(new AutoregCommand());
		this.addCommand(new UpdateSiegeSettingsCommand());

		// Players commands to add/remove themselves from list
		this.addCommand(new AddPlayerCommand());
		this.addCommand(new RemovePlayerCommand());

		// Managing commands to force add/remove players from list
		this.addCommand(new ForceAddPlayersCommand());
		this.addCommand(new ForceRemPlayersCommand());

		// General purpose commands
		this.addCommand(new SiegeHelp(this));
		// Moderation commands
		this.addCommand(new ClearCommand());
		// Owner commands
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
			final List<String> args = new ArrayList<String>(Arrays.asList(split).subList(1, split.length));
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
