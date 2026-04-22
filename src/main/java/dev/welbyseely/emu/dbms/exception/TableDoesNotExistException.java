package dev.welbyseely.emu.dbms.exception;

public class TableDoesNotExistException extends TableLoadException {
  public TableDoesNotExistException(final String tableName) {
    super("Table does not exist: " + tableName);
  }
}