package org.fotum.app;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.fotum.app.utils.BotUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
class Listener extends ListenerAdapter
{
	private final CommandManager manager;

	Listener(CommandManager manager)
	{
		this.manager = manager;
	}
	
	@Override
	public void onReady(@NotNull ReadyEvent event)
	{
		log.info(String.format("Logged in as %#s", event.getJDA().getSelfUser()));
		BotUtils.runStartupSequence();
	}
	
	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event)
	{
		User author = event.getAuthor();
		String content = event.getMessage().getContentDisplay();

		// If event came from guild's text channel - log with additional info
		if (event.isFromType(ChannelType.TEXT))
		{
			Guild guild = event.getGuild();
			TextChannel textChannel = event.getChannel().asTextChannel();

			log.info(String.format("(%s) [%s] <%#s>: %s", guild.getName(), textChannel.getName(), author, content));
		}
		// If event came from private text channel - log author and message
		else if (event.isFromType(ChannelType.PRIVATE))
		{
			log.info(String.format("[PRIV] <%#s>: %s", author, content));
		}
		// Handle command
		this.manager.handleTextCommand(event);
	}

	@Override
	public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event)
	{
		if (event.isFromGuild())
		{
			Guild guild = event.getGuild();
			log.info(String.format("(%s) [%s] command was used by <%#s> with options:\n%s",
					Objects.nonNull(guild) ? guild.getName() : "Unknown",
					event.getName(),
					event.getUser(),
					event.getOptions().stream()
							.map((opt) -> opt.getName() + ": '" + opt.getAsString() + "'")
							.collect(Collectors.joining("\n")))
			);

			this.manager.handleSlashCommand(event);
		}
	}

	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event)
	{
		event.deferEdit().queue();

		if (event.isFromGuild())
		{
			Guild guild = event.getGuild();

			log.info(String.format("(%s) [%s] was clicked by <%#s>",
					Objects.nonNull(guild) ? guild.getName() : "Unknown",
					event.getComponentId(),
					event.getUser())
			);

			this.manager.handleButtonCommand(event);
		}
	}
}
