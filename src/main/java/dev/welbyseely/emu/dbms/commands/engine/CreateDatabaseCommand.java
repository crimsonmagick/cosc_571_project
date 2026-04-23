package dev.welbyseely.emu.dbms.commands.engine;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;

public record CreateDatabaseCommand(String databaseName) implements PreparedCommand {

}
