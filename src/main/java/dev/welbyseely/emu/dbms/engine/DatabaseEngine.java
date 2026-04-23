package dev.welbyseely.emu.dbms.engine;

import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.query.QueryEngine;
import dev.welbyseely.emu.dbms.query.SelectQuery;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Database;
import java.util.List;

public class DatabaseEngine {

  private final Tokenizer tokenizer;
  private final Database database;
  private final QueryEngine queryEngine;

  public DatabaseEngine(Database database) {
    this.tokenizer = new Tokenizer();
    this.database = database;
    this.queryEngine = new QueryEngine(database);
  }

  public List<Row> execute(String sql) {
    List<Token> tokens = tokenizer.tokenize(sql);

    Parser parser = new Parser(tokens);
    SelectQuery query = parser.parseSelect();

    return queryEngine.executeSelect(query);
  }
}
