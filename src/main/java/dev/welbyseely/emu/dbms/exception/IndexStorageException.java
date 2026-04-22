package dev.welbyseely.emu.dbms.exception;

public class IndexStorageException extends RuntimeException {

  public IndexStorageException(final String message) {
    super(message);
  }

  public IndexStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
