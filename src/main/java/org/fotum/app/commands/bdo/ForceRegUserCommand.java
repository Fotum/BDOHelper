package org.fotum.app.commands.bdo;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.bdo.BDOClass;
import org.fotum.app.modules.bdo.GuildMemberInfo;
import org.fotum.app.modules.bdo.siege.SiegeSettings;

public class ForceRegUserCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        long guildId = event.getGuild().getIdLong();
        SiegeSettings settings = GuildManager.getInstance().getGuildHandler(guildId).getSiegeSettings();

        long toAdd = event.getOption("discord_user").getAsUser().getIdLong();
        String nameStr = event.getOption("bdo_name").getAsString();

        GuildMemberInfo memberInfo = settings.getRegisteredMembers().computeIfAbsent(toAdd, GuildMemberInfo::new);
        memberInfo.setBdoName(nameStr);

        OptionMapping bdoClassMapping = event.getOption("bdo_class");
        if (bdoClassMapping != null) {
            memberInfo.setBdoClass(BDOClass.valueOf(bdoClassMapping.getAsString()));
        }

        OptionMapping priorityMapping = event.getOption("priority");
        if (priorityMapping != null) {
            memberInfo.setPriority(priorityMapping.getAsInt());
        }

        event.getHook().sendMessage("Player's BDO info successfully updated").queue();
    }

    @Override
    public String getInvoke() {
        return "forcereguser";
    }
}
