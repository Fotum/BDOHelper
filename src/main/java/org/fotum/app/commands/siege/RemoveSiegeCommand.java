package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.utils.BotUtils;

import java.time.LocalDate;

public class RemoveSiegeCommand implements ISlashCommand
{

	@Override
	public void handle(SlashCommandInteractionEvent event)
	{
		event.deferReply(true).queue();

		Guild guild = event.getGuild();
		GuildManager manager = GuildManager.getInstance();
		GuildSettings settings = manager.getGuildSettings(guild.getIdLong());

		if (settings == null)
		{
			event.getHook().sendMessage("No siege settings configured for this guild").queue();
			return;
		}

		OptionMapping siegeDtOpt = event.getOption("siege_dt");
		LocalDate siegeDt = null;
		if (siegeDtOpt != null)
		{
			String strSiegeDt = siegeDtOpt.getAsString().trim();
			siegeDt = BotUtils.convertStrToDate(strSiegeDt);

			if (siegeDt == null)
			{
				event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
				return;
			}
		}

		if (siegeDt != null)
		{
			GuildManager.getInstance().removeSiegeInstance(guild.getIdLong(), siegeDt);
			event.getHook().sendMessage(String.format("Siege with date `%s` successfully deleted", siegeDtOpt.getAsString().trim())).queue();
		}
		else
		{
			GuildManager.getInstance().removeAllGuildSiegeInstances(guild.getIdLong());
			event.getHook().sendMessage("All sieges successfully deleted").queue();
		}
	}

	@Override
	public String getInvoke()
	{
		return "remsiege";
	}
}
