package dev.welbyseely.emu.dbms.commands.engine;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;

public record InputCommand(String input, String output) implements PreparedCommand {

}
