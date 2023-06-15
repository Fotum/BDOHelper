package org.fotum.app.features.tictactoe;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.fotum.app.utils.DiscordObjectsGetters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TicTacToeGame
{
    @Getter
    private final long userOneId;
    @Getter
    private final long userTwoId;
    @Getter
    private final long guildId;
    @Getter
    private final long channelId;
    @Getter
    private long gameRequestMsgId = 0L;
    @Getter
    private long gameMessageId = 0L;
    @Getter
    private int state = 0; // 0 - Pending, 1 - active, 2 - over

    @Getter
    private final int size;
    private String[][] board;
    @Getter
    private long turnHolderId = 0L;
    private boolean valFlg = true; // true - X, false - O
    private final String boardFiller = ".";

    public TicTacToeGame(long guildId, long channelId, long one, long two, int size)
    {
        this.guildId = guildId;
        this.channelId = channelId;
        this.userOneId = one;
        this.userTwoId = two;
        this.size = size;
    }

    public void sendGameRequestMessage()
    {
        Member caller = DiscordObjectsGetters.getGuildMemberById(this.guildId, this.userOneId);
        Member target = DiscordObjectsGetters.getGuildMemberById(this.guildId, this.userTwoId);
        TextChannel channel = DiscordObjectsGetters.getTextChannelById(this.channelId);

        channel.sendMessage(String.format("%s calling %s to play TicTacToe with board size %d",
                caller.getAsMention(),
                target.getAsMention(),
                this.size)
        ).setActionRow(
                Button.success("tictactoe-pend-accept", "Accept"),
                Button.danger("tictactoe-pend-decline", "Decline")
        ).queue(
                (msg) -> this.gameRequestMsgId = msg.getIdLong()
        );
    }

    public void deleteGameRequestMessage()
    {
        if (this.gameRequestMsgId == 0L)
            return;

        Message requestMessage = DiscordObjectsGetters.getMessageById(this.channelId, this.gameRequestMsgId);
        if (Objects.nonNull(requestMessage))
        {
            requestMessage.delete().queue();
            this.gameRequestMsgId = 0L;
        }
    }

    public void startAcceptedGame()
    {
        this.deleteGameRequestMessage();

        this.board = new String[this.size][this.size];
        for (String[] line : this.board)
            Arrays.fill(line, this.boardFiller);

        boolean turnFlg = Math.random() <= 0.5;
        if (turnFlg)
            this.turnHolderId = this.userOneId;
        else
            this.turnHolderId = this.userTwoId;

        this.state = 1;
        Member turnHolder = DiscordObjectsGetters.getGuildMemberById(this.guildId, this.turnHolderId);
        String header = String.format("Current turn: %s", turnHolder.getEffectiveName());
        this.printBoard(header);
    }

    public void makeTurn(int position)
    {
        String currValue = this.valFlg ? "❌" : "⭕";
        int indexY = (position - 1) / this.size;
        int indexX = (position - 1) - indexY * this.size;

        String posValue = this.board[indexY][indexX];
        if (!posValue.equalsIgnoreCase(this.boardFiller))
            return;

        this.valFlg = !this.valFlg;
        this.board[indexY][indexX] = currValue;

        String headerMsg = "";
        if (this.isGameOver())
        {
            Member turnHolder = DiscordObjectsGetters.getGuildMemberById(this.guildId, this.turnHolderId);
            headerMsg = String.format("Winner is: %s!", turnHolder.getEffectiveName());
            this.state = 2;
        }
        else if (this.isDraw())
        {
            headerMsg = "Game ended in draw!";
            this.state = 2;
        }
        else
        {
            if (this.turnHolderId == this.userOneId)
                this.turnHolderId = this.userTwoId;
            else
                this.turnHolderId = this.userOneId;

            Member turnHolder = DiscordObjectsGetters.getGuildMemberById(this.guildId, this.turnHolderId);
            headerMsg = String.format("Current turn: %s", turnHolder.getEffectiveName());
        }

        this.printBoard(headerMsg);
    }

    public void surrenderGame(long userId)
    {
        if (this.userOneId != userId && this.userTwoId != userId)
            return;

        this.state = 2;

        Member surrMember = DiscordObjectsGetters.getGuildMemberById(this.guildId, userId);
        String headerMsg = String.format("%s surrendered!", surrMember.getEffectiveName());

        this.printBoard(headerMsg);
    }

    @Override
    public boolean equals(Object other)
    {
        if (Objects.isNull(other))
            return false;

        if (!(other instanceof TicTacToeGame))
            return false;

        TicTacToeGame otherGame = (TicTacToeGame) other;
        if (this.userOneId != otherGame.getUserOneId())
            return false;

        if (this.userTwoId != otherGame.getUserTwoId())
            return false;

        if (this.guildId != otherGame.getGuildId())
            return false;

        if (this.size != otherGame.getSize())
            return false;

        if (this.state != otherGame.getState())
            return false;

        return true;
    }

    private boolean isGameOver()
    {
        // Check horizontals
        for (String[] horizontal : this.board)
        {
            if (this.checkStringForValues(horizontal))
                return true;
        }

        // Check vertices
        for (int i = 0; i < this.size; i++)
        {
            String[] vertice = new String[this.size];
            for (int j = 0; j < this.size; j++)
                vertice[j] = this.board[j][i];

            if (this.checkStringForValues(vertice))
                return true;
        }

        // Check diagonals
        // Check right diagonal
        String[] diagonal = new String[this.size];
        for (int i = 0; i < this.size; i++)
        {
            diagonal[i] = this.board[i][i];
        }

        if (this.checkStringForValues(diagonal))
            return true;

        // Check left diagonal
        diagonal = new String[this.size];
        for (int i = 0; i < this.size; i++)
        {
            diagonal[i] = this.board[i][this.size - 1 - i];
        }

        return this.checkStringForValues(diagonal);
    }

    private boolean isDraw()
    {
        int lineSpaceCnt = 0;
        for (String[] line : this.board)
        {
            lineSpaceCnt += Arrays.stream(line)
                    .map((val) -> val.equals(this.boardFiller) ? 1 : 0)
                    .reduce(0, Integer::sum);
        }

        return lineSpaceCnt == 0;
    }

    private void printBoard(String header)
    {
        List<ActionRow> actionRowList = new ArrayList<>();
        for (int i = 0; i < this.size; i++)
        {
            String[] line = this.board[i];
            List<Button> buttons = new ArrayList<>();
            for (int j = 0; j < this.size; j++)
            {
                String val = line[j];
                int rowValue = j + 1 + i * this.size;
                String buttonIdValue = "tictactoe-board-" + rowValue;

                if (val.equalsIgnoreCase("❌"))
                    buttons.add(Button.primary(buttonIdValue, val).withStyle(ButtonStyle.PRIMARY));
                else if (val.equalsIgnoreCase("⭕"))
                    buttons.add(Button.primary(buttonIdValue, val).withStyle(ButtonStyle.PRIMARY));
                else
                    buttons.add(Button.secondary(buttonIdValue, val));
            }

            actionRowList.add(ActionRow.of(buttons));
        }
        actionRowList.add(ActionRow.of(Button.danger("tictactoe-board-surrender", "Surrender")));

        TextChannel channel = DiscordObjectsGetters.getTextChannelById(this.channelId);
        if (this.gameMessageId != 0L)
        {
            Message message = DiscordObjectsGetters.getMessageById(this.channelId, this.gameMessageId);
            message.editMessage(header)
                    .flatMap(
                        (msg) -> msg.editMessageComponents(actionRowList)
                    ).queue();
        }
        else
        {
            MessageCreateBuilder messageCreateBuilder = new MessageCreateBuilder();
            messageCreateBuilder.addContent(header);
            for (ActionRow row : actionRowList)
                messageCreateBuilder.addActionRow(row.getComponents());

            channel.sendMessage(messageCreateBuilder.build()).queue((msg) -> this.gameMessageId = msg.getIdLong());
        }
    }

    private boolean checkStringForValues(String[] stringArr)
    {
        long xNum = Arrays.stream(stringArr).filter((val) -> val.equalsIgnoreCase("❌")).count();
        long oNum = Arrays.stream(stringArr).filter((val) -> val.equalsIgnoreCase("⭕")).count();

        int checkValue = this.size;
//        if (this.size == 4)
//            checkValue = 3;

        return xNum == checkValue || oNum == checkValue;
    }
}
