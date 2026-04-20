package dev.welbyseely.emu.dbms.table;

import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.storage.RecordPointer;
import dev.welbyseely.emu.dbms.storage.TableStorage;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import dev.welbyseely.emu.dbms.tree.ResultEntry;

public class Table<K extends Comparable<? super K>> {

  final Schema schema;
  final TableStorage tableStorage;
  final BinarySearchTree<K, RecordPointer> index;

  public Table(final Schema schema, final TableStorage tableStorage,
      final BinarySearchTree<K, RecordPointer> index) {
    this.schema = schema;
    this.tableStorage = tableStorage;
    this.index = index;
  }

  public Iterable<RecordPointer> scanPointers() {
    if (index != null) {
      return index.inorder()
          .stream()
          .map(ResultEntry::value)
          .toList();
    } else {
      return tableStorage.scan();
    }
  }
}
