package dev.welbyseely.emu.dbms.parsing.query;

public record Comparison(String left, String op, String right) implements Expression {

}
