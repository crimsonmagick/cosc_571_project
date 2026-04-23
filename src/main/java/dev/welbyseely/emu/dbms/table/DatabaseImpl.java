package dev.welbyseely.emu.dbms.table;


import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.query.CreateTableQuery;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.commands.results.VoidResult;
import dev.welbyseely.emu.dbms.exception.TableDoesNotExistException;
import dev.welbyseely.emu.dbms.index.PrimaryIndex;
import dev.welbyseely.emu.dbms.commands.query.PreparedQuery;
import dev.welbyseely.emu.dbms.query.QueryEngine;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import dev.welbyseely.emu.dbms.storage.table.TableStorageProvider;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseImpl implements Database {

  private final String dbName;
  private final Path dbPath;
  private final Map<String, Table> cache = new HashMap<>();
  private final QueryEngine queryEngine;

  public DatabaseImpl(final String dbName) {
    this.dbName = dbName;
    this.dbPath = resolveBaseDir().resolve(dbName);
    this.queryEngine = new QueryEngine(this);
  }

  public Table createTable(Schema schema) {
    TableStorage storage = TableStorageProvider.createTableStorage(schema, dbPath);

    PrimaryIndex<?> index = buildIndex(schema);

    return new Table(schema, storage, index);
  }

  public Table getTable(String name) {
    return cache.computeIfAbsent(name, this::loadTable);
  }

  @Override
  public String getName() {
    return dbName;
  }

  @Override
  public Result executeQuery(final PreparedQuery preparedQuery) {
    if (preparedQuery instanceof SelectQuery q) {
      var rows = queryEngine.executeSelect(q);
      return new TupleResult(rows);
    }
    if (preparedQuery instanceof CreateTableQuery ctq) {
      final Schema schema = new Schema(ctq.table(), ctq.attributes());
      createTable(schema);
      return new VoidResult();
    }
    throw new UnsupportedOperationException(
        "Unsupported preparedQuery type, class=" + preparedQuery.getClass());
  }

  private Table loadTable(String name) {
    Path tablePath = dbPath.resolve(name.toLowerCase() + ".tbl");

    if (!Files.exists(tablePath)) {
      throw new TableDoesNotExistException(name);
    }

    TableStorage storage = TableStorageProvider.readTableStorage(tablePath);
    Schema schema = storage.getSchema();

    PrimaryIndex<?> index = buildIndex(schema);

    return new Table(schema, storage, index);
  }

  private PrimaryIndex<?> buildIndex(Schema schema) {
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
          dbPath,
          type,
          Integer::parseInt,
          i -> Integer.toString(i)
      );

      case FLOAT -> new PrimaryIndex<>(
          new BinarySearchTree<>(),
          indexName,
          dbPath,
          type,
          Double::parseDouble,
          d -> Double.toString(d)
      );

      case TEXT -> new PrimaryIndex<>(
          new BinarySearchTree<String, RecordPointer>(),
          indexName,
          dbPath,
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
