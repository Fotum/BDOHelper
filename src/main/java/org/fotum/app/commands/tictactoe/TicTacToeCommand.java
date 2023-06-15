package org.fotum.app.commands.tictactoe;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.tictactoe.TicTacToeGame;
import org.fotum.app.interfaces.ISlashCommand;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TicTacToeCommand implements ISlashCommand
{

    @Override
    public void handle(SlashCommandInteractionEvent event)
    {
        event.deferReply(true).queue();

        User caller = event.getUser();
        Guild guild = event.getGuild();
        if (Objects.isNull(guild))
            return;

        User target = event.getOption("target").getAsUser();
        if (target.isBot() || caller.equals(target))
        {
            event.getHook().sendMessage("Target player cannot be bot or yourself").queue();
            return;
        }

        // Check if caller/target user is not already participating in game
        long guildId = guild.getIdLong();
        long callerUserId = caller.getIdLong();
        long targetUserId = target.getIdLong();
        List<TicTacToeGame> activeGames = GuildManager.getInstance()
                .getTicTacToeGames()
                .stream()
                .filter((game) -> game.getGuildId() == guildId && game.getState() != 2)
                .collect(Collectors.toList());

        for (TicTacToeGame game : activeGames)
        {
            long userOneId = game.getUserOneId();
            long userTwoId = game.getUserTwoId();

            if (callerUserId == userOneId || callerUserId == userTwoId)
            {
                event.getHook().sendMessage("You are already participating in active game").queue();
                return;
            }
            else if (targetUserId == userOneId || targetUserId == userTwoId)
            {
                event.getHook().sendMessage("Target user is already participating in active game").queue();
                return;
            }
        }

        int fieldSize = 3;
        OptionMapping sizeOption = event.getOption("size");
        if (Objects.nonNull(sizeOption))
            fieldSize = sizeOption.getAsInt();

        TicTacToeGame gameInstance = new TicTacToeGame(guildId, event.getChannel().getIdLong(), callerUserId, targetUserId, fieldSize);
        GuildManager.getInstance().addTicTacToeGame(gameInstance);
        gameInstance.sendGameRequestMessage();

        event.getHook().deleteOriginal().queue();
    }

    @Override
    public String getInvoke()
    {
        return "tictactoe";
    }
}
