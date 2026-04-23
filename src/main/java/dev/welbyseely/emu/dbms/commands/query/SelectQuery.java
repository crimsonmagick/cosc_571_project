package dev.welbyseely.emu.dbms.commands.query;

import dev.welbyseely.emu.dbms.query.Expression;
import java.util.List;

public record SelectQuery(
    List<String> columns,
    String table,
    Expression where
) implements PreparedQuery {

}