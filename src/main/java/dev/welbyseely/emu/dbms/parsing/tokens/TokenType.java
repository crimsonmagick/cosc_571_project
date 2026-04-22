package dev.welbyseely.emu.dbms.parsing.tokens;

public enum TokenType {
  SELECT, FROM, WHERE,
  IDENTIFIER, NUMBER, STRING,
  COMMA,
  EQ, NEQ, LT, GT, LTE, GTE,
  AND, OR,
  ASTERISK
}
