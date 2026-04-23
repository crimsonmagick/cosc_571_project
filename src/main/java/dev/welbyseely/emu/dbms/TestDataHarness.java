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
import java.util.List;

public class TestDataHarness {

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
        create database university;
        use university;
        create table x(id integer , name text);
        create table student(a
        text );
        select x,y,z
        from student
        where x=y or y!=z;
        describe student;
        let x select id,name from student;
        rename x(a,b,c)
        ;
        insert student values("abc","x","aaa",1,2,"yy"
        )
        ;
        """;

    List<Result> resultList = dbms.execute(sql);
    for (Result result : resultList) {
      System.out.println(result);
    }

  }
}