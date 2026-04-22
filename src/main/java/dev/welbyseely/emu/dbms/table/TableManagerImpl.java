package dev.welbyseely.emu.dbms.table;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.exception.TableDoesNotExistException;
import dev.welbyseely.emu.dbms.exception.TableLoadException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TableManagerImpl implements TableManager {

  private final Map<String, Table> cache = new HashMap<>();

  @Override
  public Table getTable(final String name) {
    return cache.computeIfAbsent(name, this::loadTable);
  }

  private Table loadTable(String name) {
    Path path = resolveBaseDir().resolve(name + ".tbl");

    if (!Files.exists(path)) {
      throw new TableDoesNotExistException(name);
    }

    try {
      return TableProvider.load(path);
    } catch (final RuntimeException runtimeException) {
      throw new TableLoadException(name, runtimeException);
    }
  }
}
