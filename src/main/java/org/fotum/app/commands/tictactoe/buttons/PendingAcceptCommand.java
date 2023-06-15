package org.fotum.app.commands.tictactoe.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.features.siege.GuildManager;
import org.fotum.app.features.tictactoe.TicTacToeGame;
import org.fotum.app.interfaces.IButtonCommand;

import java.util.Objects;
import java.util.function.Predicate;

public class PendingAcceptCommand implements IButtonCommand
{
    @Override
    public void handle(ButtonInteractionEvent event)
    {
        long msgId = event.getMessageIdLong();
        long memberId = event.getMember().getIdLong();

        Predicate<TicTacToeGame> searchCondition = (game) ->
                game.getGameRequestMsgId() == msgId
                && game.getUserTwoId() == memberId
                && game.getState() == 0
        ;

        // Get game instance that is in current guild and assigned to current request message
        TicTacToeGame gameInstance = GuildManager.getInstance()
                .getTicTacToeGames()
                .stream()
                .filter(searchCondition)
                .findFirst()
                .orElse(null);

        if (Objects.isNull(gameInstance))
            return;

        // Here second member accepts
        gameInstance.startAcceptedGame();
    }

    @Override
    public String getCommandId()
    {
        return "tictactoe-pend-accept";
    }
}
