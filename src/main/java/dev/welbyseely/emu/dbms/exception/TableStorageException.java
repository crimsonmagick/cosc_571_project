package dev.welbyseely.emu.dbms.exception;

public class TableStorageException extends RuntimeException {

  public TableStorageException(final String message) {
    super(message);
  }

  public TableStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
