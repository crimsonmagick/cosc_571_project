package dev.welbyseely.emu.dbms.table;


import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;
import static dev.welbyseely.emu.dbms.util.DatatypeParser.parse;

import dev.welbyseely.emu.dbms.commands.query.CreateTableQuery;
import dev.welbyseely.emu.dbms.commands.query.DeleteQuery;
import dev.welbyseely.emu.dbms.commands.query.DescribeQuery;
import dev.welbyseely.emu.dbms.commands.query.InsertQuery;
import dev.welbyseely.emu.dbms.commands.query.LetQuery;
import dev.welbyseely.emu.dbms.commands.query.RenameQuery;
import dev.welbyseely.emu.dbms.commands.query.UpdateQuery;
import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.commands.results.VoidResult;
import dev.welbyseely.emu.dbms.evaluation.Evaluator;
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
import dev.welbyseely.emu.dbms.storage.table.RowEntry;
import dev.welbyseely.emu.dbms.storage.table.TableStorage;
import dev.welbyseely.emu.dbms.storage.table.TableStorageProvider;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      var rows = queryEngine.executeSelect(q);
      return new TupleResult(rows);
    }
    if (preparedQuery instanceof CreateTableQuery(String table, List<Attribute> attributes)) {
      final Schema schema = new Schema(table, attributes);
      cache.put(schema.schemaName().toLowerCase(), createTable(schema));
      return new VoidResult();
    }
    if (preparedQuery instanceof InsertQuery(String tableName, List<String> values)) {
      final Table table = getTable(tableName);
      final Schema schema = table.getSchema();

      final List<Attribute> attrs = schema.attributes();

      if (values.size() != attrs.size()) {
        throw new RuntimeException(
            "Value count does not match schema. Expected " + attrs.size() +
                " but got " + values.size());
      }

      final LinkedHashMap<String, Object> rowValues = new LinkedHashMap<>();

      for (int i = 0; i < attrs.size(); i++) {
        Attribute attr = attrs.get(i);
        String raw = values.get(i);

        DataType type = DataType.valueOf(attr.type());
        Object parsed = parse(raw, type);

        rowValues.put(attr.name(), parsed);
      }

      Row row = new Row(rowValues);
      table.insert(row);
      return new VoidResult();
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

      Table table = getTable(uq.table());
      Schema schema = table.getSchema();
      Evaluator evaluator = new Evaluator();

      // Phase 1: collect updates
      List<RowEntry> matches = new ArrayList<>();
      List<Row> newRows = new ArrayList<>();

      for (RowEntry entry : table.scan()) {
        Row row = entry.row();

        if (evaluator.eval(uq.where(), row, schema)) {

          Row newRow = new Row(new LinkedHashMap<>(row.values()));

          for (var e : uq.updates().entrySet()) {
            Attribute attr = schema.getAttribute(e.getKey());
            Object parsed = parse(e.getValue(), DataType.valueOf(attr.type()));
            newRow.values().put(attr.name(), parsed);
          }

          matches.add(entry);
          newRows.add(newRow);
        }
      }

      // Phase 2: validate PK constraints
      Attribute pkAttr = schema.attributes().stream()
          .filter(Attribute::primaryKey)
          .findFirst()
          .orElse(null);

      if (pkAttr != null) {

        Set<Object> newKeys = new HashSet<>();

        Set<Object> oldKeys = new HashSet<>();

        for (int i = 0; i < matches.size(); i++) {
          Row oldRow = matches.get(i).row();
          Row newRow = newRows.get(i);

          Object oldKey = oldRow.get(pkAttr.name());
          Object newKey = newRow.get(pkAttr.name());

          oldKeys.add(oldKey);

          // duplicate within update set
          if (!newKeys.add(newKey)) {
            throw new RuntimeException("Duplicate primary key: " + newKey);
          }
        }

        // Check conflicts with existing rows NOT being updated
        for (Object newKey : newKeys) {
          var existing = table.getByPrimaryKey(newKey);

          if (existing.isPresent()) {
            Object existingKey = existing.get().get(pkAttr.name());

            // if it's not one of the rows we're replacing → conflict
            if (!oldKeys.contains(existingKey)) {
              throw new RuntimeException("Duplicate primary key: " + newKey);
            }
          }
        }
      }

      // Phase 3: apply updates
      for (int i = 0; i < matches.size(); i++) {
        RowEntry entry = matches.get(i);
        Row newRow = newRows.get(i);

        table.update(entry.pointer(), newRow);
      }

      return new VoidResult();
    }
    if (preparedQuery instanceof DeleteQuery dq) {

      Table table = getTable(dq.table());
      Schema schema = table.getSchema();
      Evaluator evaluator = new Evaluator();

      if (dq.where() == null) {
        table.drop();
        cache.remove(dq.table().toLowerCase());
        return new MessageResult("Deleted table " + table.getSchema().schemaName());
      }

      List<RecordPointer> toDelete = new ArrayList<>();

      for (RowEntry entry : table.scan()) {
        if (evaluator.eval(dq.where(), entry.row(), schema)) {
          toDelete.add(entry.pointer());
        }
      }

      for (RecordPointer ptr : toDelete) {
        table.delete(ptr);
      }

      return new MessageResult("Deleted from table " + table.getSchema().schemaName());
    }
    if (preparedQuery instanceof RenameQuery rq) {
      Table table = getTable(rq.table());
      table.rename(rq.newNames());
      return new VoidResult();
    }
    if (preparedQuery instanceof LetQuery lq) {

      // run SELECT
      List<Row> rows = queryEngine.executeSelect(lq.select());

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

      return new VoidResult();
    }
    throw new UnsupportedOperationException(
        "Unsupported preparedQuery type, class=" + preparedQuery.getClass());
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

  private static Attribute getPrimaryKeyAttribute(Schema schema) {
    return schema.attributes().stream()
        .filter(Attribute::primaryKey)
        .findFirst()
        .orElse(null);
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
