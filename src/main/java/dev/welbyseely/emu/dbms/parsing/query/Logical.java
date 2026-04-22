package dev.welbyseely.emu.dbms.parsing.query;

public record Logical(Expression left, String op, Expression right) implements Expression {

}
