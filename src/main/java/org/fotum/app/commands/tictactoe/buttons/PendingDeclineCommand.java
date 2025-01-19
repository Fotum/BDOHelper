package org.fotum.app.commands.tictactoe.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.commands.interfaces.IButtonCommand;
import org.fotum.app.modules.tictactoe.TicTacToeGame;
import org.fotum.app.modules.tictactoe.TicTacToeState;

import java.util.function.Predicate;

public class PendingDeclineCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {
        long guildId = event.getGuild().getIdLong();
        long msgId = event.getMessageIdLong();
        long memberId = event.getMember().getIdLong();

        Predicate<TicTacToeGame> searchCondition = (game) ->
                game.getGameRequestMsgId() == msgId
                        && (game.getSecondPlayerId() == memberId || game.getFirstPlayerId() == memberId)
                        && game.getState() == TicTacToeState.PENDING;

        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        TicTacToeGame gameInstance = handler
                .getTicTacToeGames()
                .stream()
                .filter(searchCondition)
                .findFirst()
                .orElse(null);

        if (gameInstance == null)
            return;

        long channelId = event.getMessageChannel().getIdLong();
        DiscordObjectsOperations.deleteMessageById(channelId, msgId);
        handler.getTicTacToeGames().remove(gameInstance);
    }

    @Override
    public String getCommandId() {
        return "tictactoe-pend-decline";
    }
}
