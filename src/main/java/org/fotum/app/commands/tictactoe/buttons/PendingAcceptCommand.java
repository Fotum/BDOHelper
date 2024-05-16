package org.fotum.app.commands.tictactoe.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.interfaces.IButtonCommand;
import org.fotum.app.modules.tictactoe.TicTacToeGame;
import org.fotum.app.modules.tictactoe.TicTacToeState;

import java.util.function.Predicate;

public class PendingAcceptCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long msgId = event.getMessageIdLong();
        long memberId = event.getMember().getIdLong();

        Predicate<TicTacToeGame> searchCondition = (game) ->
                game.getGameRequestMsgId() == msgId
                && game.getSecondPlayerId() == memberId
                && game.getState() == TicTacToeState.PENDING;

        TicTacToeGame gameInstance = GuildManager.getInstance().getGuildHandler(guildId)
                .getTicTacToeGames()
                .stream()
                .filter(searchCondition)
                .findFirst()
                .orElse(null);

        if (gameInstance == null)
            return;

        long channelId = event.getMessageChannel().getIdLong();
        DiscordObjectsOperations.deleteMessageById(channelId, msgId);

        gameInstance.initializeGame();
        MessageCreateData msgData = gameInstance.getBoardRepresentation();
        event.getMessageChannel().sendMessage(msgData).queue((msg) -> gameInstance.setGameMsgId(msg.getIdLong()));
    }

    @Override
    public String getCommandId() {
        return "tictactoe-pend-accept";
    }
}
