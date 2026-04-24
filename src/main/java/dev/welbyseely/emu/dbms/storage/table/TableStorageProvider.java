package dev.welbyseely.emu.dbms.storage.table;

import static dev.welbyseely.emu.dbms.constants.DirUtil.TABLE_EXTENSION;
import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.exception.TableStorageException;
import dev.welbyseely.emu.dbms.schema.Schema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TableStorageProvider {

  public static TableStorage createTableStorage(final Schema schema, final Path dbPath) {
    final Path path = dbPath.resolve(schema.schemaName().toLowerCase() + TABLE_EXTENSION);

    try {
      final Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
    } catch (Exception e) {
      throw new TableStorageException("Unable to create directories for table path: " + path, e);
    }

    return new LineTableStorage(path, schema);
  }

  public static List<TableStorage> readTablesInDatabase(Path dbPath) {
    if (!Files.exists(dbPath) || !Files.isDirectory(dbPath)) {
      return List.of();
    }

    try (var stream = Files.list(dbPath)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(TABLE_EXTENSION))
          .map(TableStorageProvider::readTableStorage)
          .toList();
    } catch (Exception e) {
      throw new TableStorageException("Unable to list tables in: " + dbPath, e);
    }
  }

  private static List<Path> tablePaths() {
    final Path base = resolveBaseDir();

    if (!Files.exists(base) || !Files.isDirectory(base)) {
      throw new TableStorageException(
          "Database path does not exist or is not a directory: " + base);
    }

    try (var stream = Files.walk(base)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(p -> p.getFileName().toString().endsWith(TABLE_EXTENSION))
          .toList();
    } catch (Exception e) {
      throw new TableStorageException("Unable to list table files in: " + base, e);
    }
  }

  public static TableStorage readTableStorage(final Path tablePath) {
    return new LineTableStorage(tablePath);
  }

}