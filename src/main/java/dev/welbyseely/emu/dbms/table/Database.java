package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.commands.query.PreparedQuery;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.schema.Schema;

public interface Database {

  Table createTable(Schema schema);

  Table getTable(String name);

  String getName();

  Result executeQuery(PreparedQuery preparedQuery);

}