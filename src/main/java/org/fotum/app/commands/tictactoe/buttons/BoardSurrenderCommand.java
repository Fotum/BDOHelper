package org.fotum.app.commands.tictactoe.buttons;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.interfaces.IButtonCommand;
import org.fotum.app.modules.tictactoe.TicTacToeGame;
import org.fotum.app.modules.tictactoe.TicTacToeState;

import java.util.function.Predicate;

public class BoardSurrenderCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long msgId = event.getMessageIdLong();
        long memberId = event.getMember().getIdLong();

        Predicate<TicTacToeGame> searchCondition = (game) ->
                game.getGameMsgId() == msgId
                && game.getState() == TicTacToeState.ACTIVE
                && (game.getFirstPlayerId() == memberId
                || game.getSecondPlayerId() == memberId);

        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        TicTacToeGame gameInstance = handler
                .getTicTacToeGames()
                .stream()
                .filter(searchCondition)
                .findFirst()
                .orElse(null);

        if (gameInstance == null)
            return;

        gameInstance.surrenderGame(memberId);
        MessageCreateData msgData = gameInstance.getBoardRepresentation();
        handler.getTicTacToeGames().remove(gameInstance);

        Message message = event.getMessage();
        message.editMessage(MessageEditData.fromCreateData(msgData)).queue();
    }

    @Override
    public String getCommandId() {
        return "tictactoe-board-surrender";
    }
}
