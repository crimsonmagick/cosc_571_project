package dev.welbyseely.emu.dbms.commands.query;

import dev.welbyseely.emu.dbms.schema.Attribute;
import java.util.List;

public record CreateTableQuery(
    String table,
    List<Attribute> attributes
) implements PreparedQuery {

}