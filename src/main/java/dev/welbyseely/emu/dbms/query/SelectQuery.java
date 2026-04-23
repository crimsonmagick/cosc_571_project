package dev.welbyseely.emu.dbms.query;

import java.util.List;

public record SelectQuery(
    List<String> columns,
    String table,
    Expression where
) implements PreparedQuery {

}