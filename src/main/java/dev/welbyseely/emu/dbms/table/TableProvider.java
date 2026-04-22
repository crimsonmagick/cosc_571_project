package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import dev.welbyseely.emu.dbms.storage.table.TableStorageProvider;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import java.nio.file.Path;

public class TableProvider {

  public static Table create(Schema schema) {
    TableStorage storage = TableStorageProvider.createTableStorage(schema);

    PrimaryIndex<?> index = buildIndex(schema);

    return new Table(schema, storage, index);
  }

  public static Table load(Path tablePath) {
    TableStorage storage = TableStorageProvider.readTableStorage(tablePath);
    Schema schema = storage.getSchema();

    PrimaryIndex<?> index = buildIndex(schema);

    return new Table(schema, storage, index);
  }

  private static PrimaryIndex<?> buildIndex(Schema schema) {
    Attribute pk = getPrimaryKeyAttribute(schema);
    if (pk == null) {
      return null;
    }

    String indexName = schema.schemaName() + "_pk";
    DataType type = DataType.valueOf(pk.type());

    return switch (type) {
      case INTEGER -> new PrimaryIndex<>(
          new BinarySearchTree<Integer, RecordPointer>(),
          indexName,
          type,
          Integer::parseInt,
          i -> Integer.toString(i)
      );

      case FLOAT -> new PrimaryIndex<>(
          new BinarySearchTree<>(),
          indexName,
          type,
          Double::parseDouble,
          d -> Double.toString(d)
      );

      case TEXT -> new PrimaryIndex<>(
          new BinarySearchTree<String, RecordPointer>(),
          indexName,
          type,
          s -> s,
          s -> s
      );
    };
  }

  private static Attribute getPrimaryKeyAttribute(Schema schema) {
    return schema.attributes().stream()
        .filter(Attribute::primaryKey)
        .findFirst()
        .orElse(null);
  }
}