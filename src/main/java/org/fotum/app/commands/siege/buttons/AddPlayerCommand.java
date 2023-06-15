package org.fotum.app.commands.siege.buttons;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.IButtonCommand;

import java.util.Objects;

@Slf4j
public class AddPlayerCommand implements IButtonCommand
{
	@Override
	public void handle(ButtonInteractionEvent event)
	{
		GuildManager manager = GuildManager.getInstance();

		Guild guild = event.getGuild();
		if (Objects.isNull(guild))
			return;

		long guildId = event.getGuild().getIdLong();
		long msgId = event.getMessageIdLong();

		SiegeInstance instance = manager.getGuildSiegeInstances(guildId)
				.stream()
				.filter((inst) -> inst.getSiegeAnnounceMsgId() == msgId)
				.findFirst()
				.orElse(null);

		if (instance != null)
		{
			Member member = event.getMember();
			if (member != null)
				instance.registerPlayer(member.getIdLong());

			log.info(String.format("(%s) [%s] was clicked by <%#s> for siege with date %s",
					guild.getName(),
					event.getComponentId(),
					event.getUser(),
					instance.getStartDt())
			);
		}
	}

	@Override
	public String getCommandId()
	{
		return "button-plus";
	}
}
