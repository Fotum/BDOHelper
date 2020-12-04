package org.fotum.app;

import org.fotum.app.objects.BotUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

class Listener extends ListenerAdapter
{
	private final CommandManager manager;
	private final Logger logger = LoggerFactory.getLogger(Listener.class);
	
	Listener(CommandManager manager)
	{
		this.manager = manager;
	}
	
	@Override
	public void onReady(ReadyEvent event)
	{
		logger.info(String.format("Logged in as %#s", event.getJDA().getSelfUser()));
		
		logger.info("Loading configs");
		BotUtils.loadConfigs(event.getJDA());
		logger.info("Configs successfully loaded");
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
			
			logger.info(String.format("(%s) [%s] <%#s>: %s", guild.getName(), textChannel.getName(), author, content));
		}
		else if (event.isFromType(ChannelType.PRIVATE))
		{
			logger.info(String.format("[PRIV] <%#s>: %s", author, content));
		}
	}
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event)
	{
		if (event.getAuthor().isBot() || event.getMessage().isWebhookMessage())
			return;
		
		String rw = event.getMessage().getContentRaw();
		
		if (rw.equalsIgnoreCase(Constants.PREFIX + "helperoff") &&
				event.getAuthor().getIdLong() == Constants.OWNER)
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
		BotUtils.saveConfigs();
		jda.shutdown();
		System.exit(0);
	}
}
