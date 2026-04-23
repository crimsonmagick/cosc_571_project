package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.query.PreparedQuery;
import dev.welbyseely.emu.dbms.schema.Schema;
import java.util.List;

public interface Database {

  Table createTable(Schema schema);

  Table getTable(String name);

  String getName();

  List<Row> executeQuery(PreparedQuery preparedQuery);

}