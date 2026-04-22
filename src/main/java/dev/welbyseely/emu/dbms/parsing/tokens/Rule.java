package dev.welbyseely.emu.dbms.parsing.tokens;

import java.util.List;
import java.util.regex.Pattern;

record Rule(Pattern pattern, TokenType type) {

  static final List<Rule> RULES = List.of(
      new Rule(Pattern.compile("^\\s+"), null), // skip whitespace

      new Rule(Pattern.compile("^SELECT\\b", Pattern.CASE_INSENSITIVE), TokenType.SELECT),
      new Rule(Pattern.compile("^FROM\\b", Pattern.CASE_INSENSITIVE), TokenType.FROM),
      new Rule(Pattern.compile("^WHERE\\b", Pattern.CASE_INSENSITIVE), TokenType.WHERE),
      new Rule(Pattern.compile("^AND\\b", Pattern.CASE_INSENSITIVE), TokenType.AND),
      new Rule(Pattern.compile("^OR\\b", Pattern.CASE_INSENSITIVE), TokenType.OR),

      new Rule(Pattern.compile("^\\*"), TokenType.ASTERISK),
      new Rule(Pattern.compile("^,"), TokenType.COMMA),

      new Rule(Pattern.compile("^!="), TokenType.NEQ),
      new Rule(Pattern.compile("^>="), TokenType.GTE),
      new Rule(Pattern.compile("^<="), TokenType.LTE),
      new Rule(Pattern.compile("^="), TokenType.EQ),
      new Rule(Pattern.compile("^>"), TokenType.GT),
      new Rule(Pattern.compile("^<"), TokenType.LT),

      new Rule(Pattern.compile("^\"([^\"]*)\""), TokenType.STRING),
      new Rule(Pattern.compile("^\\d+(\\.\\d+)?"), TokenType.NUMBER),

      new Rule(Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*"), TokenType.IDENTIFIER)
  );

}
