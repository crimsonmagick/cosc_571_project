package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
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

  public List<Result> execute(final String sql) {
    return engine.execute(sql);
  }

  public Result executeStatement(final String sql) {
    return engine.executeStatement(sql);
  }

  public DatabaseEngine getEngine() {
    return this.engine;
  }
}