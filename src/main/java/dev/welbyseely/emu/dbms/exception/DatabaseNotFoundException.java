package dev.welbyseely.emu.dbms.exception;

public class DatabaseNotFoundException extends RuntimeException {

  public DatabaseNotFoundException(String message) {
    super(message);
  }
}
