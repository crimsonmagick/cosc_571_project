package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
import dev.welbyseely.emu.dbms.table.Row;
import java.util.List;

public final class Dbms {

  private static final Dbms INSTANCE = new Dbms();

  private final DatabaseEngine engine;

  private Dbms() {
    this.engine = new DatabaseEngine();
  }

  public static Dbms get() {
    return INSTANCE;
  }

  public List<Row> execute(String sql) {
    return engine.execute(sql);
  }

  public DatabaseEngine getEngine() {
    return this.engine;
  }
}