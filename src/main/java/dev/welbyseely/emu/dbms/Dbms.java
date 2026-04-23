package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Database;
import java.util.List;

public final class Dbms {

  private static final Dbms INSTANCE = new Dbms();

  private final DatabaseEngine engine;

  private Dbms() {
    Database database = new DatabaseImpl("tempdb"); // TODO need to support multiple!!
    this.engine = new DatabaseEngine(database);
  }

  public static Dbms get() {
    return INSTANCE;
  }

  public List<Row> execute(String sql) {
    return engine.execute(sql);
  }
}