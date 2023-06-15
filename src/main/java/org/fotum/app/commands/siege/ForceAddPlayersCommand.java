package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.utils.BotUtils;

import java.time.LocalDate;

public class ForceAddPlayersCommand implements ISlashCommand
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

		String strSiegeDt = event.getOption("siege_dt").getAsString();
		LocalDate siegeDt = BotUtils.convertStrToDate(strSiegeDt);
		if (siegeDt == null)
		{
			event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
			return;
		}

		SiegeInstance siegeInst = GuildManager.getInstance().getGuildSiegeInstance(guild.getIdLong(), siegeDt);
		if (siegeInst == null)
		{
			event.getHook().sendMessage(String.format("Active siege announcement is not found for date `%s`", strSiegeDt)).queue();
			return;
		}

		event.getOption("players").getMentions().getMembers().forEach(
				(member) -> siegeInst.registerPlayer(member.getIdLong())
		);

		event.getHook().sendMessage("Player(s) successfully added").queue();
	}

	@Override
	public String getInvoke()
	{
		return "forceadd";
	}
}
