package dev.welbyseely.emu.dbms.commands.query;

public record DescribeQuery(
    boolean all,
    String table
) implements PreparedQuery {

}