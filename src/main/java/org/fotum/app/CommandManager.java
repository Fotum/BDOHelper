package org.fotum.app;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fotum.app.commands.buttons.AddPlayerCommand;
import org.fotum.app.commands.buttons.RemovePlayerCommand;
import org.fotum.app.commands.moderation.ClearCommand;
import org.fotum.app.commands.owner.*;
import org.fotum.app.commands.siege.AddSiegeCommand;
import org.fotum.app.commands.siege.ForceAddPlayersCommand;
import org.fotum.app.commands.siege.ForceRemPlayersCommand;
import org.fotum.app.commands.siege.RemoveSiegeCommand;
import org.fotum.app.commands.siege.settings.AutoregCommand;
import org.fotum.app.commands.siege.settings.SetupCommand;
import org.fotum.app.commands.siege.settings.UpdateSiegeSettingsCommand;
import org.fotum.app.interfaces.IButtonCommand;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.interfaces.ITextCommand;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager
{
	private final Map<String, ITextCommand> textCommands = new HashMap<>();
	private final Map<String, IButtonCommand> buttonCommands = new HashMap<>();
	private final Map<String, ISlashCommand> slashCommands = new HashMap<>();
	
	public CommandManager()
	{
		// Siege managing commands
		this.addSlashCommand(new AddSiegeCommand());
		this.addSlashCommand(new RemoveSiegeCommand());
		this.addSlashCommand(new SetupCommand());
		this.addSlashCommand(new AutoregCommand());
		this.addSlashCommand(new UpdateSiegeSettingsCommand());

		// Managing commands to force add/remove players from list
		this.addSlashCommand(new ForceAddPlayersCommand());
		this.addSlashCommand(new ForceRemPlayersCommand());

		// General purpose commands
		// Moderation commands
		this.addSlashCommand(new ClearCommand());
		// Owner commands
		this.addTextCommand(new EvalCommand());
		this.addTextCommand(new ChangeActivityCommand());
		this.addTextCommand(new ShutdownCommand());

		// Button commands
		this.addButtonCommand(new AddPlayerCommand());
		this.addButtonCommand(new RemovePlayerCommand());
	}
	
	public Collection<ITextCommand> getTextCommands()
	{
		return this.textCommands.values();
	}
	
	public ITextCommand getCommand(String name)
	{
		return this.textCommands.get(name);
	}
	
	void handleTextCommand(MessageReceivedEvent event)
	{
		User author = event.getAuthor();
		String content = event.getMessage().getContentRaw();

		if (!author.isBot() && !event.getMessage().isWebhookMessage() && content.startsWith(Constants.PREFIX))
		{
			final String[] split = content.replaceFirst("(?i)" + Pattern.quote(Constants.PREFIX), "").split("\\s+");
			final String invoke = split[0].toLowerCase();

			ITextCommand cmd = this.textCommands.get(invoke);
			if (Objects.nonNull(cmd))
			{
				ChannelType channelType = event.getChannelType();
				if (channelType != ChannelType.PRIVATE && !cmd.canBePrivate())
					return;

				final List<String> args = new ArrayList<>(Arrays.asList(split).subList(1, split.length));
				cmd.handle(args, event);
			}
		}
	}
	
	void handleButtonCommand(ButtonInteractionEvent event)
	{
		String componentId = event.getComponentId();
		if (this.buttonCommands.containsKey(componentId))
			this.buttonCommands.get(componentId).handle(event);
	}

	void handleSlashCommand(SlashCommandInteractionEvent event)
	{
		String commandNm = event.getName();
		if (this.slashCommands.containsKey(commandNm))
			this.slashCommands.get(commandNm).handle(event);
	}

	private void addTextCommand(ITextCommand command)
	{
		if (!this.textCommands.containsKey(command.getInvoke()))
		{
			this.textCommands.put(command.getInvoke(), command);
		}
	}

	private void addButtonCommand(IButtonCommand command)
	{
		if (!this.buttonCommands.containsKey(command.getCommandId()))
		{
			this.buttonCommands.put(command.getCommandId(), command);
		}
	}

	private void addSlashCommand(ISlashCommand command)
	{
		if (!this.slashCommands.containsKey(command.getInvoke()))
		{
			this.slashCommands.put(command.getInvoke(), command);
		}
	}
}
