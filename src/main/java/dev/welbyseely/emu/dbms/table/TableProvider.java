package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import dev.welbyseely.emu.dbms.storage.table.TableStorageProvider;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import java.nio.file.Path;
import java.util.function.Function;

public class TableProvider {

  private static Attribute getPrimaryKeyAttribute(Schema schema) {
  return schema.attributes().stream()
      .filter(Attribute::primaryKey)
      .findFirst()
      .orElse(null);
}

  public static <K extends Comparable<? super K>> Table<K> create(
      Schema schema,
      DataType keyType,
      Function<String, K> keyParser,
      Function<K, String> keySerializer
  ) {
    TableStorage storage = TableStorageProvider.createTableStorage(schema);

    PrimaryIndex<K> index = null;

    if (hasPrimaryKey(schema)) {
      index = new PrimaryIndex<>(
          new BinarySearchTree<>(),
          schema.schemaName() + "_pk",
          keyType,
          keyParser,
          keySerializer
      );
    }

    return new Table<>(schema, storage, index);
  }

  public static <K extends Comparable<? super K>> Table<K> load(
      Path tablePath,
      DataType keyType,
      Function<String, K> keyParser,
      Function<K, String> keySerializer
  ) {
    TableStorage storage = TableStorageProvider.readTableStorage(tablePath);
    Schema schema = storage.getSchema();

    PrimaryIndex<K> index = null;

    if (hasPrimaryKey(schema)) {
      String tableName = schema.schemaName();

      // TODO consider checking index and rebuilding if necessary
      index = new PrimaryIndex<>(
          new BinarySearchTree<>(),
          tableName + "_pk",
          keyType,
          keyParser,
          keySerializer
      );
    }

    return new Table<>(schema, storage, index);
  }

  private static boolean hasPrimaryKey(Schema schema) {
    return schema.attributes().stream().anyMatch(Attribute::primaryKey);
  }


}