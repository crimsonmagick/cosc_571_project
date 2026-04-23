package dev.welbyseely.emu.dbms.table;

import java.util.LinkedHashMap;

public record Row(LinkedHashMap<String, Object> values) {

  public Row {
    values = new LinkedHashMap<>(values);
  }

  public Object get(String name) {
    return values.get(name);
  }
}