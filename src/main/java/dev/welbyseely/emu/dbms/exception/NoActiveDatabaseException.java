package dev.welbyseely.emu.dbms.exception;

public class NoActiveDatabaseException extends RuntimeException {

  public NoActiveDatabaseException(String message) {
    super(message);
  }
}
