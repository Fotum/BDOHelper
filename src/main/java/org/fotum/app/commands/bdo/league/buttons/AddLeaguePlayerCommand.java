package org.fotum.app.commands.bdo.league.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.fotum.app.interfaces.IButtonCommand;

public class AddLeaguePlayerCommand implements IButtonCommand {
    @Override
    public void handle(ButtonInteractionEvent event) {

    }

    @Override
    public String getCommandId() {
        return "league-button-plus";
    }
}
