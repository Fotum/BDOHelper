package org.fotum.app.commands.buttons;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.interfaces.IButtonCommand;

import java.util.Objects;

public class RemovePlayerCommand implements IButtonCommand
{
	@Override
	public void handle(ButtonInteractionEvent event)
	{
		GuildManager manager = GuildManager.getInstance();

		Guild guild = event.getGuild();
		if (Objects.isNull(guild))
			return;

		long guildId = event.getGuild().getIdLong();

		if (Objects.nonNull(manager.getSiegeInstance(guildId)))
		{
			Member member = event.getMember();
			if (Objects.nonNull(member))
				manager.getSiegeInstance(guildId).removePlayer(member.getIdLong());
		}
	}

	@Override
	public String getCommandId()
	{
		return "button-minus";
	}
}
