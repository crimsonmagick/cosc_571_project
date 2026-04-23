package dev.welbyseely.emu.dbms.query;

import dev.welbyseely.emu.dbms.commands.query.Aggregate;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.evaluation.Evaluator;
import dev.welbyseely.emu.dbms.exception.InvalidProjectionException;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.table.RowEntry;
import dev.welbyseely.emu.dbms.table.Database;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class QueryEngine {

    private final Database database;

    public QueryEngine(final Database database) {
        this.database = database;
    }

    private final Evaluator evaluator = new Evaluator();

    private List<Row> executeAggregateSelect(SelectQuery q) {
        Table table = database.getTable(q.table());
        Schema schema = table.getSchema();

        Evaluator evaluator = new Evaluator();
        Aggregate agg = q.aggregate();

        if (!agg.column().equals("*") && !schema.hasAttribute(agg.column())) {
            throw new RuntimeException("Unknown column: " + agg.column());
        }

        List<Row> matches = new ArrayList<>();
        for (RowEntry entry : table.scan()) {
            if (evaluator.eval(q.where(), entry.row(), schema)) {
                matches.add(entry.row());
            }
        }

        Row resultRow = switch (agg.type()) {
            case COUNT -> computeCount(matches);
            case MIN -> computeMin(matches, agg.column(), schema);
            case MAX -> computeMax(matches, agg.column(), schema);
            case AVERAGE -> computeAverage(matches, agg.column(), schema);
        };

        return List.of(resultRow);
    }

    private Row computeCount(List<Row> rows) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("count", rows.size());
        return new Row(values);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Row computeMin(List<Row> rows, String column, Schema schema) {
        Object min = null;

        for (Row row : rows) {
            Object value = row.get(column);
            if (value == null) continue;

            if (min == null || ((Comparable) value).compareTo(min) < 0) {
                min = value;
            }
        }

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("min", min);
        return new Row(values);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Row computeMax(List<Row> rows, String column, Schema schema) {
        Object max = null;

        for (Row row : rows) {
            Object value = row.get(column);
            if (value == null) continue;

            if (max == null || ((Comparable) value).compareTo(max) > 0) {
                max = value;
            }
        }

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("max", max);
        return new Row(values);
    }

    private Row computeAverage(List<Row> rows, String column, Schema schema) {
        DataType type = DataType.valueOf(schema.getAttribute(column).type());
        if (type != DataType.INTEGER && type != DataType.FLOAT) {
            throw new RuntimeException("AVERAGE requires numeric column: " + column);
        }

        double sum = 0.0;
        int count = 0;

        for (Row row : rows) {
            Object value = row.get(column);
            if (value instanceof Number n) {
                sum += n.doubleValue();
                count++;
            }
        }

        Double avg = count == 0 ? null : sum / count;

        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("average", avg);
        return new Row(values);
    }

    public List<Row> executeSelect(SelectQuery query) {
        if (query.aggregate() != null) {
            return executeAggregateSelect(query);
        }
        Table table = database.getTable(query.table());

        for (String col : query.columns()) {
            if (!col.equals("*") && !table.getSchema().hasAttribute(col)) {
                throw new InvalidProjectionException("Unknown column: " + col);
            }
        }

        List<Row> results = new ArrayList<>();

        for (RowEntry rowEntry : table.scan()) {
            Row row = rowEntry.row();
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

        var projected = new java.util.LinkedHashMap<String, Object>();

        for (String col : columns) {
            projected.put(col, row.get(col));
        }

        return new Row(projected);
    }
}