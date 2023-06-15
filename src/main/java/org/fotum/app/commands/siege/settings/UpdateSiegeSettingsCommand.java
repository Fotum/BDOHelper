package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.utils.BotUtils;

import java.time.LocalDate;

public class UpdateSiegeSettingsCommand implements ISlashCommand
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

        SiegeInstance inst = GuildManager.getInstance().getGuildSiegeInstance(guild.getIdLong(), siegeDt);
        if (inst == null)
        {
            event.getHook().sendMessage(String.format("Active siege announcement is not found for date `%s`", strSiegeDt)).queue();
            return;
        }

        String fieldNm = event.getOption("field_nm").getAsString();
        String fieldVal = event.getOption("field_val").getAsString();
        switch (fieldNm)
        {
            case ("date") -> this.updateDate(fieldVal, inst, event);
            case ("zone") -> this.updateZone(fieldVal, inst, event);
            case ("maxplrs") -> this.updateMaxPlayers(fieldVal, inst, event);
            default -> event.getHook().sendMessage("Incorrect parameters given").queue();
        }
    }

    @Override
    public String getInvoke()
    {
        return "updsiege";
    }

    private void updateDate(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        LocalDate startDt = BotUtils.convertStrToDate(val);
        if (startDt == null)
        {
            event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
            return;
        }

        if (startDt.isBefore(LocalDate.now()))
        {
            event.getHook().sendMessage("Can only change siege date on a future date").queue();
            return;
        }

        SiegeInstance instance = GuildManager.getInstance().getGuildSiegeInstance(event.getGuild().getIdLong(), startDt);
        if (instance != null)
        {
            event.getHook().sendMessage(String.format("Siege on `%s` date is already exist", val)).queue();
            return;
        }

        inst.setStartDt(startDt);
        inst.updateTitle();
        event.getHook().sendMessage("Siege date successfully updated").queue();
    }

    private void updateZone(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        inst.setZone(val);
        inst.updateTitle();
        event.getHook().sendMessage("Siege zone successfully updated").queue();
    }

    private void updateMaxPlayers(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        int playersAmount;
        try
        {
            playersAmount = Integer.parseInt(val);
        }
        catch (NumberFormatException ex)
        {
            event.getHook().sendMessage("Incorrect number of players given").queue();
            return;
        }

        inst.updatePlayersMax(playersAmount);
        event.getHook().sendMessage("Siege maximum players successfully updated").queue();
    }
}
