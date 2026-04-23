package dev.welbyseely.emu.dbms;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.commands.results.MessageResult;
import dev.welbyseely.emu.dbms.commands.results.Result;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class EvalHarness {

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

    String sql = """
        CREATE DATABASE tempdb;
        USE tempdb;
        CREATE TABLE employee (name TEXT, department TEXT, salary FLOAT);
        INSERT employee VALUES ("Alice", "Engineering", 95000.0);
        INSERT employee VALUES ("Bob", "Sales", 72000.0);
        INSERT employee VALUES ("Charlie", "Engineering", 105000.0);
        INSERT employee VALUES ("Dana", "HR", 68000.0);
        INSERT employee VALUES ("Evan", "Engineering", 95000.0);
        SELECT name, department FROM employee WHERE salary = 95000.0;
        SELECT * FROM EMPLOYEE;
        DESCRIBE ALL;
        """;

    execSql(sql);

    System.out.println("--------------- Next --------------");

    sql = """
        CREATE DATABASE corp;
        USE corp;
        
        CREATE TABLE employee (id INTEGER PRIMARY KEY, name TEXT, department TEXT, salary FLOAT);
        CREATE TABLE department (dept_id INTEGER PRIMARY KEY, dept_name TEXT, location TEXT);
        CREATE TABLE project (project_id INTEGER PRIMARY KEY, project_name TEXT, owner TEXT, budget FLOAT);
        
        INSERT employee VALUES (1, "Alice", "Engineering", 95000.0);
        INSERT employee VALUES (2, "Bob", "Sales", 72000.0);
        INSERT employee VALUES (3, "Charlie", "Engineering", 105000.0);
        
        INSERT department VALUES (10, "Engineering", "NY");
        INSERT department VALUES (20, "Sales", "SF");
        INSERT department VALUES (30, "HR", "LA");
        INSERT department VALUES (30, "Dining", "MI");
        
        INSERT project VALUES (100, "Apollo", "Alice", 1000000.0);
        INSERT project VALUES (200, "Zeus", "Charlie", 750000.0);
        INSERT project VALUES (300, "Hermes", "Bob", 250000.0);
        
        SELECT name, department FROM employee WHERE department = "Engineering";
        SELECT dept_name, location FROM department WHERE location = "SF";
        SELECT project_name, budget FROM project WHERE budget > 500000.0;
        
        UPDATE employee SET department = "Platform" WHERE name = "Alice";
        UPDATE employee SET salary = 80000.0 WHERE id = 2;
        UPDATE project SET budget = 900000.0, owner = "Bob" WHERE project_name = "Zeus";
        
        SELECT id, name, department, salary FROM employee;
        SELECT project_id, project_name, owner, budget FROM project;
        
        UPDATE department SET location = "REMOTE";
        SELECT dept_id, location FROM department;
        
        UPDATE employee SET id = 4 WHERE id = 3;
        SELECT id, name FROM employee;
        
        DESCRIBE employee;
        DESCRIBE department;
        DESCRIBE project;
        
        UPDATE employee SET id = 2 WHERE id = 1;
        UPDATE employee SET salary = 1 WHERE id = 999;
        SELECT * FROM EMPLOYEE;
        
        UPDATE employee SET id = 10;
        SELECT * FROM EMPLOYEE;
        """;

    execSql(sql);
//
//    execSql("UPDATE employee SET department = \"Platform\" WHERE name = \"Alice\";\n");

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