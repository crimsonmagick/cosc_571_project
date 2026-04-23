package dev.welbyseely.emu.dbms.engine;

import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.query.QueryEngine;
import dev.welbyseely.emu.dbms.query.SelectQuery;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.TableManager;
import java.util.List;

public class DatabaseEngine {

  private final Tokenizer tokenizer;
  private final TableManager tableManager;
  private final QueryEngine queryEngine;

  public DatabaseEngine(TableManager tableManager) {
    this.tokenizer = new Tokenizer();
    this.tableManager = tableManager;
    this.queryEngine = new QueryEngine(tableManager);
  }

  public List<Row> execute(String sql) {
    List<Token> tokens = tokenizer.tokenize(sql);

    Parser parser = new Parser(tokens);
    SelectQuery query = parser.parseSelect();

    return queryEngine.executeSelect(query);
  }
}
