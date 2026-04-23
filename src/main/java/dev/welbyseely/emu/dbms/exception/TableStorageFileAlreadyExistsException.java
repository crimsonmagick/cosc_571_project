package dev.welbyseely.emu.dbms.exception;

public class TableStorageFileAlreadyExistsException extends RuntimeException {

  public TableStorageFileAlreadyExistsException(final String message, final Throwable e) {
    super(message, e);
  }

  public TableStorageFileAlreadyExistsException(final String message) {
    super(message);
  }
}
