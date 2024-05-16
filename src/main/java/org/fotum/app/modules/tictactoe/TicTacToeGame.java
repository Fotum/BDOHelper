package org.fotum.app.modules.tictactoe;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.fotum.app.handlers.DiscordObjectsOperations;

import java.util.*;

public class TicTacToeGame {
    private final int size = 3;
//    private final String boardFiller = "•";
    private final String boardFiller = EmbedBuilder.ZERO_WIDTH_SPACE;
    private final String crossSymbol = "❌";
    private final String circleSymbol = "⭕";

    @Getter
    private final long guildId;
    @Getter
    private final long firstPlayerId;
    @Getter
    private final long secondPlayerId;
    private final Queue<Integer> crossQueue = new LinkedList<>();
    private final Queue<Integer> circleQueue = new LinkedList<>();

    @Getter @Setter
    private long gameRequestMsgId = 0L;
    @Getter @Setter
    private long gameMsgId = 0L;

    @Getter
    private TicTacToeState state = TicTacToeState.PENDING;
    private String[][] board;
    @Getter
    private long turnHolderId = 0L;
    private boolean turnFlg = true; // true - "❌", false - "⭕"

    public TicTacToeGame(long guildId, long one, long two) {
        this.guildId = guildId;
        this.firstPlayerId = one;
        this.secondPlayerId = two;
    }

    public void initializeGame() {
        this.board = new String[this.size][this.size];
        for (String[] line : this.board) {
            Arrays.fill(line, this.boardFiller);
        }

        this.turnHolderId = Math.random() <= 0.5 ? this.firstPlayerId : this.secondPlayerId;
        this.state = TicTacToeState.ACTIVE;
    }

    public void processTurn(int position) {
        String currValue = this.turnFlg ? this.crossSymbol : this.circleSymbol;
        int indexY = (position - 1) / this.size;
        int indexX = (position - 1) - indexY * this.size;

        String posValue = this.board[indexY][indexX];
        if (!posValue.equals(this.boardFiller))
            return;

        this.board[indexY][indexX] = currValue;
        this.markRemoveOldValue(position);

        if (this.isGameOver()) {
            this.state = TicTacToeState.FINISHED;
        } else {
            this.turnFlg = !this.turnFlg;
            if (this.turnHolderId == this.firstPlayerId)
                this.turnHolderId = this.secondPlayerId;
            else
                this.turnHolderId = this.firstPlayerId;
        }
    }

    public void surrenderGame(long userId) {
        if (this.firstPlayerId != userId && this.secondPlayerId != userId)
            return;

        this.state = TicTacToeState.SURRENDER;
        this.turnHolderId = userId;
    }

    public MessageCreateData getBoardRepresentation() {
        String turnHolderName = DiscordObjectsOperations.getGuildMemberById(this.guildId, this.turnHolderId).getEffectiveName();
        String headerMessage;
        switch (this.state) {
            case ACTIVE:
                headerMessage = String.format("Current turn: %s", turnHolderName);
                break;
            case FINISHED:
                headerMessage = String.format("Winner is: %s!", turnHolderName);
                break;
            case SURRENDER:
                headerMessage = String.format("%s surrendered!", turnHolderName);
                break;
            default:
                headerMessage = "";
        }

        MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
        messageBuilder.setContent(headerMessage);
        for (int i = 0; i < this.size; i++) {
            String[] line = this.board[i];
            List<Button> buttons = new ArrayList<>();
            for (int j = 0; j < this.size; j++) {
                String val = line[j];
                int rowIndex = j + 1 + i * this.size;
                String buttonIdValue = "tictactoe-board-" + rowIndex;

                if (val.equals(this.boardFiller))
                    buttons.add(Button.secondary(buttonIdValue, val));
                else if (val.startsWith("-"))
                    buttons.add(Button.success(buttonIdValue, val.substring(1)));
                else
                    buttons.add(Button.primary(buttonIdValue, val).withStyle(ButtonStyle.PRIMARY));
            }

            messageBuilder.addActionRow(buttons);
        }
        messageBuilder.addActionRow(Button.danger("tictactoe-board-surrender", "Surrender"));

        return messageBuilder.build();
    }

    public boolean isActive() {
        return this.state == TicTacToeState.PENDING || this.state == TicTacToeState.ACTIVE;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (!(other instanceof TicTacToeGame))
            return false;

        TicTacToeGame otherGame = (TicTacToeGame) other;
        if (this.guildId != otherGame.getGuildId())
            return false;

        if (this.firstPlayerId != otherGame.getFirstPlayerId() || this.secondPlayerId != otherGame.getSecondPlayerId())
            return false;

        return this.state == otherGame.getState();
    }

    private boolean isGameOver() {
        String[] rDiagonal = new String[this.size];
        String[] lDiagonal = new String[this.size];
        for (int i = 0; i < this.size; i++) {
            String[] horizontal = new String[this.size];
            String[] vertical = new String[this.size];

            for (int j = 0; j < this.size; j++) {
                horizontal[j] = this.board[i][j];
                vertical[j] = this.board[j][i];
            }

            if (this.checkWinCondition(horizontal) || this.checkWinCondition(vertical))
                return true;

            rDiagonal[i] = this.board[i][i];
            lDiagonal[i] = this.board[i][this.size - 1 - i];
        }

        return this.checkWinCondition(rDiagonal) || this.checkWinCondition(lDiagonal);
    }

    private boolean checkWinCondition(String[] line) {
        int xNum = Arrays.stream(line).map((val) -> val.contains(this.crossSymbol) ? 1 : 0).reduce(0, Integer::sum);
        int oNum = Arrays.stream(line).map((val) -> val.contains(this.circleSymbol) ? 1 : 0).reduce(0, Integer::sum);

        return xNum == this.size || oNum == this.size;
    }

    private void markRemoveOldValue(int position) {
        Queue<Integer> currQueue = this.turnFlg ? this.crossQueue : this.circleQueue;
        currQueue.add(position);

        if (currQueue.size() == 4) {
            int delPos = currQueue.poll();
            int delY = (delPos - 1) / this.size;
            int delX = (delPos - 1) - delY * this.size;

            this.board[delY][delX] = this.boardFiller;
        }

        if (currQueue.size() == 3) {
            int markPos = currQueue.peek();
            int markY = (markPos - 1) / this.size;
            int markX = (markPos - 1) - markY * this.size;

            this.board[markY][markX] = "-" + this.board[markY][markX];
        }
    }
}
