package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.siege.GuildSettings;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.interfaces.ISlashCommand;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

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

        SiegeInstance inst = GuildManager.getInstance().getSiegeInstance(guild.getIdLong());
        if (inst == null)
        {
            event.getHook().sendMessage("Active siege announcement is not found").queue();
            return;
        }

        List<OptionMapping> opts = event.getOptions();
        switch (opts.get(0).getAsString().toLowerCase(Locale.ROOT))
        {
            case ("date"):
                this.updateDate(opts.get(1).getAsString(), inst, event);
                break;

            case ("zone"):
                this.updateZone(opts.get(1).getAsString(), inst, event);
                break;

            case ("maxplrs"):
                this.updateMaxPlayers(opts.get(1).getAsString(), inst, event);
                break;

            case ("desc"):
                this.updateSiegeDesc(opts.get(1).getAsString(), inst, event);
                break;

            default:
                event.getHook().sendMessage("Incorrect parameters given").queue();
                break;
        }
    }

    @Override
    public String getInvoke()
    {
        return "updsiege";
    }

    private void updateDate(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        LocalDate startDt;
        try
        {
            startDt = LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        catch (DateTimeParseException ex)
        {
            event.getHook().sendMessage("Incorrect date format given, expected format is `dd.mm.yyyy`").queue();
            return;
        }

        if (startDt.isBefore(LocalDate.now()))
        {
            event.getHook().sendMessage("Can only change siege date on a future date").queue();
            return;
        }

        inst.setStartDt(startDt);
        inst.updateTitle();
        event.getHook().sendMessage("Siege date successfully updated").queue();
    }

    private void updateZone(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        String zone = Constants.ZONES.get(val.toLowerCase());
        if (zone == null)
        {
            String correctZones = String.join(", ", Constants.ZONES.keySet());
            event.getHook().sendMessage("Incorrect zone identifier given, expected one of the following: `[" + correctZones + "]`").queue();
            return;
        }

        inst.setZone(zone);
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

        inst.setPlayersMax(playersAmount);
        inst.rearrangePlayers();
        event.getHook().sendMessage("Siege maximum players successfully updated").queue();
    }

    private void updateSiegeDesc(String val, SiegeInstance inst, SlashCommandInteractionEvent event)
    {
        inst.setDescriptionMessage(val);
        event.getHook().sendMessage("Siege description successfully updated").queue();
    }
}
