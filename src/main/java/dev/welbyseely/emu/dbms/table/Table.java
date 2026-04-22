package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Table {

  private final Schema schema;
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
}