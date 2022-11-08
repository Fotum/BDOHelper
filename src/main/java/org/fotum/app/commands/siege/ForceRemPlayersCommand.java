package org.fotum.app.commands.siege;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;

import java.util.List;

public class ForceRemPlayersCommand implements ISlashCommand
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
		
		SiegeInstance siegeInst = GuildManager.getInstance().getSiegeInstance(guild.getIdLong());
		if (siegeInst == null)
		{
			event.getHook().sendMessage("Active siege is not found for current guild").queue();
			return;
		}

		List<OptionMapping> opts = event.getOptions();
		opts.get(0).getMentions().getMembers().forEach(
				(member) -> siegeInst.removePlayer(member.getIdLong())
		);

		event.getHook().sendMessage("Player(s) successfully removed").queue();
	}

	@Override
	public String getInvoke()
	{
		return "forcerem";
	}
}
