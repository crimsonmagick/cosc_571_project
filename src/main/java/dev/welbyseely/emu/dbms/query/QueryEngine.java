package dev.welbyseely.emu.dbms.query;

import dev.welbyseely.emu.dbms.commands.query.Aggregate;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.evaluation.Evaluator;
import dev.welbyseely.emu.dbms.exception.InvalidProjectionException;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RowEntry;
import dev.welbyseely.emu.dbms.table.Database;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryEngine {

  private final Database database;

  public QueryEngine(final Database database) {
    this.database = database;
  }

  private final Evaluator evaluator = new Evaluator();

  private Row executeAggregate(List<Row> rows, SelectQuery q) {
    Aggregate agg = q.aggregate();

    return switch (agg.type()) {
      case COUNT -> computeCount(rows);
      case MIN -> computeMin(rows, agg.column());
      case MAX -> computeMax(rows, agg.column());
      case AVERAGE -> computeAverage(rows, agg.column(), rows);
    };
  }

  private Row computeCount(List<Row> rows) {
    LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    values.put("count", rows.size());
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

  public List<Row> executeSelect(SelectQuery query) {
    List<Row> rows = buildRows(query);

    if (query.aggregate() != null) {
      return List.of(executeAggregate(rows, query));
    }

    validateProjection(query, rows);

    List<Row> results = new ArrayList<>();

    for (Row row : rows) {
      results.add(project(row, query.columns()));
    }

    return results;
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


  private Row project(Row row, List<String> columns) {
    if (columns.size() == 1 && columns.get(0).equals("*")) {
      return row;
    }

    var projected = new java.util.LinkedHashMap<String, Object>();

    for (String col : columns) {
      projected.put(col, row.get(col));
    }

    return new Row(projected);
  }
}