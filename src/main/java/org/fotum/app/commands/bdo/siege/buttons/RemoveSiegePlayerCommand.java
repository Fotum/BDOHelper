package org.fotum.app.commands.bdo.siege.buttons;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.IButtonCommand;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

@Slf4j
public class RemoveSiegePlayerCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;

        long guildId = event.getGuild().getIdLong();
        long msgId = event.getMessageIdLong();

        SiegeInstance instance = GuildManager.getInstance().getGuildHandler(guildId)
                .getSiegeInstances().stream()
                .filter((inst) -> inst.getSiegeAnnounceMsgId() == msgId)
                .findFirst()
                .orElse(null);

        if (instance != null) {
            Member member = event.getMember();
            if (member != null)
                instance.unregisterPlayer(member.getIdLong());

            log.info(String.format("(%s) [%s] was clicked by <%#s> for siege with date %s",
                    guild.getName(),
                    event.getComponentId(),
                    event.getUser(),
                    instance.getSiegeDt().format(Constants.DATE_FORMAT))
            );
        }
    }

    @Override
    public String getCommandId() {
        return "siege-button-minus";
    }
}
