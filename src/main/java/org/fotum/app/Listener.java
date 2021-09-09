package org.fotum.app;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.fotum.app.utils.BotUtils;

@Slf4j
class Listener extends ListenerAdapter
{
	private final CommandManager manager;
	
	Listener(CommandManager manager)
	{
		this.manager = manager;
	}
	
	@Override
	public void onReady(ReadyEvent event)
	{
		log.info(String.format("Logged in as %#s", event.getJDA().getSelfUser()));
		BotUtils.runStartupSequence(event.getJDA());
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		User author = event.getAuthor();
		String content = event.getMessage().getContentDisplay();
		
		if (event.isFromType(ChannelType.TEXT))
		{
			Guild guild = event.getGuild();
			TextChannel textChannel = event.getTextChannel();
			
			log.info(String.format("(%s) [%s] <%#s>: %s", guild.getName(), textChannel.getName(), author, content));
		}
		else if (event.isFromType(ChannelType.PRIVATE))
		{
			log.info(String.format("[PRIV] <%#s>: %s", author, content));
		}
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event)
	{
		if (event.getAuthor().isBot() || event.getMessage().isWebhookMessage())
			return;
		
		String rw = event.getMessage().getContentRaw();
		
		if (rw.equalsIgnoreCase(Constants.PREFIX + "helperoff")
			&& event.getAuthor().getIdLong() == Constants.OWNER)
		{
			this.shutdown(event.getJDA());
			return;
		}
		
		if (rw.startsWith(Constants.PREFIX))
		{
			manager.handleCommand(event);
		}
		else if (rw.startsWith("+") || rw.startsWith("-"))
		{
			manager.handleAudCommand(event, String.valueOf(rw.charAt(0)));
		}
	}
	
	private void shutdown(JDA jda)
	{
		log.info("Manual shutdown initiated...");
		BotUtils.runShutdownSequence();
		log.info("Shutdown sequence successfully finished, shutting down JDA");
		jda.shutdown();
		System.exit(0);
	}
}
