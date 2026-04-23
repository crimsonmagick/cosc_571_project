package dev.welbyseely.emu.dbms.commands.query;

import dev.welbyseely.emu.dbms.query.Expression;
import java.util.Map;

public record DeleteQuery(String table, Expression where) implements
    PreparedQuery {

}