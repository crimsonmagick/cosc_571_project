package dev.welbyseely.emu.dbms.commands.results;

import dev.welbyseely.emu.dbms.table.Row;
import java.util.List;

public record TupleResult(List<Row> tuples) implements Result {

}
