package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.schema.Schema;
import java.nio.file.Path;

public interface Database {

  Table createTable(Schema schema);

  Table getTable(String name);

  String getName();

}