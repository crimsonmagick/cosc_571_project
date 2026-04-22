package dev.welbyseely.emu.dbms.storage;

import dev.welbyseely.emu.dbms.exception.TableStorageException;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TableStorageProvider {

  private static final String TABLE_EXTENSION = ".tbl";
  private static final String DB_PATH_PROPERTY = "db.path";
  private static final String DEFAULT_DB_DIR = "cosc_571_db";

private static Path baseDbPath() {
  final String configuredPath = System.getProperty(DB_PATH_PROPERTY);

  if (configuredPath != null && !configuredPath.isBlank()) {
    return Path.of(configuredPath);
  }

  return Path.of(System.getProperty("user.dir"), DEFAULT_DB_DIR);
}

  private static Path tableStoragePath(final Schema schema) {
    return baseDbPath().resolve(schema.schemaName() + TABLE_EXTENSION);
  }

  public static TableStorage createTableStorage(final Schema schema) {
    final Path path = tableStoragePath(schema);

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

  public static List<TableStorage> readAllTables() {
    return tablePaths().stream()
        .map(TableStorageProvider::readTableStorage)
        .toList();
  }

  private static List<Path> tablePaths() {
    final Path base = baseDbPath();

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


  public static void main(String[] args) {
    createTest();
    readTest();
  }

  private static void readTest() {
    final TableStorage tableStorage = readAllTables().get(0);
    for (RecordPointer pointer : tableStorage.scan()) {
      System.out.println(pointer + " -> " + tableStorage.read(pointer));
    }
    tableStorage.remove();
  }

  private static void createTest() {
    final Schema schema = new Schema(
        "student",
        List.of(
            new Attribute("id", DataType.INTEGER.name(), true),
            new Attribute("name", DataType.TEXT.name(), false),
            new Attribute("gpa", DataType.FLOAT.name(), false)
        )
    );

    final TableStorage storage = createTableStorage(schema);

    final Row row1 = new Row(Map.of(
        "id", 1,
        "name", "Alice",
        "gpa", 3.5
    ));

    final Row row2 = new Row(Map.of(
        "id", 2,
        "name", "Bob",
        "gpa", 2.75
    ));

    final RecordPointer ptr1 = storage.insert(row1);
    final RecordPointer ptr2 = storage.insert(row2);

    System.out.println("Inserted row1 at: " + ptr1);
    System.out.println("Inserted row2 at: " + ptr2);

    final Row readBack = storage.read(ptr1);
    System.out.println("Read back row1: " + readBack);

    System.out.println("Scanning all rows:");
    for (RecordPointer pointer : storage.scan()) {
      System.out.println(pointer + " -> " + storage.read(pointer));
    }

    if (ptr1 instanceof LinePointer lp) {
      System.out.println("First row line number: " + lp.line());
    }
  }
}