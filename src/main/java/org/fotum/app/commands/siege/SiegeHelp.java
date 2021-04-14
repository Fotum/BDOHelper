package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.CommandManager;
import org.fotum.app.Constants;
import org.fotum.app.objects.EmbedCreator;
import org.fotum.app.objects.ICommand;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SiegeHelp implements ICommand
{
	private CommandManager manager;
	
	public SiegeHelp(CommandManager manager)
	{
		this.manager = manager;
	}

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		List<ICommand> available = manager.getCommands()
				.stream()
				.filter(
					(command) -> {
						String cmd = command.getInvoke();
						return !cmd.equalsIgnoreCase("+") &&
								!cmd.equalsIgnoreCase("-") &&
								!cmd.equalsIgnoreCase("eval") &&
								!cmd.isEmpty();
				})
				.collect(Collectors.toList());
		
		if (args.isEmpty())
		{
			this.generateAndSendEmbed(event, available);
			return;
		}
		
		String joined = String.join("", args);
		ICommand command = available.contains(manager.getCommand(joined)) ? manager.getCommand(joined) : null;

		if (command == null)
		{
			event.getChannel().sendMessage("The command `" + joined + "` does not exist\n" + 
					"Use `" + Constants.PREFIX + this.getInvoke() + "` for a list of commands").queue();
			return;
		}
		
		String message = "Command help for `" + command.getInvoke() + "`\n" + command.getHelp();
		event.getChannel().sendMessage(message).queue();
	}

	@Override
	public String getHelp()
	{
		return "Shows a list of all the commands.\n" +
				"Usage: `" + Constants.PREFIX + this.getInvoke() + " [command]`";
	}

	@Override
	public String getInvoke()
	{
		return "help";
	}

	private void generateAndSendEmbed(GuildMessageReceivedEvent event, List<ICommand> visible)
	{
		EmbedBuilder builder = EmbedCreator.getDefault().setTitle("A list of siege manager commands:");
		
		StringBuilder descriptionBuilder = builder.getDescriptionBuilder();
		visible.stream()
			.sorted(Comparator.comparing(ICommand::getInvoke))
			.forEach(
				(command) -> descriptionBuilder.append("`").append(Constants.PREFIX + command.getInvoke()).append("`\n")
			);

		event.getChannel().sendMessage(builder.build()).queue();
		builder.clear();
	}
}
