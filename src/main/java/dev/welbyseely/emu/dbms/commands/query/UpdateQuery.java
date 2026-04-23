package dev.welbyseely.emu.dbms.commands.query;

import dev.welbyseely.emu.dbms.query.Expression;
import java.util.Map;

public record UpdateQuery(String table, Map<String, String> updates, Expression where) implements
    PreparedQuery {

}