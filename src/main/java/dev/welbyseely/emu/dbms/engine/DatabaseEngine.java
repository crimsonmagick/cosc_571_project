package dev.welbyseely.emu.dbms.engine;

import dev.welbyseely.emu.dbms.exception.NoActiveDatabaseException;
import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.query.SelectQuery;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Database;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    if (activeDatabase == null) {
      throw new NoActiveDatabaseException("No active database selected!");
    }
    List<Token> tokens = tokenizer.tokenize(sql);

    Parser parser = new Parser(tokens);
    SelectQuery query = parser.parseSelect();

    return activeDatabase.executeQuery(query);
  }

  public void useDatabase(final String databaseName) {
    final String normDbName = databaseName.toLowerCase();
    activeDatabase = databases.computeIfAbsent(normDbName, DatabaseImpl::new);
  }
}
