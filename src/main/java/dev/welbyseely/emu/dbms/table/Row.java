package dev.welbyseely.emu.dbms.table;

import java.util.Map;

public record Row(Map<String, Object> values) {

  public Row {
    values = Map.copyOf(values);
  }

  public Object get(String name) {
    return values.get(name);
  }
}