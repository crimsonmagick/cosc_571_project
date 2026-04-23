package dev.welbyseely.emu.dbms.commands.query;

import dev.welbyseely.emu.dbms.query.Expression;

import java.util.List;

public record SelectQuery(
        List<String> columns,
        List<String> tables,
        Expression where,
        Aggregate aggregate
) implements PreparedQuery {
}