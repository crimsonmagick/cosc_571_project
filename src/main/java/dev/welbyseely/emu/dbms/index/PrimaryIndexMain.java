package dev.welbyseely.emu.dbms.index;

import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.storage.table.LinePointer;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

public class PrimaryIndexMain {

  public static void main(String[] args) throws Exception {
    final String indexName = "student_pk_test";

    final Path baseDir = resolveBaseDir();
    final Path indexPath = baseDir.resolve(indexName + ".idx");

    Files.createDirectories(baseDir);
    Files.deleteIfExists(indexPath);

    System.out.println("Base dir: " + baseDir.toAbsolutePath());
    System.out.println("Index path: " + indexPath.toAbsolutePath());

    Function<String, Integer> parser = Integer::parseInt;
    Function<Integer, String> serializer = Object::toString;

    PrimaryIndex<Integer> index = new PrimaryIndex<>(
        new BinarySearchTree<>(),
        indexName,
        DataType.INTEGER,
        parser,
        serializer
    );

    index.insert(3, new LinePointer(7));
    index.insert(1, new LinePointer(5));
    index.insert(2, new LinePointer(6));
    index.insert(5, new LinePointer(9));
    index.insert(4, new LinePointer(8));

    System.out.println("\nSearch tests from original index:");
    System.out.println("search(1) -> " + index.search(1));
    System.out.println("search(3) -> " + index.search(3));
    System.out.println("search(999) -> " + index.search(999));

    System.out.println("\nScan original index:");
    for (RecordPointer pointer : index.scan()) {
      System.out.println(pointer);
    }

    System.out.println("\nRaw index file contents:");
    Files.readAllLines(indexPath).forEach(System.out::println);

    PrimaryIndex<Integer> reloaded = new PrimaryIndex<>(
        new BinarySearchTree<>(),
        indexName,
        DataType.INTEGER,
        parser,
        serializer
    );

    System.out.println("\nSearch tests from reloaded index:");
    System.out.println("search(1) -> " + reloaded.search(1));
    System.out.println("search(3) -> " + reloaded.search(3));
    System.out.println("search(999) -> " + reloaded.search(999));

    System.out.println("\nScan reloaded index:");
    for (RecordPointer pointer : reloaded.scan()) {
      System.out.println(pointer);
    }

    reloaded.remove();
    System.out.println("\nRemoved index.");
    System.out.println("Index file exists after remove? " + Files.exists(indexPath));
  }
}