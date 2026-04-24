package dev.welbyseely.emu.dbms.query;

import static dev.welbyseely.emu.dbms.util.DatatypeParser.parse;

import dev.welbyseely.emu.dbms.commands.query.Aggregate;
import dev.welbyseely.emu.dbms.commands.query.DeleteQuery;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.commands.query.UpdateQuery;
import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.evaluation.Evaluator;
import dev.welbyseely.emu.dbms.exception.InvalidProjectionException;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.storage.table.RowEntry;
import dev.welbyseely.emu.dbms.table.Database;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class QueryEngine {

  private final Database database;
  private final Evaluator evaluator = new Evaluator();

  public QueryEngine(final Database database) {
    this.database = database;
  }

  public TupleResult executeSelect(SelectQuery query) {
    List<Row> rows;

    // Fast path: PK lookup
    // !IMPORTANT
    if (query.tables().size() == 1) {
      Table table = database.getTable(query.tables().getFirst());

      if (isPrimaryKeyEquality(query.where(), table)) {
        Comparison c = (Comparison) query.where();
        Object key = primaryKeyValue(c, table);

        rows = table.getByPrimaryKey(key)
            .map(List::of)
            .orElse(List.of());
      } else {
        rows = buildRows(query);
      }
    } else {
      rows = buildRows(query);
    }

    if (query.aggregate() != null) {
      return new TupleResult(List.of(executeAggregate(rows, query)));
    }

    validateProjection(query, rows);

    List<Row> results = new ArrayList<>();

    for (Row row : rows) {
      results.add(project(row, query.columns()));
    }

    return new TupleResult(results);
  }

  public MessageResult executeInsertQuery(String tableName, List<String> values) {
    final Table table = database.getTable(tableName);
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
    return new MessageResult("Inserted values successfully.");
  }

  public MessageResult executeUpdateQuery(UpdateQuery uq) {

    Table table = database.getTable(uq.table());
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

    return new MessageResult("Update succseful");

  }

  public MessageResult executeDeleteQuery(DeleteQuery dq) {

    Table table = database.getTable(dq.table());
    Schema schema = table.getSchema();
    Evaluator evaluator = new Evaluator();

    if (dq.where() == null) {
      table.drop();
      database.getCache().remove(dq.table().toLowerCase());
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


  private List<Row> buildCartesianProduct(List<Table> tables) {
    List<Row> result = List.of(new Row(new LinkedHashMap<>()));

    for (Table table : tables) {
      List<Row> next = new ArrayList<>();

      for (Row base : result) {
        for (RowEntry entry : table.scan()) {
          next.add(combine(base, entry.row()));
        }
      }

      result = next;
    }

    return result;
  }

  private List<Row> buildRows(SelectQuery q) {
    List<Table> tables = q.tables().stream().map(database::getTable).toList();

    List<Row> rows = buildCartesianProduct(tables);
    Schema schema = combineSchemas(tables);

    List<Row> filtered = new ArrayList<>();

    for (Row row : rows) {
      if (q.where() == null || evaluator.eval(q.where(), row, schema)) {
        filtered.add(row);
      }
    }

    return filtered;
  }

  private Row combine(Row left, Row right) {
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();

    values.putAll(left.values());
    values.putAll(right.values());

    return new Row(values);
  }

  private Schema combineSchemas(List<Table> tables) {
    List<Attribute> attrs = new ArrayList<>();

    for (Table t : tables) {
      attrs.addAll(t.getSchema().attributes());
    }

    return new Schema("joined", attrs);
  }

  private Row computeAverage(List<Row> rows, String column, List<Row> allRows) {
    double sum = 0.0;
    int count = 0;

    for (Row row : rows) {
      Object value = row.get(column);
      if (value instanceof Number n) {
        sum += n.doubleValue();
        count++;
      } else if (value != null) {
        throw new RuntimeException("AVERAGE requires numeric column: " + column);
      }
    }

    Double avg = count == 0 ? null : sum / count;

    return new Row(Map.of("average", avg));
  }

  private Row computeCount(List<Row> rows) {
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put("count", rows.size());
    return new Row(values);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Row computeMax(List<Row> rows, String column) {
    if (!rows.isEmpty() && !rows.get(0).values().containsKey(column)) {
      throw new RuntimeException("Unknown column: " + column);
    }

    Object max = null;

    for (Row row : rows) {
      Object value = row.get(column);
      if (value == null) {
        continue;
      }

      if (max == null || ((Comparable) value).compareTo(max) > 0) {
        max = value;
      }
    }

    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put("max", max);

    return new Row(values);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Row computeMin(List<Row> rows, String column) {
    if (!rows.isEmpty() && !rows.get(0).values().containsKey(column)) {
      throw new RuntimeException("Unknown column: " + column);
    }

    Object min = null;

    for (Row row : rows) {
      Object value = row.get(column);
      if (value == null) {
        continue;
      }

      if (min == null || ((Comparable) value).compareTo(min) < 0) {
        min = value;
      }
    }

    return new Row(Map.of("min", min));
  }

  private Row executeAggregate(List<Row> rows, SelectQuery q) {
    Aggregate agg = q.aggregate();

    return switch (agg.type()) {
      case COUNT -> computeCount(rows);
      case MIN -> computeMin(rows, agg.column());
      case MAX -> computeMax(rows, agg.column());
      case AVERAGE -> computeAverage(rows, agg.column(), rows);
    };
  }

  private boolean isPrimaryKeyEquality(Expression where, Table table) {
    if (!(where instanceof Comparison(String left, String op, String right))) {
      return false;
    }

    if (!op.equals("=")) {
      return false;
    }

    Optional<String> pk = table.getSchema().getPrimaryKeyName();
    if (pk.isEmpty()) {
      return false;
    }

    if (!left.equalsIgnoreCase(pk.get())) {
      return false;
    }

    return !table.getSchema().hasAttribute(right);
  }

  private Object primaryKeyValue(Comparison c, Table table) {
    String pkName = table.getSchema()
        .getPrimaryKeyName()
        .orElseThrow();

    Attribute pkAttr = table.getSchema().getAttribute(pkName);
    DataType type = DataType.valueOf(pkAttr.type());

    return dev.welbyseely.emu.dbms.util.DatatypeParser.parse(c.right(), type);
  }

  private Row project(Row row, List<String> columns) {
    if (columns.size() == 1 && columns.get(0).equals("*")) {
      return row;
    }

    var projected = new LinkedHashMap<String, Object>();

    for (String col : columns) {
      projected.put(col, row.get(col));
    }

    return new Row(projected);
  }

  private void validateProjection(SelectQuery query, List<Row> rows) {
    if (query.columns().size() == 1 && query.columns().get(0).equals("*")) {
      return;
    }

    if (rows.isEmpty()) {
      return;
    }

    Row sample = rows.get(0);

    for (String col : query.columns()) {
      if (!sample.values().containsKey(col)) {
        throw new InvalidProjectionException("Unknown column: " + col);
      }
    }
  }
}