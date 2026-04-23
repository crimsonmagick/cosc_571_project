package dev.welbyseely.emu.dbms.parsing;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;
import dev.welbyseely.emu.dbms.commands.engine.CreateCommand;
import dev.welbyseely.emu.dbms.commands.engine.ExitCommand;
import dev.welbyseely.emu.dbms.commands.engine.UseCommand;
import dev.welbyseely.emu.dbms.exception.DbmsParseException;
import dev.welbyseely.emu.dbms.query.Comparison;
import dev.welbyseely.emu.dbms.query.Expression;
import dev.welbyseely.emu.dbms.query.Logical;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.TokenType;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import java.util.ArrayList;
import java.util.List;

public class Parser {

  private final List<Token> tokens;
  private int pos = 0;

  public Parser(final List<Token> tokens) {
    if (tokens.isEmpty()) {
      throw new IllegalArgumentException("Parser needs tokens");
    }
    this.tokens = tokens;
  }

  public PreparedCommand parse() {
    final Token firstToken = tokens.getFirst();
    return switch (firstToken.type()) {
      case SELECT -> parseSelect();
      case EXIT -> new ExitCommand();
      case CREATE -> parseCreate();
      case USE -> parseUse();
      default ->
          throw new UnsupportedOperationException("Command not supported: " + firstToken.text());
    };
  }

  public CreateCommand parseCreate() {
    expect(TokenType.CREATE);
    final String databaseName = parseDatabaseName();
    return new CreateCommand(databaseName);
  }

  public UseCommand parseUse() {
    expect(TokenType.USE);
    final String databaseName = parseDatabaseName();
    return new UseCommand(databaseName);
  }

  private String parseDatabaseName() {
    Token dbIdentifier = advance();
    if (dbIdentifier.type() == TokenType.IDENTIFIER) {
      return dbIdentifier.text();
    }
    throw new DbmsParseException("Expected identifier");
  }

  private SelectQuery parseSelect() {
    expect(TokenType.SELECT);

    List<String> columns = parseColumns();

    expect(TokenType.FROM);
    String table = expect(TokenType.IDENTIFIER).text();

    Expression where = null;
    if (match(TokenType.WHERE)) {
      where = parseExpression();
    }

    return new SelectQuery(columns, table, where);
  }

  private boolean hasNext() {
    return pos < tokens.size();
  }

  private Token peek() {
    if (!hasNext()) {
      throw new DbmsParseException("Unexpected end of input at position " + pos);
    }
    return tokens.get(pos);
  }

  private Token advance() {
    Token t = peek();
    pos++;
    return t;
  }

  private boolean match(TokenType type) {
    if (hasNext() && peek().type() == type) {
      advance();
      return true;
    }
    return false;
  }

  private Token expect(TokenType type) {
    if (peek().type() != type) {
      throw new RuntimeException("Expected " + type + " but got " + peek());
    }
    return advance();
  }

  private List<String> parseColumns() {
    List<String> cols = new ArrayList<>();

    if (match(TokenType.ASTERISK)) {
      return List.of("*");
    }

    do {
      cols.add(expect(TokenType.IDENTIFIER).text());
    } while (match(TokenType.COMMA));

    return cols;
  }

  private Expression parseExpression() {
    Expression expr = parseAnd();

    while (match(TokenType.OR)) {
      Expression right = parseAnd();
      expr = new Logical(expr, "OR", right);
    }

    return expr;
  }

  private Expression parseAnd() {
    Expression expr = parseComparison();

    while (match(TokenType.AND)) {
      Expression right = parseComparison();
      expr = new Logical(expr, "AND", right);
    }

    return expr;
  }

  private Expression parseComparison() {
    String left = expect(TokenType.IDENTIFIER).text();

    Token op = advance(); // =, !=, <, etc.

    String right;
    if (peek().type() == TokenType.STRING || peek().type() == TokenType.NUMBER) {
      right = advance().text();
    } else {
      right = expect(TokenType.IDENTIFIER).text();
    }

    return new Comparison(left, op.text(), right);
  }

  public static void main(final String[] args) {
    Tokenizer tokenizer = new Tokenizer();
    List<String> queries = List.of(
        "SELECT * FROM student",
        "SELECT * FROM student where id = 1",
        "SELECT * FROM student where id = 1 AND gpa > 3.0",
        "SELECT * FROM student where id = 1 AND gpa > 3.0 OR id = 2"
    );
    for (final String query : queries) {
      System.out.println("Query String: " + query);
      try {
        final List<Token> tokens = tokenizer.tokenize(query);
        final Parser parser = new Parser(tokens);
        final SelectQuery selectQuery = parser.parseSelect();
        System.out.println("Tokens: " + tokens);
        System.out.println("SelectQuery: " + selectQuery);
      } catch (DbmsParseException e) {
        System.out.println(e.getMessage());
        System.out.println("Moving on to next query...");
      } finally {
        System.out.println("\n-----------------\n");
      }
    }
  }
}