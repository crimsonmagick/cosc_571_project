package dev.welbyseely.emu.dbms.commands.query;

import java.util.List;

public record RenameQuery(
    String table,
    List<String> newNames
) implements PreparedQuery {

  public RenameQuery {
    newNames = List.copyOf(newNames);
  }
}