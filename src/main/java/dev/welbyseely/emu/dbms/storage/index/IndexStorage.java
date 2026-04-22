package dev.welbyseely.emu.dbms.storage.index;

import dev.welbyseely.emu.dbms.exception.IndexStorageException;
import dev.welbyseely.emu.dbms.schema.DataType;
import dev.welbyseely.emu.dbms.storage.table.LinePointer;
import dev.welbyseely.emu.dbms.storage.table.RecordPointer;
import dev.welbyseely.emu.dbms.tree.BinarySearchTree;
import dev.welbyseely.emu.dbms.tree.ResultEntry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Function;

public class IndexStorage<T extends Comparable<? super T>> {

  private static final String INDEX_HEADER = "#index";
  private static final String TYPE_PREFIX = "type|";

  private final Path path;
  private final DataType keyType;
  private final Function<String, T> keyParser;
  private final Function<T, String> keySerializer;

  public IndexStorage(
      final Path path,
      final DataType keyType,
      final Function<String, T> keyParser,
      final Function<T, String> keySerializer
  ) {
    this.path = path;
    this.keyType = keyType;
    this.keyParser = keyParser;
    this.keySerializer = keySerializer;
  }

  public void write(final BinarySearchTree<T, RecordPointer> tree) {
    final Path tmpPath = path.resolveSibling(path.getFileName() + ".tmp");

    try (BufferedWriter w = Files.newBufferedWriter(tmpPath)) {
      w.write(INDEX_HEADER);
      w.newLine();

      w.write(TYPE_PREFIX + keyType.name());
      w.newLine();

      for (ResultEntry<T, RecordPointer> entry : tree.inorder()) {
        w.write(serializeEntry(entry));
        w.newLine();
      }
    } catch (IOException e) {
      throw new IndexStorageException("Unable to write index file: " + path, e);
    }

    moveAtomicallyIfPossible(tmpPath, path);
  }

  public BinarySearchTree<T, RecordPointer> read() {
    if (!Files.exists(path)) {
      throw new IndexStorageException("Index file does not exist: " + path);
    }

    final BinarySearchTree<T, RecordPointer> tree = new BinarySearchTree<>();

    try (BufferedReader r = Files.newBufferedReader(path)) {
      String line;

      line = r.readLine();
      if (line == null || !INDEX_HEADER.equals(line)) {
        throw new IndexStorageException("Missing index header in file: " + path);
      }

      line = r.readLine();
      if (line == null || !line.equals(TYPE_PREFIX + keyType.name())) {
        throw new IndexStorageException(
            "Index key type mismatch in file: " + path + ", expected " + keyType.name());
      }

      while ((line = r.readLine()) != null) {
        if (line.isBlank() || line.startsWith("#")) {
          continue;
        }

        final ParsedEntry<T> parsed = parseEntry(line);
        tree.insert(parsed.key(), parsed.pointer());
      }

      return tree;
    } catch (IOException e) {
      throw new IndexStorageException("Unable to read index file: " + path, e);
    }
  }

  public boolean exists() {
    return Files.exists(path);
  }

  public void remove() {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      throw new IndexStorageException("Unable to remove index file: " + path, e);
    }
  }

  private String serializeEntry(final ResultEntry<T, RecordPointer> entry) {
    return keySerializer.apply(entry.key()) + "|" + serializePointer(entry.value());
  }

  private ParsedEntry<T> parseEntry(final String line) {
    final String[] parts = line.split("\\|", -1);
    if (parts.length != 2) {
      throw new IndexStorageException("Invalid index entry: " + line);
    }

    final T key;
    try {
      key = keyParser.apply(parts[0]);
    } catch (Exception e) {
      throw new IndexStorageException("Unable to parse index key from entry: " + line, e);
    }

    final RecordPointer pointer = parsePointer(parts[1]);
    return new ParsedEntry<>(key, pointer);
  }

  private String serializePointer(final RecordPointer pointer) {
    if (pointer instanceof LinePointer lp) {
      return Integer.toString(lp.line());
    }

    throw new IndexStorageException("Unsupported record pointer type: " + pointer.getClass().getName());
  }

  private RecordPointer parsePointer(final String raw) {
    try {
      return new LinePointer(Integer.parseInt(raw));
    } catch (NumberFormatException e) {
      throw new IndexStorageException("Invalid line pointer value: " + raw, e);
    }
  }

  private void moveAtomicallyIfPossible(final Path source, final Path target) {
    try {
      Files.move(
          source,
          target,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE
      );
    } catch (AtomicMoveNotSupportedException e) {
      try {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException ex) {
        throw new IndexStorageException("Unable to replace index file: " + target, ex);
      }
    } catch (IOException e) {
      throw new IndexStorageException("Unable to replace index file: " + target, e);
    }
  }

  private record ParsedEntry<T>(T key, dev.welbyseely.emu.dbms.storage.table.RecordPointer pointer) {
  }
}