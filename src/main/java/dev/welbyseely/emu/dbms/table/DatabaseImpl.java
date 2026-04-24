package dev.welbyseely.emu.dbms.table;


import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.query.CreateTableQuery;
import dev.welbyseely.emu.dbms.commands.query.DeleteQuery;
import dev.welbyseely.emu.dbms.commands.query.DescribeQuery;
import dev.welbyseely.emu.dbms.commands.query.InsertQuery;
import dev.welbyseely.emu.dbms.commands.query.LetQuery;
import dev.welbyseely.emu.dbms.commands.query.RenameQuery;
import dev.welbyseely.emu.dbms.commands.query.UpdateQuery;
import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.exception.TableAlreadyExistsException;
import dev.welbyseely.emu.dbms.exception.TableDoesNotExistException;
import dev.welbyseely.emu.dbms.exception.TableStorageFileAlreadyExistsException;
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
import java.util.ArrayList;
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
    preloadSchemas();
  }

  private static Attribute getPrimaryKeyAttribute(Schema schema) {
    return schema.attributes().stream()
        .filter(Attribute::primaryKey)
        .findFirst()
        .orElse(null);
  }

  public Table createTable(Schema schema) {
    final TableStorage storage;
    try {
      storage = TableStorageProvider.createTableStorage(schema, dbPath);
    } catch (TableStorageFileAlreadyExistsException e) {
      throw new TableAlreadyExistsException("Table " + schema.schemaName() + " already exists", e);
    }

    PrimaryIndex<?> index = buildIndex(schema);

    return new Table(schema, storage, index);
  }

  public Table getTable(String name) {
    return cache.computeIfAbsent(name.toLowerCase(), this::loadTable);
  }

  @Override
  public String getName() {
    return dbName;
  }

  @Override
  public Result executeQuery(final PreparedQuery preparedQuery) {
    if (preparedQuery instanceof SelectQuery q) {
      return queryEngine.executeSelect(q);
    }
    if (preparedQuery instanceof CreateTableQuery(String table, List<Attribute> attributes)) {
      final Schema schema = new Schema(table, attributes);
      cache.put(schema.schemaName().toLowerCase(), createTable(schema));
      return new MessageResult("Created table with name " + table);
    }
    if (preparedQuery instanceof InsertQuery(String tableName, List<String> values)) {
      return queryEngine.executeInsertQuery(tableName, values);
    }
    if (preparedQuery instanceof DescribeQuery dq) {
      if (dq.all()) {
        List<Schema> schemas = loadAllSchemas();
        return new MessageResult(formatDescribeAll(schemas));
      }

      Table table = getTable(dq.table());
      return new MessageResult(formatDescribe(table.getSchema()));
    }
    if (preparedQuery instanceof UpdateQuery uq) {
      return queryEngine.executeUpdateQuery(uq);
    }
    if (preparedQuery instanceof DeleteQuery dq) {
      queryEngine.executeDeleteQuery(dq);
    }
    if (preparedQuery instanceof RenameQuery rq) {
      Table table = getTable(rq.table());
      table.rename(rq.newNames());
      return new MessageResult(
          "Renamed table values of table " + rq.table() + " to " + rq.newNames());
    }
    if (preparedQuery instanceof LetQuery lq) {

      // run SELECT
      List<Row> rows = queryEngine.executeSelect(lq.select()).tuples();

      List<String> columns = lq.select().columns();

      // validate key exists
      if (!columns.contains(lq.key())) {
        throw new RuntimeException("KEY must be one of selected attributes");
      }

      List<Attribute> attrs = new ArrayList<>();

      for (String col : columns) {
        DataType type = resolveColumnType(lq.select(), col);
        boolean isPk = col.equals(lq.key());

        attrs.add(new Attribute(col, type.name(), isPk));
      }

      Schema schema = new Schema(lq.table(), attrs);

      Table table = createTable(schema);
      cache.put(schema.schemaName(), table);

      for (Row row : rows) {
        table.insert(row);
      }

      return new MessageResult("Let Query, created table " + table.getSchema().schemaName());
    }
    throw new UnsupportedOperationException(
        "Unsupported preparedQuery type, class=" + preparedQuery.getClass());
  }

  // FIXME code smell - class responsibilities aren't properly bounded
  @Override
  public Map<String, Table> getCache() {
    return cache;
  }

  private DataType resolveColumnType(SelectQuery select, String column) {

    DataType foundType = null;

    for (String tableName : select.tables()) {
      Table table = getTable(tableName);
      Schema schema = table.getSchema();

      if (schema.hasAttribute(column)) {
        DataType type = DataType.valueOf(schema.getAttribute(column).type());

        if (foundType != null) {
          throw new RuntimeException("Ambiguous column: " + column);
        }

        foundType = type;
      }
    }

    if (foundType == null) {
      throw new RuntimeException("Unknown column: " + column);
    }

    return foundType;
  }

  private String formatDescribe(Schema schema) {
    StringBuilder sb = new StringBuilder();

    sb.append(schema.schemaName().toUpperCase()).append("\n");

    for (Attribute attr : schema.attributes()) {
      sb.append(attr.name().toUpperCase())
          .append(": ")
          .append(formatType(attr.type()));

      if (attr.primaryKey()) {
        sb.append(" PRIMARY KEY");
      }

      sb.append("\n");
    }

    return sb.toString().trim();
  }

  private String formatDescribeAll(List<Schema> schemas) {
    return schemas.stream()
        .map(this::formatDescribe)
        .reduce((a, b) -> a + "\n\n" + b)
        .orElse("");
  }

  private String formatType(String type) {
    return switch (type.toUpperCase()) {
      case "INTEGER" -> "Integer";
      case "FLOAT" -> "Float";
      case "TEXT" -> "Text";
      default -> type;
    };
  }

  private List<Schema> loadAllSchemas() {
    try (var stream = Files.list(dbPath)) {
      return stream
          .filter(p -> p.getFileName().toString().endsWith(".tbl"))
          .map(TableStorageProvider::readTableStorage)
          .map(TableStorage::getSchema)
          .toList();
    } catch (Exception e) {
      throw new RuntimeException("Failed to list tables in " + dbPath, e);
    }
  }

  private Table loadTable(String name) {
    Path tablePath = dbPath.resolve(name.toLowerCase() + ".tbl");

    if (!Files.exists(tablePath)) {
      throw new TableDoesNotExistException(name);
    }

    TableStorage storage = TableStorageProvider.readTableStorage(tablePath);
    Schema schema = storage.getSchema();

    PrimaryIndex<?> index = buildIndex(schema);

    Table table = new Table(schema, storage, index);
    cache.put(schema.schemaName(), table);
    return table;
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

  private void preloadSchemas() {
    List<TableStorage> storages = TableStorageProvider.readTablesInDatabase(dbPath);

    for (TableStorage storage : storages) {
      Schema schema = storage.getSchema();

      Table table = new Table(schema, storage, buildIndex(schema));

      cache.put(schema.schemaName().toLowerCase(), table);
    }
  }


}
