package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.schema.Attribute;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.schema.Schema;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

public class TableProviderMain {

  public static void main(String[] args) throws Exception {
    final Schema schema = new Schema(
        "student",
        List.of(
            new Attribute("id", DataType.INTEGER.name(), true),
            new Attribute("name", DataType.TEXT.name(), false),
            new Attribute("gpa", DataType.FLOAT.name(), false)
        )
    );

    final Table<Integer> table = TableProvider.create(
        schema,
        DataType.INTEGER,
        (String s) -> Integer.parseInt(s),
        (Integer i) -> i.toString()
    );

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

    final Path tablePath = resolveBaseDir().resolve(schema.schemaName() + ".tbl");

    System.out.println("\n=== Reloading table from disk ===");
    final Table<Integer> reloaded = TableProvider.load(
        tablePath,
        DataType.INTEGER,
        (String s) -> Integer.parseInt(s),
        (Integer i) -> i.toString()
    );

    System.out.println("\n=== Scan after reload ===");
    for (Row row : reloaded.scan()) {
      System.out.println(row);
    }

    System.out.println("\n=== PK lookup after reload ===");
    System.out.println("id=1 -> " + reloaded.getByPrimaryKey(1));
    System.out.println("id=3 -> " + reloaded.getByPrimaryKey(3));
    System.out.println("id=999 -> " + reloaded.getByPrimaryKey(999));

    System.out.println("\n=== Done ===");
  }
}