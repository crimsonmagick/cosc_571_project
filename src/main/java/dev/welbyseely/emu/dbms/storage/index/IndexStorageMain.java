package dev.welbyseely.emu.dbms.storage.index;

import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.storage.table.LinePointer;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import dev.welbyseely.emu.dbms.tree.ResultEntry;
import java.nio.file.Files;
import java.nio.file.Path;

public class IndexStorageMain {

  public static void main(String[] args) throws Exception {
    final Path indexPath = Path.of("student.idx");

    Files.deleteIfExists(indexPath);

    final IndexStorage<Integer> indexStorage = new IndexStorage<>(
        indexPath,
        DataType.INTEGER,
        Integer::parseInt,
        Object::toString
    );

    final BinarySearchTree<Integer, RecordPointer> tree = new BinarySearchTree<>();
    tree.insert(3, new LinePointer(7));
    tree.insert(1, new LinePointer(5));
    tree.insert(2, new LinePointer(6));
    tree.insert(5, new LinePointer(9));
    tree.insert(4, new LinePointer(8));

    System.out.println("Original tree inorder:");
    for (ResultEntry<Integer, RecordPointer> entry : tree.inorder()) {
      System.out.println(entry);
    }

    indexStorage.write(tree);
    System.out.println("\nIndex file written to: " + indexPath.toAbsolutePath());

    final BinarySearchTree<Integer, RecordPointer> reloaded = indexStorage.read();

    System.out.println("\nReloaded tree inorder:");
    for (ResultEntry<Integer, RecordPointer> entry : reloaded.inorder()) {
      System.out.println(entry);
    }

    System.out.println("\nSearch tests:");
    System.out.println("search(1) -> " + reloaded.search(1));
    System.out.println("search(3) -> " + reloaded.search(3));
    System.out.println("search(999) -> " + reloaded.search(999));

    final ResultEntry<Integer, RecordPointer> found = reloaded.search(4);
    if (found != null && found.value() instanceof LinePointer lp) {
      System.out.println("\nPointer for key 4 is line: " + lp.line());
    }

    System.out.println("\nRaw file contents:");
    Files.readAllLines(indexPath).forEach(System.out::println);
  }
}