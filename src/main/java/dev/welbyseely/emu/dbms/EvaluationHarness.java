package dev.welbyseely.emu.dbms;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.results.ErrorResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class EvaluationHarness {

  public static void main(String[] args) throws Exception {

    final Path baseDir = resolveBaseDir();

    // clean run
    if (Files.exists(baseDir)) {
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
    }

    Dbms dbms = Dbms.get();

    System.out.println("=== Creating database ===");
    dbms.executeStatement("CREATE DATABASE tempdb");
    dbms.executeStatement("USE tempdb");

    System.out.println("\n=== Creating table ===");
    dbms.executeStatement(
        "CREATE TABLE student (id INTEGER PRIMARY KEY, name TEXT, gpa FLOAT)"
    );

    System.out.println("\n=== Inserting rows ===");

    Result result;
    result = dbms.executeStatement(
        "INSERT student VALUES (2, \"Alice\", 3.5)"
    );

    System.out.println("Result Alice :" + result);

    dbms.executeStatement(
        "INSERT student VALUES (1, \"Bob\", 2.75)"
    );

    System.out.println("Result Bob:" + result);

    dbms.executeStatement(
        "INSERT student VALUES (3, \"Charlie\", 3.9)"
    );

    System.out.println("Result Charlie:" + result);

    System.out.println("\n=== Query ===");

    result = dbms.executeStatement(
        "SeLECt name, gpa FROM STuDEnT WHERE name = \"Alice\""
    );

    if (result instanceof ErrorResult) {
      System.out.println("Tuple search errored: " + result);
    } else {
      ((TupleResult) result).tuples()
          .forEach(System.out::println);
    }
  }
}