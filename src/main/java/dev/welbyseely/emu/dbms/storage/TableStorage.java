package dev.welbyseely.emu.dbms.storage;

import dev.welbyseely.emu.dbms.schema.Schema;
import dev.welbyseely.emu.dbms.table.Row;

public interface TableStorage {

  RecordPointer insert(Row row);

  Row read(RecordPointer pointer);

  Iterable<RecordPointer> scan();

  Schema getSchema();

  void remove();

  boolean isRemoved();
}