package dev.welbyseely.emu.dbms.engine;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;
import dev.welbyseely.emu.dbms.commands.engine.CreateDatabaseCommand;
import dev.welbyseely.emu.dbms.commands.engine.ExitCommand;
import dev.welbyseely.emu.dbms.commands.engine.InputCommand;
import dev.welbyseely.emu.dbms.commands.engine.UseCommand;
import dev.welbyseely.emu.dbms.commands.query.PreparedQuery;
import dev.welbyseely.emu.dbms.commands.results.*;
import dev.welbyseely.emu.dbms.exception.DatabaseEngineException;
import dev.welbyseely.emu.dbms.exception.NoActiveDatabaseException;
import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Database;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    loadExistingDatabases();
  }

  private void loadExistingDatabases() {
    Path baseDir = resolveBaseDir();

    if (!Files.exists(baseDir)) {
      return;
    }

    try (var stream = Files.list(baseDir)) {
      stream
          .filter(Files::isDirectory)
          .forEach(path -> {
            String dbName = path.getFileName().toString().toLowerCase();

            // initialize database from directory
            Database db = new DatabaseImpl(dbName);

            databases.put(dbName, db);
          });

    } catch (final IOException e) {
      throw new DatabaseEngineException("Failed to load databases from disk", e);
    }
  }

  public List<Result> execute(final String statements) {
    return Arrays.stream(statements.split(";"))
        .filter(q -> !q.isBlank())
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
      return new ErrorResult("Reason: " + e.getMessage());
    }
  }

  private Result executeCommand(final PreparedCommand preparedCommand) {
    return switch (preparedCommand) {
      case ExitCommand exitCommand -> new ExitResult();
      case CreateDatabaseCommand(String databaseName) -> createDatabase(databaseName);
      case UseCommand(String databaseName) -> useDatabase(databaseName);
      case InputCommand(String input, String output) -> new InputResult(input, output);
      case null, default ->
          throw new UnsupportedOperationException("Command not supported: " + preparedCommand);
    };
  }

  private VoidResult createDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    if (databases.containsKey(normDbName)) {
      throw new DatabaseEngineException(
          "Database with name \"" + databaseName + "\" already exists.");
    }
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
