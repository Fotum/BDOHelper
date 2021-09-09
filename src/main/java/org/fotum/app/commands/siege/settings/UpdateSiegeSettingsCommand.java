package org.fotum.app.commands.siege.settings;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.fotum.app.Constants;
import org.fotum.app.features.siege.SiegeInstance;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.utils.BotUtils;
import org.fotum.app.objects.ICommand;
import org.fotum.app.objects.checkers.PermissionChecker;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public class UpdateSiegeSettingsCommand implements ICommand
{
    @Override
    public void handle(List<String> args, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        if (!PermissionChecker.checkGeneralPermissions(event))
            return;

        SiegeInstance inst = GuildManager.getInstance().getSiegeInstance(event.getGuild().getIdLong());
        if (inst == null)
        {
            BotUtils.sendMessageToChannel(channel, "Active siege announcement is not found");
            return;
        }

        if (args.isEmpty())
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect number of arguments given");
            return;
        }

        switch (args.remove(0).toLowerCase())
        {
            case ("date"):
                this.updateDate(args.get(0), inst, event);
                break;

            case ("zone"):
                this.updateZone(args.get(0), inst, event);
                break;

            case ("maxplrs"):
                this.updateMaxPlayers(args.get(0), inst, event);
                break;

            case ("desc"):
                this.updateSiegeDesc(args, inst, event);
                break;

            default:
                BotUtils.sendMessageToChannel(event.getChannel(), "Incorrect parameters given");
                break;
        }
    }

    private void updateDate(String val, SiegeInstance inst, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        LocalDate startDt;
        try
        {
            startDt = LocalDate.parse(val, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }
        catch (DateTimeParseException ex)
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect date format given, expected format is `dd.mm.yyyy`");
            return;
        }

        if (startDt.isBefore(LocalDate.now()))
        {
            BotUtils.sendMessageToChannel(channel, "Can only change siege date on a future date");
            return;
        }

        inst.setStartDt(startDt);
        inst.updateTitle();
        BotUtils.sendMessageToChannel(channel, "Siege date successfully updated");
    }

    private void updateZone(String val, SiegeInstance inst, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        String zone = Constants.ZONES.get(val.toLowerCase());
        if (zone == null)
        {
            String correctZones = String.join(", ", Constants.ZONES.keySet());
            BotUtils.sendMessageToChannel(channel, "Incorrect zone identifier given, expected one of the following: `[" + correctZones + "]`");
            return;
        }

        inst.setZone(zone);
        inst.updateTitle();
        BotUtils.sendMessageToChannel(channel, "Siege zone successfully updated");
    }

    private void updateMaxPlayers(String val, SiegeInstance inst, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        int playersAmount;
        try
        {
            playersAmount = Integer.parseInt(val);
        }
        catch (NumberFormatException ex)
        {
            BotUtils.sendMessageToChannel(channel, "Incorrect number of players given");
            return;
        }

        inst.setPlayersMax(playersAmount);
        inst.rearrangePlayers();
        BotUtils.sendMessageToChannel(channel, "Siege maximum players successfully updated");
    }

    private void updateSiegeDesc(List<String> val, SiegeInstance inst, GuildMessageReceivedEvent event)
    {
        TextChannel channel = event.getChannel();

        inst.setDescriptionMessage(String.join(" ", val));
        BotUtils.sendMessageToChannel(channel, "Siege description successfully updated");
    }

    @Override
    public String getHelp()
    {
        return "Updates settings of currently active siege\n" +
                "Usage: `" + this.getInvoke() + " [date/zone/maxplrs/desc] <new_value>`";
    }

    @Override
    public String getInvoke()
    {
        return "updsiege";
    }
}
