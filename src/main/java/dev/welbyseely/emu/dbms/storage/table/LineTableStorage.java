package dev.welbyseely.emu.dbms.storage.table;

import static dev.welbyseely.emu.dbms.util.DatatypeParser.parse;

import dev.welbyseely.emu.dbms.exception.TableStorageException;
import dev.welbyseely.emu.dbms.exception.TableStorageFileAlreadyExistsException;
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
import java.util.LinkedHashMap;
import java.util.List;

public class LineTableStorage implements TableStorage {

  private final Path path;
  private Schema schema;
  private final int dataStartLine;
  private int nextLine;
  private boolean removed;

  public LineTableStorage(final Path path, final Schema schema) {
    this.path = path;
    this.schema = schema;
    this.dataStartLine = createFile(this.path, this.schema);
    this.nextLine = dataStartLine;
    this.removed = false;
  }

  public LineTableStorage(final Path path) {
    this.path = path;
    final ReadResult result = readFile(path);
    this.dataStartLine = result.dataStartLine();
    this.schema = result.schema();
    this.nextLine = countLines();
    this.removed = false;
  }

  @Override
  public RecordPointer insert(final Row row) {
    if (removed) {
      throw new TableStorageException("Table is already removed");
    }
    try {
      int lineNumber = nextLine++;
      try (BufferedWriter w = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
        w.write(serialize(row));
        w.newLine();
      }

      return new LinePointer(lineNumber);
    } catch (IOException e) {
      throw new RuntimeException("Failed to insert row", e);
    }
  }

  @Override
  public Row read(RecordPointer pointer) {
    if (removed) {
      throw new TableStorageException("Table is already removed");
    }
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
      throw new RuntimeException("Failed to read RecordPointer from TableStorage", e);
    }

    return null;
  }

  @Override
  public Iterable<RecordPointer> scan() {
    if (removed) {
      throw new TableStorageException("Table is already removed");
    }
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
      throw new RuntimeException("Failed to scan", e);
    }

    return pointers;
  }

  @Override
  public Schema getSchema() {
    return this.schema;
  }

  @Override
  public void remove() {
    if (removed) {
      return;
    }
    try {
      Files.deleteIfExists(path);
      removed = true;
    } catch (final IOException e) {
      throw new TableStorageException("Unable to remove table file: " + path, e);
    }
  }

  @Override
  public boolean isRemoved() {
    return removed;
  }

  @Override
  public void delete(RecordPointer pointer) {
    int target = ((LinePointer) pointer).line();

    List<String> lines = null;
    try {
      lines = Files.readAllLines(path);
      lines.set(target, "#DELETED");

      Files.write(path, lines);
    } catch (IOException e) {
      throw new TableStorageException("Unable ", e);
    }
  }

  @Override
  public void update(RecordPointer pointer, Row newRow) {
    if (removed) {
      throw new TableStorageException("Table is already removed");
    }

    int target = ((LinePointer) pointer).line();

    try {
      List<String> lines = Files.readAllLines(path);

      if (target < 0 || target >= lines.size()) {
        throw new TableStorageException("Invalid record pointer: " + pointer);
      }

      String existing = lines.get(target);

      // prevent updating deleted or invalid rows
      if (existing == null || existing.isBlank() || existing.startsWith("#")) {
        throw new TableStorageException("Cannot update deleted or invalid row at: " + pointer);
      }

      lines.set(target, serialize(newRow));

      Files.write(path, lines);
    } catch (IOException e) {
      throw new TableStorageException("Unable to update row at pointer: " + pointer, e);
    }
  }

  @Override
  public void rewriteSchema(Schema newSchema) {

    try {
      List<String> lines = Files.readAllLines(path);

      List<String> newLines = new ArrayList<>();

      int i = 0;

      newLines.add(lines.get(i++));

      newLines.add("table|" + newSchema.schemaName());

      for (Attribute attr : newSchema.attributes()) {
        newLines.add(attr.name() + "|" + attr.type() + "|" + attr.primaryKey());
      }

      while (!lines.get(i).equals("#data")) {
        i++;
      }

      newLines.add("");
      newLines.add("#data");
      i++;

      // copy all data rows unchanged
      while (i < lines.size()) {
        newLines.add(lines.get(i++));
      }

      Files.write(path, newLines);

      // update internal schema reference
      this.schema = newSchema;

    } catch (IOException e) {
      throw new TableStorageException("Failed to rewrite schema", e);
    }
  }

  private record ReadResult(int dataStartLine, Schema schema) {

  }

  private static ReadResult readFile(final Path path) {
    if (!Files.exists(path)) {
      throw new TableStorageException("Table file does not exist: " + path);
    }

    try (BufferedReader r = Files.newBufferedReader(path)) {
      String line;
      int lineNumber = 0;

      // #schema
      line = r.readLine();
      if (line == null || !line.equals("#schema")) {
        throw new TableStorageException("Missing #schema header");
      }
      lineNumber++;

      // table|<schemaName>
      line = r.readLine();
      if (line == null) {
        throw new TableStorageException("Missing table metadata line");
      }
      lineNumber++;

      String[] tableParts = line.split("\\|", -1);
      if (tableParts.length != 2 || !tableParts[0].equals("table") || tableParts[1].isBlank()) {
        throw new TableStorageException("Invalid table metadata line: " + line);
      }
      final String schemaName = tableParts[1];

      final List<Attribute> attributes = new ArrayList<>();

      while ((line = r.readLine()) != null) {
        // blank line between schema and #data
        if (line.isBlank()) {
          lineNumber++;
          continue;
        }

        if (line.equals("#data")) {
          lineNumber++;
          return new ReadResult(lineNumber, new Schema(schemaName, attributes));
        }

        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
          throw new TableStorageException("Invalid schema row at line " + lineNumber + ": " + line);
        }

        final String attrName = parts[0];
        final String attrType = parts[1];
        final String pkRaw = parts[2];

        if (attrName.isBlank()) {
          throw new TableStorageException("Blank attribute name at line " + lineNumber);
        }

        try {
          DataType.valueOf(attrType);
        } catch (IllegalArgumentException e) {
          throw new TableStorageException(
              "Invalid attribute type at line " + lineNumber + ": " + attrType, e);
        }

        if (!pkRaw.equals("true") && !pkRaw.equals("false")) {
          throw new TableStorageException(
              "Invalid primary key flag at line " + lineNumber + ": " + pkRaw);
        }

        attributes.add(new Attribute(attrName, attrType, Boolean.parseBoolean(pkRaw)));
        lineNumber++;
      }

      throw new TableStorageException("Missing #data section");
    } catch (IOException e) {
      throw new TableStorageException("Unable to read table file: " + path, e);
    }
  }

  /**
   * Creates file for Table
   *
   * @return First line containing a tuple
   */
  private static int createFile(final Path path, final Schema schema) {
    if (Files.exists(path)) {
      throw new TableStorageFileAlreadyExistsException("Tablefile already exists");
    }
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
      throw new RuntimeException("Failed to create file", e);
    }
  }

  private int countLines() {
    try (BufferedReader r = Files.newBufferedReader(path)) {
      int count = 0;
      while (r.readLine() != null) {
        count++;
      }
      return count;
    } catch (IOException e) {
      throw new TableStorageException("Unable to count lines for table file: " + path, e);
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
    final LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    final List<Attribute> attrs = schema.attributes();

    for (int i = 0; i < attrs.size(); i++) {
      final Attribute attr = attrs.get(i);
      final String raw = parts[i];
      final DataType type = DataType.valueOf(attr.type()); // TODO validate
      values.put(attr.name(), parse(raw, type));
    }

    return new Row(values);
  }


}