package dev.welbyseely.emu.dbms.exception;

public class TableLoadException extends RuntimeException {

  public TableLoadException(final String tableName, final Throwable e) {
    super("Table failed to load " + tableName, e);
  }

  public TableLoadException(final String message) {
    super(message);
  }
}
