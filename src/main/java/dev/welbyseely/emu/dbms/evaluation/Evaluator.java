package dev.welbyseely.emu.dbms.evaluation;

import dev.welbyseely.emu.dbms.exception.DbmsParseException;
import dev.welbyseely.emu.dbms.query.Comparison;
import dev.welbyseely.emu.dbms.query.Expression;
import dev.welbyseely.emu.dbms.query.Logical;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;

public class Evaluator {

  public boolean eval(Expression expr, Row row, Schema schema) {
    return switch (expr) {
      case null -> true; // no WHERE clause

      case Comparison c -> evalComparison(c, row, schema);
      case Logical l -> evalLogical(l, row, schema);
      default -> throw new IllegalStateException("Unknown expression type: " + expr);
    };

  }

  private boolean evalLogical(Logical l, Row row, Schema schema) {
    boolean left = eval(l.left(), row, schema);
    boolean right = eval(l.right(), row, schema);

    return switch (l.op()) {
      case "AND" -> left && right;
      case "OR" -> left || right;
      default -> throw new DbmsParseException("Unknown logical operator: " + l.op());
    };
  }

  private boolean evalComparison(Comparison c, Row row, Schema schema) {
    Object left = resolveValue(c.left(), row, schema);
    Object right = resolveValue(c.right(), row, schema);

    if (left == null || right == null) {
      return false; // simple null handling for now
    }

    return compare(left, c.op(), right);
  }

  private Object resolveValue(String token, Row row, Schema schema) {
    // column reference
    if (hasAttribute(schema, token)) {
      return row.get(token);
    }

    // literal → infer type from schema context (best effort)
    // fallback: try parsing in order
    return parseLiteral(token);
  }

  private boolean hasAttribute(Schema schema, String name) {
    return schema.attributes().stream()
        .anyMatch(attr -> attr.name().equals(name));
  }

  private Object parseLiteral(String raw) {
    // try integer
    try {
      return Integer.parseInt(raw);
    } catch (Exception ignored) {}

    // try double
    try {
      return Double.parseDouble(raw);
    } catch (Exception ignored) {}

    // fallback → string
    return raw;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean compare(Object left, String op, Object right) {
    if (!(left instanceof Comparable l) || !(right instanceof Comparable r)) {
      throw new DbmsParseException("Values not comparable: " + left + ", " + right);
    }

    int cmp = l.compareTo(r);

    return switch (op) {
      case "=" -> cmp == 0;
      case "!=" -> cmp != 0;
      case ">" -> cmp > 0;
      case "<" -> cmp < 0;
      case ">=" -> cmp >= 0;
      case "<=" -> cmp <= 0;
      default -> throw new DbmsParseException("Unknown operator: " + op);
    };
  }
}