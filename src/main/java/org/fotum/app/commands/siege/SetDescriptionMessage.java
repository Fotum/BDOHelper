package org.fotum.app.commands.siege;

import java.util.List;

import org.fotum.app.objects.ICommand;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class SetDescriptionMessage implements ICommand
{
	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		
	}

	@Override
	public String getHelp()
	{
		return null;
	}

	@Override
	public String getInvoke()
	{
		return "setdesc";
	}

}
