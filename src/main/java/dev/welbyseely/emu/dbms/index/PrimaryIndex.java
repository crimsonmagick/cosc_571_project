package dev.welbyseely.emu.dbms.index;

import static dev.welbyseely.emu.dbms.constants.DirUtil.resolveBaseDir;

import dev.welbyseely.emu.dbms.exception.IndexStorageException;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.storage.index.IndexStorage;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import dev.welbyseely.emu.dbms.tree.ResultEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PrimaryIndex<T extends Comparable<? super T>> {

  private final BinarySearchTree<T, RecordPointer> tree;
  private final IndexStorage<T> indexStorage;
  private boolean removed;

  public PrimaryIndex(final BinarySearchTree<T, RecordPointer> tree,
      final String indexName,
      final Path indexDir,
      final DataType keyType,
      final java.util.function.Function<String, T> keyParser,
      final java.util.function.Function<T, String> keySerializer) {

    this.removed = false;

    final Path baseDir = resolveBaseDir();
    final Path indexPath = indexDir.resolve(indexName + ".idx");

    try {
      Files.createDirectories(baseDir);
    } catch (final IOException e) {
      throw new IndexStorageException("Unable to create index directory: " + baseDir, e);
    }

    this.indexStorage = new IndexStorage<>(
        indexPath,
        keyType,
        keyParser,
        keySerializer
    );

    if (Files.exists(indexPath)) {
      this.tree = indexStorage.read();
    } else {
      this.tree = tree;
      indexStorage.write(tree);
    }
  }

  @SuppressWarnings("unchecked")
  public void insertUntyped(final Comparable<?> key, final RecordPointer pointer) {
    insert((T) key, pointer);
  }

  public void insert(T key, RecordPointer recordPointer) {
    assertNotRemoved();
    tree.insert(key, recordPointer);
    indexStorage.write(tree);
  }

  @SuppressWarnings("unchecked")
  public Optional<RecordPointer> searchUntyped(final Comparable<?> key) {
    return search((T) key);
  }

  public Optional<RecordPointer> search(final T key) {
    assertNotRemoved();
    return Optional.ofNullable(tree.search(key))
        .map(ResultEntry::value);
  }

  public void removeIndex() {
    if (removed) {
      return;
    }
    this.removed = true;
    indexStorage.remove();
  }

  public Iterable<RecordPointer> scan() {
    assertNotRemoved();
    return tree.inorder()
        .stream()
        .map(ResultEntry::value)
        .toList();
  }

  private void assertNotRemoved() {
    if (removed) {
      throw new IndexStorageException("Index has already been removed");
    }
  }

  public void deleteUntyped(Comparable<?> key) {
    tree.delete((T) key);
    indexStorage.write(tree);
  }
}