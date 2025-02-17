package org.fotum.app.commands.bdo;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.commands.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

public class ForceRemUserCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();

        long toRemove = event.getOption("discord_user").getAsUser().getIdLong();
        GuildMemberInfo memberInfo = settings.getRegisteredMembers().computeIfAbsent(toRemove, GuildMemberInfo::new);
        memberInfo.setBdoName(null);

        event.getHook().sendMessage("Player's BDO info successfully removed").queue();
    }

    @Override
    public String getInvoke() {
        return "forceremuser";
    }
}
