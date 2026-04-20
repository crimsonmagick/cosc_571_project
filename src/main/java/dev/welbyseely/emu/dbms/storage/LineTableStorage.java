package dev.welbyseely.emu.dbms.storage;

import dev.welbyseely.emu.dbms.exception.TableStorageException;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineTableStorage implements TableStorage {

  private final Path path;
  private final Schema schema;
  private final int dataStartLine;
  private int nextLine;

  public LineTableStorage(Path path, Schema schema) {
    this.path = path;
    this.schema = schema;
    this.dataStartLine = initFile();
    this.nextLine = dataStartLine;
  }

  @Override
  public RecordPointer insert(final Row row) {
    try {
      int lineNumber = nextLine++;
      try (BufferedWriter w = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
        w.write(serialize(row));
        w.newLine();
      }

      return new LinePointer(lineNumber);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Row read(RecordPointer pointer) {
    int target = ((LinePointer) pointer).line();

    try (BufferedReader r = Files.newBufferedReader(path)) {
      String line;
      int i = 0;

      while ((line = r.readLine()) != null) {
        if (i == target) {
          if (line.startsWith("#") || line.isBlank()) {
            return null; // deleted or invalid
          }
          return deserialize(line);
        }
        i++;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  @Override
  public Iterable<RecordPointer> scan() {
    List<RecordPointer> pointers = new ArrayList<>();

    try (BufferedReader r = Files.newBufferedReader(path)) {
      String line;
      int i = 0;

      while ((line = r.readLine()) != null) {
        if (i >= dataStartLine && !line.isBlank() && !line.startsWith("#")) {
          pointers.add(new LinePointer(i));
        }
        i++;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return pointers;
  }

  /**
   * Creates file for Table
   *
   * @return First line containing a tuple
   */
  private int initFile() {
    if (!Files.exists(path)) {
      try {
        Files.createFile(path);
      } catch (final IOException e) {
        final String errorMessage = String.format(
            "Unable to create table file for schema with schemaName=%s", schema.schemaName());
        System.out.println(("Error: " + errorMessage));
        throw new TableStorageException(errorMessage, e);
      }

      int line = 0;

      try (BufferedWriter w = Files.newBufferedWriter(path)) {
        w.write("#schema");
        w.newLine();
        line++;

        w.write("table|" + schema.schemaName());
        w.newLine();
        line++;

        for (Attribute attr : schema.attributes()) {
          w.write(attr.name() + "|" + attr.type() + "|" + attr.primaryKey());
          w.newLine();
          line++;
        }

        w.newLine();
        line++;          // blank line
        w.write("#data");
        w.newLine();
        line++;

        // first data row will be at this line
        return line;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new UnsupportedOperationException("Not yet implemented");
    }
  }

  private int countLines() throws IOException {
    try (BufferedReader r = Files.newBufferedReader(path)) {
      int count = 0;
      while (r.readLine() != null) {
        count++;
      }
      return count;
    }
  }

  private String serialize(Row row) {
    List<String> parts = new ArrayList<>();

    for (Attribute attr : schema.attributes()) {
      Object v = row.get(attr.name());
      parts.add(v == null ? "" : v.toString());
    }

    return String.join("|", parts);
  }

  private Row deserialize(String line) {
    final String[] parts = line.split("\\|", -1);
    final Map<String, Object> values = new HashMap<>();

    final List<Attribute> attrs = schema.attributes();

    for (int i = 0; i < attrs.size(); i++) {
      final Attribute attr = attrs.get(i);
      final String raw = parts[i];
      final DataType type = DataType.valueOf(attr.type()); // TODO validate
      values.put(attr.name(), parse(raw, type));
    }

    return new Row(values);
  }

  private Object parse(String raw, DataType type) {
    return switch (type) {
      case DataType.INTEGER -> raw.isEmpty() ? null : Integer.parseInt(raw);
      case DataType.FLOAT -> raw.isEmpty() ? null : Double.parseDouble(raw);
      case DataType.TEXT -> raw;
    };
  }

  public static void main(String[] args) {
    final Schema schema = new Schema(
        "student",
        List.of(
            new Attribute("id", DataType.INTEGER.name(), true),
            new Attribute("name", DataType.TEXT.name(), false),
            new Attribute("gpa", DataType.FLOAT.name(), false)
        )
    );

    final Path path = Path.of("student.tbl");
    final LineTableStorage storage = new LineTableStorage(path, schema);

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