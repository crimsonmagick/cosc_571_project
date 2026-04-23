package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.TableManager;
import dev.welbyseely.emu.dbms.table.TableManagerImpl;
import java.util.List;

public final class Dbms {

  private static final Dbms INSTANCE = new Dbms();

  private final DatabaseEngine engine;

  private Dbms() {
    TableManager tableManager = new TableManagerImpl(); // shared cache
    this.engine = new DatabaseEngine(tableManager);
  }

  public static Dbms get() {
    return INSTANCE;
  }

  public List<Row> execute(String sql) {
    return engine.execute(sql);
  }
}