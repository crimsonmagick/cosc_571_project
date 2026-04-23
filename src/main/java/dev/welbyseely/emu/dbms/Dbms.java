package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.commands.results.ErrorResult;
import dev.welbyseely.emu.dbms.commands.results.ExitResult;
import dev.welbyseely.emu.dbms.commands.results.InputResult;
import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.commands.results.VoidResult;
import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class Dbms {

  private static final Dbms INSTANCE = new Dbms();

  private final DatabaseEngine engine;

  private Dbms() {
    this.engine = new DatabaseEngine();
  }

  public static Dbms get() {
    return INSTANCE;
  }

  public List<Result> execute(final String sql) {
    return engine.execute(sql);
  }

  public Result executeStatement(final String sql) {
    return engine.executeStatement(sql);
  }

  public DatabaseEngine getEngine() {
    return this.engine;
  }

  public boolean executeAndPrint(String sql) {
    List<Result> results = execute(sql);

    for (Result result : results) {
      if (result instanceof InputResult inputResult) {
        if (executeInputAndPrint(inputResult)) {
          return true;
        }
        continue;
      }

      printResult(result);

      if (result instanceof ExitResult) {
        return true;
      }
    }

    return false;
  }

  private boolean executeInputAndPrint(InputResult inputResult) {
    try {
      String fileContents = Files.readString(Path.of(inputResult.input()));

      List<Result> nestedResults = execute(fileContents);

      for (Result nested : nestedResults) {
        if (nested instanceof InputResult nestedInput) {
          if (executeInputAndPrint(nestedInput)) {
            return true;
          }
          continue;
        }

        printResult(nested);

        if (nested instanceof ExitResult) {
          return true;
        }
      }

      return false;
    } catch (final IOException e) {
      printResult(new ErrorResult("Failed to read input file: " + inputResult.input()));
      return false;
    }
  }

  private void printResult(Result result) {
    if (result instanceof ErrorResult(String message)) {
      System.out.println("Error: " + message);
    } else if (result instanceof MessageResult msg) {
      System.out.println(msg.message());
    } else if (result instanceof TupleResult tuples) {
      printTuples(tuples.tuples());
    } else if (result instanceof VoidResult) {
      System.out.println("OK");
    } else if (result instanceof ExitResult) {
      System.out.println("Exiting...");
    }
  }

  private void printTuples(List<dev.welbyseely.emu.dbms.table.Row> rows) {
    if (rows.isEmpty()) {
      System.out.println("Nothing found");
      return;
    }

    int i = 1;
    for (var row : rows) {
      System.out.println(i++ + ". " + row.values());
    }
  }
}