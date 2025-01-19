package org.fotum.app.commands.bdo.buttons;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.Constants;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.commands.interfaces.IButtonCommand;
import org.fotum.app.modules.bdo.siege.SiegeInstance;

@Slf4j
public class AddPlayerCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null)
            return;

        long guildId = event.getGuild().getIdLong();
        long msgId = event.getMessageIdLong();
        TextChannel channel = event.getChannel().asTextChannel();

        SiegeInstance instance = GuildManager.getInstance().getGuildHandler(guildId)
                .getInstances().stream()
                .filter((inst) -> inst.getAnnounceMsgId() == msgId)
                .findFirst()
                .orElse(null);

        if (instance != null) {
            Member member = event.getMember();
            if (member != null)
                instance.registerPlayer(member.getIdLong());

            log.info(String.format("(%s) {%s} [%s] was clicked by <%#s> for siege with date %s",
                        guild.getName(),
                        channel.getName(),
                        event.getComponentId(),
                        event.getUser(),
                        instance.getSiegeDt().format(Constants.DATE_FORMAT)
                    )
            );
        }
    }

    @Override
    public String getCommandId() {
        return "button-plus";
    }
}
