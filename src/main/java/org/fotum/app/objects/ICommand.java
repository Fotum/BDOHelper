package org.fotum.app.objects;

import java.util.List;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface ICommand
{
	void handle(List<String> args, GuildMessageReceivedEvent event);
	
	String getHelp();
	
	String getInvoke();
}
