package dev.welbyseely.emu.dbms.query;

import dev.welbyseely.emu.dbms.evaluation.Evaluator;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;

import dev.welbyseely.emu.dbms.table.TableManager;
import java.util.ArrayList;
import java.util.List;

public class QueryEngine {

  private final TableManager tableManager;

  public QueryEngine(final TableManager tableManager) {
    this.tableManager = tableManager;
  }

  private final Evaluator evaluator = new Evaluator();

  public List<Row> executeSelect(SelectQuery query) {
    Table table = tableManager.getTable(query.table());

    List<Row> results = new ArrayList<>();

    for (Row row : table.scan()) {
      if (query.where() == null ||
          evaluator.eval(query.where(), row, table.getSchema())) {

        results.add(project(row, query.columns()));
      }
    }

    return results;
  }

  private Row project(Row row, List<String> columns) {
    if (columns.size() == 1 && columns.get(0).equals("*")) {
      return row;
    }

    var projected = new java.util.HashMap<String, Object>();

    for (String col : columns) {
      projected.put(col, row.get(col));
    }

    return new Row(projected);
  }
}