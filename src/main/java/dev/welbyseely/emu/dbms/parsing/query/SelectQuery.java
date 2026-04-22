package dev.welbyseely.emu.dbms.parsing.query;

import java.util.List;

public record SelectQuery(
    List<String> columns,
    String table,
    Expression where
) {

}