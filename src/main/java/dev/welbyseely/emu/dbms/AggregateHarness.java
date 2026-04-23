package dev.welbyseely.emu.dbms;

import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

public class AggregateHarness {

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

    String sql = """
            CREATE DATABASE aggtest;
            USE aggtest;
            
            CREATE TABLE employee (
              id INTEGER PRIMARY KEY,
              name TEXT,
              dept TEXT,
              salary FLOAT
            );
            
            INSERT employee VALUES (1, "Alice", "Engineering", 100000.0);
            INSERT employee VALUES (2, "Bob", "Sales", 70000.0);
            INSERT employee VALUES (3, "Charlie", "Engineering", 120000.0);
            INSERT employee VALUES (4, "Dana", "HR", 60000.0);
            INSERT employee VALUES (5, "Eve", "Engineering", 100000.0);
            
            SELECT * FROM employee;
            
            SELECT count(*) FROM employee;
            SELECT min(salary) FROM employee;
            SELECT max(salary) FROM employee;
            SELECT average(salary) FROM employee;
            
            SELECT count(*) FROM employee WHERE dept = "Engineering";
            SELECT min(salary) FROM employee WHERE dept = "Engineering";
            SELECT max(salary) FROM employee WHERE dept = "Engineering";
            SELECT average(salary) FROM employee WHERE dept = "Engineering";
            
            SELECT count(*) FROM employee WHERE salary > 9999999;
            SELECT min(salary) FROM employee WHERE salary > 9999999;
            SELECT max(salary) FROM employee WHERE salary > 9999999;
            SELECT average(salary) FROM employee WHERE salary > 9999999;
            
            SELECT min(name) FROM employee;
            SELECT max(name) FROM employee;
            
            SELECT min(bogus) FROM employee;
            SELECT average(name) FROM employee;
            SELECT count(id) FROM employee;
            SELECT count(*), name FROM employee;
            
            UPDATE employee SET salary = 50000 WHERE id = 1;
            SELECT min(salary) FROM employee;
            SELECT average(salary) FROM employee;
            
            UPDATE employee SET salary = 50000 WHERE id = 1;
            SELECT min(salary) FROM employee;
            SELECT average(salary) FROM employee;
            
            DELETE employee WHERE id = 4;
            SELECT count(*) FROM employee;
            SELECT min(salary) FROM employee;
            
            RENAME employee (x, y, z, w);
            
            SELECT count(*) FROM employee;
            SELECT min(w) FROM employee;
            SELECT max(w) FROM employee;
            SELECT average(w) FROM employee;
            
            LET eng KEY x SELECT x, w FROM employee WHERE z = "Engineering";;
            
            SELECT * FROM eng;
        
            SELECT count(*) FROM eng;
            SELECT min(w) FROM eng;
            SELECT max(w) FROM eng;
            SELECT average(w) FROM eng;
            SELECT average(w)
                FROM employee
                WHERE z = "Engineering";
        """;

    execSql(sql);
  }

  private static void execSql(String sql) {
    List<Result> resultList = Dbms.get().execute(sql);

    for (Result result : resultList) {
      if (result instanceof MessageResult) {
        System.out.println(((MessageResult) result).message());
      } else {
        System.out.println(result);
      }
    }

  }
}