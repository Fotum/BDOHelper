package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.util.List;

public class RemoveSiegeCommand implements ICommand
{

	@Override
	public void handle(List<String> args, GuildMessageReceivedEvent event)
	{
		if (!PermissionChecker.checkGeneralPermissions(event))
			return;

		GuildManager.getInstance().removeSiegeInstance(event.getGuild().getIdLong());
		BotUtils.sendMessageToChannel(event.getChannel(), "Siege successfully deleted");
	}

	@Override
	public String getHelp()
	{
		return "Unschedules a siege info\n"
				+ "Usage: `" + Constants.PREFIX + this.getInvoke();
	}

	@Override
	public String getInvoke()
	{
		return "remsiege";
	}
}
