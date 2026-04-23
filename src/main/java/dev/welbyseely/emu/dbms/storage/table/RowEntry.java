package dev.welbyseely.emu.dbms.storage.table;

import dev.welbyseely.emu.dbms.table.Row;

public record RowEntry(RecordPointer pointer, Row row) {

}
