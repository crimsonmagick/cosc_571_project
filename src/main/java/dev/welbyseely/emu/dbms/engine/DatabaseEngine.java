package dev.welbyseely.emu.dbms.engine;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;
import dev.welbyseely.emu.dbms.commands.engine.CreateDatabaseCommand;
import dev.welbyseely.emu.dbms.commands.engine.ExitCommand;
import dev.welbyseely.emu.dbms.commands.engine.UseCommand;
import dev.welbyseely.emu.dbms.commands.query.PreparedQuery;
import dev.welbyseely.emu.dbms.commands.results.ErrorResult;
import dev.welbyseely.emu.dbms.commands.results.ExitResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.VoidResult;
import dev.welbyseely.emu.dbms.exception.NoActiveDatabaseException;
import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Database;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DatabaseEngine {

  private Database activeDatabase;
  private final Map<String, Database> databases;
  private final Tokenizer tokenizer;

  public DatabaseEngine() {
    this.tokenizer = new Tokenizer();
    this.databases = new HashMap<>();
    this.activeDatabase = null;
  }

  public List<Result> execute(final String statements) {
    return Arrays.stream(statements.split(";"))
        .map(this::executeStatement)
        .toList();
  }

  private String sanitizeStatement(final String sql) {
    if (sql.endsWith(";")) {
      return sql.substring(0, sql.length() - 1);
    }
    return sql;
  }

  public Result executeStatement(final String sql) {
    try {
      final String sanitizedSql = sanitizeStatement(sql);
      List<Token> tokens = tokenizer.tokenize(sanitizedSql);

      Parser parser = new Parser(tokens);
      PreparedCommand preparedCommand = parser.parse();
      if (preparedCommand instanceof PreparedQuery pq) {
        if (activeDatabase == null) {
          throw new NoActiveDatabaseException("No active database selected!");
        }
        return activeDatabase.executeQuery(pq);
      }
      return executeCommand(preparedCommand);
    } catch (final RuntimeException e) {
      return new ErrorResult("Failed to execute command. Reason: " + e.getMessage());
    }
  }

  private Result executeCommand(final PreparedCommand preparedCommand) {
    return switch (preparedCommand) {
      case ExitCommand exitCommand -> new ExitResult();
      case CreateDatabaseCommand(String databaseName) -> createDatabase(databaseName);
      case UseCommand(String databaseName) -> useDatabase(databaseName);
      case null, default ->
          throw new UnsupportedOperationException("Command not supported: " + preparedCommand);
    };
  }

  private VoidResult createDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    final Database database = new DatabaseImpl(normDbName);
    databases.put(normDbName, database);
    return new VoidResult();
  }

  private VoidResult useDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    activeDatabase = Optional.ofNullable(databases.get(normDbName)).orElseThrow(() ->
        new NoActiveDatabaseException("No database found with name " + databaseName));
    return new VoidResult();
  }
}
