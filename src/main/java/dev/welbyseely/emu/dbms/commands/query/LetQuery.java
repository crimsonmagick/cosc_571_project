package dev.welbyseely.emu.dbms.commands.query;

public record LetQuery(
    String table,
    String key,
    SelectQuery select
) implements PreparedQuery {}