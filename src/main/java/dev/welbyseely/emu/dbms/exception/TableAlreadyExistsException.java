package dev.welbyseely.emu.dbms.exception;

public class TableAlreadyExistsException extends RuntimeException {

  public TableAlreadyExistsException(final String message) {
    super(message);
  }

  public TableAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
