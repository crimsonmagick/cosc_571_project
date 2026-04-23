package dev.welbyseely.emu.dbms.parsing;

import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.COMMA;
import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.EQ;
import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.IDENTIFIER;
import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.SET;
import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.UPDATE;
import static dev.welbyseely.emu.dbms.parsing.tokens.TokenType.WHERE;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;
import dev.welbyseely.emu.dbms.commands.engine.CreateDatabaseCommand;
import dev.welbyseely.emu.dbms.commands.engine.ExitCommand;
import dev.welbyseely.emu.dbms.commands.engine.UseCommand;
import dev.welbyseely.emu.dbms.commands.query.*;
import dev.welbyseely.emu.dbms.exception.DbmsParseException;
import dev.welbyseely.emu.dbms.query.Comparison;
import dev.welbyseely.emu.dbms.query.Expression;
import dev.welbyseely.emu.dbms.query.Logical;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.TokenType;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Parser {
    private static record ParsedSelectProjection(List<String> columns, Aggregate aggregate) {
    }

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
            case INSERT -> parseInsert();
            case UPDATE -> parseUpdate();
            case EXIT -> new ExitCommand();
            case CREATE -> parseCreate();
            case USE -> parseUse();
            case DESCRIBE -> parseDescribe();
            case DELETE -> parseDelete();
            case RENAME -> parseRename();
            case LET -> parseLet();
            default -> throw new UnsupportedOperationException("Command not supported: " + firstToken.text());
        };
    }

    private LetQuery parseLet() {
        expect(TokenType.LET);

        String tableName = expect(TokenType.IDENTIFIER).text();

        expect(TokenType.KEY);
        String keyAttr = expect(TokenType.IDENTIFIER).text();

        SelectQuery select = parseSelect();

        return new LetQuery(tableName, keyAttr, select);
    }

    private RenameQuery parseRename() {
        expect(TokenType.RENAME);

        String tableName = expect(TokenType.IDENTIFIER).text();

        expect(TokenType.LPAREN);

        List<String> newNames = new ArrayList<>();

        do {
            newNames.add(expect(TokenType.IDENTIFIER).text());
        } while (match(TokenType.COMMA));

        expect(TokenType.RPAREN);

        return new RenameQuery(tableName, newNames);
    }

    private DeleteQuery parseDelete() {
        expect(TokenType.DELETE);

        final String tableName = expect(TokenType.IDENTIFIER).text();

        Expression where = null;
        if (match(TokenType.WHERE)) {
            where = parseExpression();
        }

        return new DeleteQuery(tableName, where);

    }

    private UpdateQuery parseUpdate() {
        expect(TokenType.UPDATE);

        final String tableName = expect(TokenType.IDENTIFIER).text();

        expect(TokenType.SET);

        final Map<String, String> updates = new LinkedHashMap<>();

        do {
            final String attrName = expect(TokenType.IDENTIFIER).text();
            expect(TokenType.EQ);
            final String value = parseConstant();
            updates.put(attrName, value);
        } while (match(TokenType.COMMA));

        Expression where = null;
        if (match(TokenType.WHERE)) {
            where = parseExpression();
        }

        return new UpdateQuery(tableName, updates, where);
    }

    private String parseConstant() {
        final Token token = advance();

        return switch (token.type()) {
            case STRING, NUMBER -> token.text();
            default -> throw new DbmsParseException(
                    "Expected STRING or NUMBER but got " + token);
        };
    }

    private DescribeQuery parseDescribe() {
        expect(TokenType.DESCRIBE);

        if (match(TokenType.ALL)) {
            return new DescribeQuery(true, null);
        }

        String tableName = expect(IDENTIFIER).text();
        return new DescribeQuery(false, tableName);
    }

    private InsertQuery parseInsert() {
        expect(TokenType.INSERT);

        final String tableName = expect(IDENTIFIER).text();

        expect(TokenType.VALUES);
        expect(TokenType.LPAREN);

        final List<String> values = new ArrayList<>();

        do {
            values.add(parseInsertValue());
        } while (match(COMMA));

        expect(TokenType.RPAREN);

        return new InsertQuery(tableName, values);
    }

    private String parseInsertValue() {
        final Token token = advance();

        return switch (token.type()) {
            case STRING, NUMBER -> token.text();
            default -> throw new DbmsParseException("Expected STRING or NUMBER but got " + token);
        };
    }

    public PreparedCommand parseCreate() {
        expect(TokenType.CREATE);

        if (match(TokenType.DATABASE)) {
            String databaseName = expect(IDENTIFIER).text();
            return new CreateDatabaseCommand(databaseName);
        }

        if (match(TokenType.TABLE)) {
            return parseCreateTable();
        }

        throw new DbmsParseException("Expected DATABASE or TABLE after CREATE");
    }

    private CreateTableQuery parseCreateTable() {
        String tableName = expect(IDENTIFIER).text();

        expect(TokenType.LPAREN);

        List<Attribute> attributes = new ArrayList<>();

        do {
            attributes.add(parseAttribute());
        } while (match(COMMA));

        expect(TokenType.RPAREN);

        return new CreateTableQuery(tableName, attributes);
    }

    private Attribute parseAttribute() {
        String name = expect(IDENTIFIER).text();

        Token typeToken = advance();

        if (typeToken.type() != TokenType.INTEGER &&
                typeToken.type() != TokenType.FLOAT &&
                typeToken.type() != TokenType.TEXT) {
            throw new DbmsParseException("Expected data type but got " + typeToken);
        }

        String type = typeToken.text().toUpperCase();

        boolean primaryKey = false;

        if (match(TokenType.PRIMARY)) {
            expect(TokenType.KEY);
            primaryKey = true;
        }

        return new Attribute(name, type, primaryKey);
    }

    public UseCommand parseUse() {
        expect(TokenType.USE);
        final String databaseName = parseDatabaseName();
        return new UseCommand(databaseName);
    }

    private String parseDatabaseName() {
        Token dbIdentifier = advance();
        if (dbIdentifier.type() == IDENTIFIER) {
            return dbIdentifier.text();
        }
        throw new DbmsParseException("Expected identifier");
    }

    private SelectQuery parseSelect() {
        expect(TokenType.SELECT);

        ParsedSelectProjection projection = parseSelectProjection();

        expect(TokenType.FROM);

        List<String> tables = new ArrayList<>();
        do {
            tables.add(expect(TokenType.IDENTIFIER).text());
        } while (match(TokenType.COMMA));

        Expression where = null;
        if (match(TokenType.WHERE)) {
            where = parseExpression();
        }

        return new SelectQuery(
                projection.columns(),
                tables,
                where,
                projection.aggregate()
        );
    }

    private boolean check(TokenType type) {
        return hasNext() && peek().type() == type;
    }

    private ParsedSelectProjection parseSelectProjection() {
        if (check(TokenType.IDENTIFIER) && peek().text().equalsIgnoreCase("count")) {
            advance(); // consume "count"
            expect(TokenType.LPAREN);
            expect(TokenType.ASTERISK);
            expect(TokenType.RPAREN);
            return new ParsedSelectProjection(
                    List.of(),
                    new Aggregate(AggregateType.COUNT, "*")
            );
        }
        if (check(TokenType.IDENTIFIER) && peek().text().equalsIgnoreCase("min")) {
            advance();
            expect(TokenType.LPAREN);
            String col = expect(TokenType.IDENTIFIER).text();
            expect(TokenType.RPAREN);
            return new ParsedSelectProjection(
                    List.of(),
                    new Aggregate(AggregateType.MIN, col)
            );
        }
        if (check(TokenType.IDENTIFIER) && peek().text().equalsIgnoreCase("max")) {
            advance();
            expect(TokenType.LPAREN);
            String col = expect(TokenType.IDENTIFIER).text();
            expect(TokenType.RPAREN);
            return new ParsedSelectProjection(
                    List.of(),
                    new Aggregate(AggregateType.MAX, col)
            );
        }
        if (check(TokenType.IDENTIFIER) && peek().text().equalsIgnoreCase("average")) {
            advance();
            expect(TokenType.LPAREN);
            String col = expect(TokenType.IDENTIFIER).text();
            expect(TokenType.RPAREN);
            return new ParsedSelectProjection(
                    List.of(),
                    new Aggregate(AggregateType.AVERAGE, col)
            );
        }
        return new ParsedSelectProjection(parseColumns(), null);
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
            cols.add(expect(IDENTIFIER).text());
        } while (match(COMMA));

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
        String left = expect(IDENTIFIER).text();

        Token op = advance(); // =, !=, <, etc.

        String right;
        if (peek().type() == TokenType.STRING || peek().type() == TokenType.NUMBER) {
            right = advance().text();
        } else {
            right = expect(IDENTIFIER).text();
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