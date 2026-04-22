package dev.welbyseely.emu.dbms.parsing.tokens;

import static dev.welbyseely.emu.dbms.parsing.tokens.Rule.RULES;

import dev.welbyseely.emu.dbms.exception.DbmsParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Tokenizer {

  public List<Token> tokenize(String input) {
    List<Token> tokens = new ArrayList<>();

    while (!input.isEmpty()) {
      boolean matched = false;

      for (Rule rule : RULES) {
        Matcher m = rule.pattern().matcher(input);
        if (m.find()) {
          matched = true;

          String lexeme = m.group();
          input = input.substring(lexeme.length());

          if (rule.type() != null) {
            String value = rule.type() == TokenType.STRING
                ? m.group(1) // strip quotes
                : lexeme;

            tokens.add(new Token(rule.type(), value));
          }

          break;
        }
      }

      if (!matched) {
        throw new DbmsParseException("Unexpected token near: " + input);
      }
    }

    return tokens;
  }

  private static Token parseToken(final String tokenStr) {
    return new Token(TokenType.EQ, tokenStr);
  }

  public static void main(final String[] args) {
    Tokenizer tokenizer = new Tokenizer();
    try {
      final List<Token> tokens = tokenizer.tokenize("SELECT * FROM student");
      System.out.println(tokens);
    } catch (DbmsParseException e) {
      System.out.println(e.getMessage());
    }

  }

}
