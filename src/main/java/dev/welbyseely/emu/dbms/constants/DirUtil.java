package dev.welbyseely.emu.dbms.constants;

import java.nio.file.Path;

public class DirUtil {

  private DirUtil() {

  }

  public static final String TABLE_EXTENSION = ".tbl";
  public static final String DB_PATH_PROPERTY = "db.path";
  public static final String DEFAULT_DB_DIR = "cosc_571_db";

  public static Path resolveBaseDir() {
    final String configuredPath = System.getProperty(DB_PATH_PROPERTY);

    if (configuredPath != null && !configuredPath.isBlank()) {
      return Path.of(configuredPath);
    }

    return Path.of(System.getProperty("user.dir"), DEFAULT_DB_DIR);
  }
}
