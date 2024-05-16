package org.fotum.app;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.fotum.app.commands.bdo.ForceRegUserCommand;
import org.fotum.app.commands.bdo.ForceRemUserCommand;
import org.fotum.app.commands.bdo.RegUserCommand;
import org.fotum.app.commands.bdo.RemUserCommand;
import org.fotum.app.commands.bdo.siege.buttons.AddSiegePlayerCommand;
import org.fotum.app.commands.bdo.siege.buttons.RemoveSiegePlayerCommand;
import org.fotum.app.commands.bdo.siege.control.*;
import org.fotum.app.commands.bdo.siege.settings.AutoregCommand;
import org.fotum.app.commands.bdo.siege.settings.SetupCommand;
import org.fotum.app.commands.general.ClearCommand;
import org.fotum.app.commands.general.RandomizeCommand;
import org.fotum.app.commands.owner.EvalCommand;
import org.fotum.app.commands.owner.ShutdownCommand;
import org.fotum.app.commands.tictactoe.TicTacToeCommand;
import org.fotum.app.commands.tictactoe.buttons.BoardInteractionCommand;
import org.fotum.app.commands.tictactoe.buttons.BoardSurrenderCommand;
import org.fotum.app.commands.tictactoe.buttons.PendingAcceptCommand;
import org.fotum.app.commands.tictactoe.buttons.PendingDeclineCommand;
import org.fotum.app.interfaces.IButtonCommand;
import org.fotum.app.interfaces.ISlashCommand;
import org.fotum.app.interfaces.ITextCommand;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Pattern;

public class CommandManager {
    private final Map<String, ITextCommand> textCommands = new HashMap<>();
    private final Map<String, IButtonCommand> buttonCommands = new HashMap<>();
    private final Map<String, ISlashCommand> slashCommands = new HashMap<>();

    CommandManager() {
        // Owner commands
        this.addTextCommand(new EvalCommand());
        this.addTextCommand(new ShutdownCommand());

        // General purpose commands
        this.addSlashCommand(new ClearCommand());
        this.addSlashCommand(new RandomizeCommand());

        // --- TicTacToe SECTION --- \\
        // Game command
        this.addSlashCommand(new TicTacToeCommand());
        // Buttons
        this.addButtonCommand(new PendingAcceptCommand());
        this.addButtonCommand(new PendingDeclineCommand());
        this.addButtonCommand(new BoardInteractionCommand());
        this.addButtonCommand(new BoardSurrenderCommand());

        // --- SIEGE SECTION --- \\
        // Siege managing commands
        this.addSlashCommand(new SetupCommand());
        this.addSlashCommand(new AddSiegeCommand());
        this.addSlashCommand(new RemoveSiegeCommand());
        this.addSlashCommand(new AutoregCommand());
        this.addSlashCommand(new UpdateSiegeCommand());
        // Managing commands to force add/remove players from list
        this.addSlashCommand(new ForceAddPlayersCommand());
        this.addSlashCommand(new ForceRemPlayersCommand());
        // Buttons
        this.addButtonCommand(new AddSiegePlayerCommand());
        this.addButtonCommand(new RemoveSiegePlayerCommand());
        // Managing BDO names commands
        this.addSlashCommand(new ForceRegUserCommand());
        this.addSlashCommand(new ForceRemUserCommand());
        // Register BDO names commands
        this.addSlashCommand(new RegUserCommand());
        this.addSlashCommand(new RemUserCommand());
        this.addSlashCommand(new PriorityCommand());

        // --- LEAGUE SECTION --- \\
        // League managing command
//        this.addSlashCommand(new AddLeagueCommand());
//        this.addSlashCommand(new RemoveLeagueCommand());
//        // Buttons
//        this.addButtonCommand(new AddLeaguePlayerCommand());
//        this.addButtonCommand(new RemoveLeaguePlayerCommand());
    }

    void handleTextCommand(@NotNull MessageReceivedEvent event) {
        User author = event.getAuthor();
        String content = event.getMessage().getContentRaw();

        if (!author.isBot() && !event.getMessage().isWebhookMessage() && content.startsWith(Constants.PREFIX)) {
            final String[] split = content.replaceFirst("(?i)" + Pattern.quote(Constants.PREFIX), "").split("\\s+");
            final String invoke = split[0].toLowerCase();

            ITextCommand cmd = this.textCommands.get(invoke);
            if (Objects.nonNull(cmd)) {
                ChannelType channelType = event.getChannelType();
                if (channelType == ChannelType.PRIVATE && !cmd.canBePrivate())
                    return;

                final List<String> args = new ArrayList<>(Arrays.asList(split).subList(1, split.length));
                cmd.handle(args, event);
            }
        }
    }

    void handleButtonCommand(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();

        String componentId = event.getComponentId();
        if (this.buttonCommands.containsKey(componentId))
            this.buttonCommands.get(componentId).handle(event);
        else if (componentId.startsWith("tictactoe-board-"))
            this.buttonCommands.get("tictactoe-board-").handle(event);
    }

    void handleSlashCommand(@NotNull SlashCommandInteractionEvent event) {
        String commandNm = event.getName();
        if (this.slashCommands.containsKey(commandNm))
            this.slashCommands.get(commandNm).handle(event);
    }

    private void addTextCommand(@NotNull ITextCommand command) {
        if (!this.textCommands.containsKey(command.getInvoke()))
            this.textCommands.put(command.getInvoke(), command);
    }

    private void addButtonCommand(@NotNull IButtonCommand command) {
        if (!this.buttonCommands.containsKey(command.getCommandId()))
            this.buttonCommands.put(command.getCommandId(), command);
    }

    private void addSlashCommand(@NotNull ISlashCommand command) {
        if (!this.slashCommands.containsKey(command.getInvoke()))
            this.slashCommands.put(command.getInvoke(), command);
    }
}
