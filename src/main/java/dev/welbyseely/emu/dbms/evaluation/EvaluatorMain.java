package dev.welbyseely.emu.dbms.evaluation;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.parsing.Parser;
import dev.welbyseely.emu.dbms.parsing.tokens.Token;
import dev.welbyseely.emu.dbms.parsing.tokens.Tokenizer;
import dev.welbyseely.emu.dbms.query.QueryEngine;
import dev.welbyseely.emu.dbms.query.SelectQuery;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;
import dev.welbyseely.emu.dbms.table.TableManager;
import dev.welbyseely.emu.dbms.table.TableManagerImpl;
import dev.welbyseely.emu.dbms.table.TableProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class EvaluatorMain {

  public static void main(String[] args) throws Exception {
    final Schema schema = new Schema(
        "student",
        List.of(
            new Attribute("id", DataType.INTEGER.name(), true),
            new Attribute("name", DataType.TEXT.name(), false),
            new Attribute("gpa", DataType.FLOAT.name(), false)
        )
    );

    final Path baseDir = resolveBaseDir();
    final Path tablePath = baseDir.resolve(schema.schemaName() + ".tbl");
    final Path indexPath = baseDir.resolve(schema.schemaName() + "_pk.idx");

    // clean run (important for repeatability)
    Files.deleteIfExists(tablePath);
    Files.deleteIfExists(indexPath);

    final Table table = TableProvider.create(schema);

    System.out.println("=== Inserting rows ===");

    table.insert(new Row(Map.of(
        "id", 1,
        "name", "Alice",
        "gpa", 3.5
    )));

    table.insert(new Row(Map.of(
        "id", 2,
        "name", "Bob",
        "gpa", 2.75
    )));

    table.insert(new Row(Map.of(
        "id", 3,
        "name", "Charlie",
        "gpa", 3.9
    )));

    System.out.println("\n=== Scan after inserts ===");
    for (Row row : table.scan()) {
      System.out.println(row);
    }

    System.out.println("\n=== PK lookup ===");
    System.out.println("id=1 -> " + table.getByPrimaryKey(1));
    System.out.println("id=3 -> " + table.getByPrimaryKey(3));
    System.out.println("id=999 -> " + table.getByPrimaryKey(999));

    System.out.println("\n=== Testing Query engine ===");

    Tokenizer tokenizer = new Tokenizer();

    String sql = "SELECT name, gpa FROM STuDEnT WHERE gpa> 3";

    List<Token> tokens = tokenizer.tokenize(sql);

    Parser parser = new Parser(tokens);
    SelectQuery query = parser.parseSelect();

    TableManager tableManager = new TableManagerImpl();
    QueryEngine engine = new QueryEngine(tableManager);
    List<Row> results = engine.executeSelect(query);

    results.forEach(System.out::println);

  }
}