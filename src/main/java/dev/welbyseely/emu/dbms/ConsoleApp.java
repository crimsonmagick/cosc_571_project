package dev.welbyseely.emu.dbms;

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

      if (!buffer.toString().contains(";")) {
        continue;
      }

      String sql = buffer.toString().trim();
      buffer.setLength(0);

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

}