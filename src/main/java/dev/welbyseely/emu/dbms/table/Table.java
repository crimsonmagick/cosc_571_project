package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Table<K extends Comparable<? super K>> {

  private final Schema schema;
  private final TableStorage tableStorage;
  private final PrimaryIndex<K> index;

  public Table(
      final Schema schema,
      final TableStorage tableStorage,
      final PrimaryIndex<K> primaryIndex
  ) {
    this.schema = schema;
    this.tableStorage = tableStorage;
    this.index = primaryIndex;
  }

  public Iterable<Row> scan() {
    final List<Row> rows = new ArrayList<>();

    for (RecordPointer pointer : scanPointers()) {
      final Row row = tableStorage.read(pointer);
      if (row != null) {
        rows.add(row);
      }
    }

    return rows;
  }

  public Optional<Row> getByPrimaryKey(final K key) {
    if (index == null) {
      return Optional.empty();
    }

    return index.search(key)
        .map(tableStorage::read);
  }

  public RecordPointer insert(final Row row) {
    if (index != null) {
      final K primaryKey = extractPrimaryKey(row);
      if (index.search(primaryKey).isPresent()) {
        throw new IllegalStateException("Duplicate primary key: " + primaryKey);
      }
    }

    final RecordPointer pointer = tableStorage.insert(row);

    if (index != null) {
      final K primaryKey = extractPrimaryKey(row);
      index.insert(primaryKey, pointer);
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

  @SuppressWarnings("unchecked")
  private K extractPrimaryKey(final Row row) {
    final Attribute primaryKeyAttribute = schema.attributes()
        .stream()
        .filter(Attribute::primaryKey)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Table has no primary key"));

    final Object value = row.get(primaryKeyAttribute.name());
    if (value == null) {
      throw new IllegalStateException(
          "Primary key value is null for attribute " + primaryKeyAttribute.name());
    }

    return (K) value;
  }
}