package org.fotum.app.commands.bdo;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

public class RegUserCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();

        long userId = event.getUser().getIdLong();
        String nameStr = event.getOption("bdo_name").getAsString();

        GuildMemberInfo memberInfo = settings.getRegisteredMembers().computeIfAbsent(userId, GuildMemberInfo::new);
        memberInfo.setBdoName(nameStr);

        event.getHook().sendMessage("Player's BDO info successfully updated").queue();
    }

    @Override
    public String getInvoke() {
        return "reguser";
    }
}
