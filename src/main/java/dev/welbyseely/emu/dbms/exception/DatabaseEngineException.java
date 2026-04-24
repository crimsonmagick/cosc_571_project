package dev.welbyseely.emu.dbms.exception;

import java.io.IOException;

public class DatabaseEngineException extends RuntimeException {

  public DatabaseEngineException(String message) {
    super(message);
  }

  public DatabaseEngineException(String message, IOException e) {

    super(message, e);
  }
}
