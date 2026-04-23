package dev.welbyseely.emu.dbms.commands.engine;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;

public record CreateCommand(String databaseName) implements PreparedCommand {

}
