package dev.welbyseely.emu.dbms.engine;

import dev.welbyseely.emu.dbms.commands.PreparedCommand;
import dev.welbyseely.emu.dbms.commands.engine.CreateCommand;
import dev.welbyseely.emu.dbms.commands.engine.ExitCommand;
import dev.welbyseely.emu.dbms.commands.engine.UseCommand;
import dev.welbyseely.emu.dbms.commands.query.PreparedQuery;
import dev.welbyseely.emu.dbms.exception.NoActiveDatabaseException;
import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.commands.query.SelectQuery;
import dev.welbyseely.emu.dbms.query.QueryEngine;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Database;
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

  public List<Row> execute(String sql) {
    List<Token> tokens = tokenizer.tokenize(sql);

    Parser parser = new Parser(tokens);
    PreparedCommand preparedCommand = parser.parse();
    if (preparedCommand instanceof PreparedQuery pq) {
      if (activeDatabase == null) {
        throw new NoActiveDatabaseException("No active database selected!");
      }
      return activeDatabase.executeQuery(pq);
    }
    return executeCommand(preparedCommand);
  }

  private List<Row> executeCommand(final PreparedCommand preparedCommand) {
    switch (preparedCommand) {
      case ExitCommand exitCommand -> System.out.println("TODO: Exit");
      case CreateCommand(String databaseName) -> createDatabase(databaseName);
      case UseCommand(String databaseName) -> useDatabase(databaseName);
      case null, default ->
          throw new UnsupportedOperationException("Command not supported: " + preparedCommand);
    }
    return null;
  }

  private void createDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    final Database database = new DatabaseImpl(normDbName);
    databases.put(normDbName, database);
  }

  private void useDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    activeDatabase = Optional.ofNullable(databases.get(normDbName)).orElseThrow(() ->
        new NoActiveDatabaseException("No database found with name " + databaseName));
  }
}
