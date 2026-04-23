package dev.welbyseely.emu.dbms.evaluation;

import dev.welbyseely.emu.dbms.exception.DbmsParseException;
import dev.welbyseely.emu.dbms.exception.InvalidConditionException;
import dev.welbyseely.emu.dbms.query.Comparison;
import dev.welbyseely.emu.dbms.query.Expression;
import dev.welbyseely.emu.dbms.query.Logical;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.util.DatatypeParser;

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
    final Object left = resolveAttributeValue(c.left(), row, schema);
    final Object right;
    if (schema.hasAttribute(c.right())) {
      right = resolveAttributeValue(c.right(), row, schema);
    } else {
      final DataType dataType = DataType.valueOf(schema.getAttribute(c.left()).type());
      right = DatatypeParser.parse(c.right(), dataType);
    }

    if (left == null || right == null) {
      return false;
    }

    return compare(left, c.op(), right);
  }

  private Object resolveAttributeValue(String token, Row row, Schema schema) {
    if (schema.hasAttribute(token)) {
      return row.get(token);
    }

    throw new InvalidConditionException("Unknown column: " + token);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private boolean compare(Object left, String op, Object right) {

    // coercion
    if (left instanceof Number && right instanceof Number) {
      double ld = ((Number) left).doubleValue();
      double rd = ((Number) right).doubleValue();

      return switch (op) {
        case "=" -> ld == rd;
        case "!=" -> ld != rd;
        case ">" -> ld > rd;
        case "<" -> ld < rd;
        case ">=" -> ld >= rd;
        case "<=" -> ld <= rd;
        default -> throw new DbmsParseException("Unknown operator: " + op);
      };
    }

    if (!(left instanceof Comparable l) || !(right instanceof Comparable r)
        || !l.getClass().equals(r.getClass())) {
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