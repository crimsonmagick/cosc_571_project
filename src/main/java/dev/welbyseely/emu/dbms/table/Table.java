package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.RowEntry;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Table {

  private Schema schema;
  private final TableStorage tableStorage;
  private final PrimaryIndex<?> index;

  public Table(
      final Schema schema,
      final TableStorage tableStorage,
      final PrimaryIndex<?> primaryIndex
  ) {
    this.schema = schema;
    this.tableStorage = tableStorage;
    this.index = primaryIndex;
  }

  public Iterable<RowEntry> scan() {
    List<RowEntry> rows = new ArrayList<>();

    for (RecordPointer pointer : scanPointers()) {
      Row row = tableStorage.read(pointer);
      if (row != null) {
        rows.add(new RowEntry(pointer, row));
      }
    }

    return rows;
  }

  @SuppressWarnings("unchecked")
  public Optional<Row> getByPrimaryKey(final Object key) {
    if (index == null) {
      return Optional.empty();
    }

    if (!(key instanceof Comparable<?> comparable)) {
      throw new IllegalArgumentException("Primary key must be Comparable: " + key);
    }

    return ((PrimaryIndex<Comparable<Object>>) index)
        .search((Comparable<Object>) comparable)
        .map(tableStorage::read);
  }

  public RecordPointer insert(final Row row) {
    if (index != null) {
      final Comparable<?> primaryKey = extractPrimaryKey(row);
      if (index.searchUntyped(primaryKey).isPresent()) {
        throw new IllegalStateException("Duplicate primary key: " + primaryKey);
      }
    }

    final RecordPointer pointer = tableStorage.insert(row);

    if (index != null) {
      final Comparable<?> key = extractPrimaryKey(row);
      index.insertUntyped(key, pointer);
    }

    return pointer;
  }

  private Iterable<RecordPointer> scanPointers() {
    if (index != null) {
      return index.scan();
    } else {
      return tableStorage.scan();
    }
  }

  private Comparable<?> extractPrimaryKey(final Row row) {
    final Attribute primaryKeyAttribute = schema.attributes()
        .stream()
        .filter(Attribute::primaryKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Table has no primary key"));

    final Comparable<?> value = (Comparable<?>) row.get(primaryKeyAttribute.name());
    if (value == null) {
      throw new IllegalStateException(
          "Primary key value is null for attribute " + primaryKeyAttribute.name());
    }

    return value;
  }

  public Schema getSchema() {
    return schema;
  }

  public void delete(RecordPointer pointer) {
    Row row = tableStorage.read(pointer);
    if (row == null) {
      return;
    }

    if (index != null) {
      Comparable<?> key = extractPrimaryKey(row);
      index.deleteUntyped(key);
    }

    tableStorage.delete(pointer);
  }

  public void update(RecordPointer pointer, Row newRow) {

    if (index != null) {
      Row oldRow = tableStorage.read(pointer);

      Comparable<?> oldKey = extractPrimaryKey(oldRow);
      Comparable<?> newKey = extractPrimaryKey(newRow);

      if (!oldKey.equals(newKey)) {

        if (index.searchUntyped(newKey).isPresent()) {
          throw new IllegalStateException("Duplicate primary key: " + newKey);
        }

        index.deleteUntyped(oldKey);
        index.insertUntyped(newKey, pointer);
      }
    }

    tableStorage.update(pointer, newRow);
  }

  public void drop() {
    if (index != null) {
      index.removeIndex();
    }

    tableStorage.remove();
  }

  public void rename(List<String> newNames) {

    List<Attribute> oldAttrs = schema.attributes();

    if (oldAttrs.size() != newNames.size()) {
      throw new RuntimeException(
          "Attribute count mismatch: expected " + oldAttrs.size()
              + " but got " + newNames.size());
    }

    List<Attribute> newAttrs = new ArrayList<>();

    for (int i = 0; i < oldAttrs.size(); i++) {
      Attribute old = oldAttrs.get(i);

      newAttrs.add(new Attribute(
          newNames.get(i),
          old.type(),
          old.primaryKey()
      ));
    }

    Schema newSchema = new Schema(schema.schemaName(), newAttrs);

    tableStorage.rewriteSchema(newSchema);

    this.schema = newSchema;
  }
}