package org.fotum.app.commands.tictactoe;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.fotum.app.guild.GuildHandler;
import org.fotum.app.guild.GuildManager;
import org.fotum.app.handlers.DiscordObjectsOperations;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.modules.tictactoe.TicTacToeGame;

import java.util.List;
import java.util.stream.Collectors;

public class TicTacToeCommand implements ISlashCommand {
    @Override
    public void handle(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        Member caller = event.getMember();
        Member target = event.getOption("target").getAsMember();
        if (target.getUser().isBot() || caller.equals(target)) {
            event.getHook().sendMessage("Target player cannot be bot or yourself").queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long callerMemberId = caller.getIdLong();
        long targetMemberId = target.getIdLong();

        GuildHandler handler = GuildManager.getInstance().getGuildHandler(guildId);
        List<TicTacToeGame> activeGames = handler
                .getTicTacToeGames()
                .stream()
                .filter((game) -> game.getGuildId() == guildId && game.isActive())
                .collect(Collectors.toList());

        for (TicTacToeGame game : activeGames) {
            long playerOneId = game.getFirstPlayerId();
            long playerTwoId = game.getSecondPlayerId();

            if (callerMemberId == playerOneId || callerMemberId == playerTwoId) {
                event.getHook().sendMessage("You are already participating in active game").queue();
                return;
            }

            if (targetMemberId == playerOneId || targetMemberId == playerTwoId) {
                event.getHook().sendMessage("Target user is already participating in active game").queue();
                return;
            }
        }

        TicTacToeGame pendGame = new TicTacToeGame(guildId, callerMemberId, targetMemberId);
        handler.getTicTacToeGames().add(pendGame);

        TextChannel channel = DiscordObjectsOperations.getTextChannelById(event.getChannelIdLong());
        channel.sendMessage(String.format("%s calling %s to play TicTacToe game",
                caller.getAsMention(),
                target.getAsMention())
        ).setActionRow(
                Button.success("tictactoe-pend-accept", "Accept"),
                Button.danger("tictactoe-pend-decline", "Decline")
        ).queue(
                (msg) -> pendGame.setGameRequestMsgId(msg.getIdLong())
        );

        event.getHook().deleteOriginal().queue();
    }

    @Override
    public String getInvoke() {
        return "tictactoe";
    }
}
