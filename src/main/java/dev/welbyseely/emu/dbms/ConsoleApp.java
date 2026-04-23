package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.commands.results.*;
import dev.welbyseely.emu.dbms.table.Row;

import java.util.List;
import java.util.Scanner;

public class ConsoleApp {

  public static void main(String[] args) {

    Dbms dbms = Dbms.get();
    Scanner scanner = new Scanner(System.in);

    System.out.println("Simple DBMS console. End statements with ';'. Type EXIT; to quit.");

    StringBuilder buffer = new StringBuilder();

    while (true) {

      // prompt changes depending on whether we're mid-statement
      System.out.print(buffer.isEmpty() ? "> " : "... ");

      String line = scanner.nextLine();

      buffer.append(line).append("\n");

      // only execute when we see a semicolon
      if (!buffer.toString().contains(";")) {
        continue;
      }

      String sql = buffer.toString().trim();
      buffer.setLength(0); // clear buffer

      try {
        if (dbms.executeAndPrint(sql)) {
          break;
        }

      } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
      }
    }

    scanner.close();
  }

  private static void handleResult(Result result) {

    if (result instanceof ErrorResult err) {
      System.out.println("Error: " + err.message());
    } else if (result instanceof MessageResult msg) {
      System.out.println(msg.message());
    } else if (result instanceof TupleResult tuples) {
      printTuples(tuples.tuples());
    } else if (result instanceof VoidResult) {
      // optional: System.out.println("OK");
    } else if (result instanceof ExitResult) {
      System.out.println("Exiting...");
    }
  }

  private static void printTuples(List<Row> rows) {
    if (rows.isEmpty()) {
      System.out.println("Nothing found");
      return;
    }

    int i = 1;
    for (Row row : rows) {
      System.out.println(i++ + ". " + row.values());
    }
  }
}