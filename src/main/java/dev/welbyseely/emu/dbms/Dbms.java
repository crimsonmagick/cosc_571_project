package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.commands.results.ErrorResult;
import dev.welbyseely.emu.dbms.commands.results.ExitResult;
import dev.welbyseely.emu.dbms.commands.results.InputResult;
import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import dev.welbyseely.emu.dbms.commands.results.TupleResult;
import dev.welbyseely.emu.dbms.engine.DatabaseEngine;
import dev.welbyseely.emu.dbms.table.Row;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
    return executeAndPrint(sql, System.out);
  }

  public boolean executeAndPrint(String sql, PrintStream out) {
    List<Result> results = execute(sql);

    for (Result result : results) {
      if (result instanceof InputResult input) {
        if (executeInputAndPrint(input)) {
          return true;
        }
        continue;
      }

      printResult(result, out);

      if (result instanceof ExitResult) {
        return true;
      }
    }

    return false;
  }

  private boolean executeInputAndPrint(InputResult input) {
    PrintStream out = System.out;

    try {
      if (input.output() != null) {
        out = new PrintStream(new FileOutputStream(input.output()), true);
      }

      String fileContents = Files.readString(Path.of(input.input()));

      List<Result> results = execute(fileContents);

      for (Result result : results) {
        if (result instanceof InputResult nested) {
          if (executeInputAndPrint(nested)) {
            return true;
          }
          continue;
        }

        printResult(result, out);

        if (result instanceof ExitResult) {
          return true;
        }
      }

      return false;

    } catch (IOException e) {
      printResult(new ErrorResult("Failed to process input file: " + input.input()), System.out);
      return false;

    } finally {
      if (out != System.out) {
        out.close();
      }
    }
  }

  private void printResult(Result result, PrintStream out) {
    if (result instanceof ErrorResult err) {
      out.println("Error: " + err.message());
    } else if (result instanceof MessageResult msg) {
      out.println(msg.message());
    } else if (result instanceof TupleResult tuples) {
      printTuples(tuples.tuples(), out);
    } else if (result instanceof ExitResult) {
      out.println("Exiting...");
    }
    out.flush();
  }

  private void printTuples(List<Row> rows, PrintStream out) {
    if (rows.isEmpty()) {
      out.println("Nothing found");
      return;
    }

    int i = 1;
    for (Row row : rows) {
      out.println(i++ + ". " + row.values());
    }
  }
}