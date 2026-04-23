package dev.welbyseely.emu.dbms.commands.query;

import java.util.List;

public record InsertQuery(
    String tableName,
    List<String> values
) implements PreparedQuery {

  public InsertQuery {
    values = List.copyOf(values);
  }
}