package dev.welbyseely.emu.dbms;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Database;
import dev.welbyseely.emu.dbms.table.DatabaseImpl;
import dev.welbyseely.emu.dbms.table.Row;
import dev.welbyseely.emu.dbms.table.Table;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EvaluationHarness {

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
    try (var stream = Files.walk(baseDir)) {
      stream
          .sorted(Comparator.reverseOrder())
          .forEach(path -> {
            try {
              Files.delete(path);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    }

    Database database = new DatabaseImpl("tempdb");
    Table table = database.createTable(schema);

    System.out.println("=== Inserting rows ===");

    table.insert(new Row(Map.of(
        "id", 2,
        "name", "Alice",
        "gpa", 3.5
    )));

    table.insert(new Row(Map.of(
        "id", 1,
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

    String sql = "SELECT name, gpa FROM STuDEnT WHERE name = Alice;";

    Dbms.get().execute("CREATE tempdb");
    Dbms.get().execute("USE tempdb");
    Result result = Dbms.get()
        .executeStatement(sql);
    ((TupleResult) result).tuples()
        .forEach(System.out::println);

  }
}