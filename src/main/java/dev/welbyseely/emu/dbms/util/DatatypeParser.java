package dev.welbyseely.emu.dbms.util;

import dev.welbyseely.emu.dbms.schema.DataType;

public class DatatypeParser {

  private DatatypeParser() {

  }

  public static Object parse(String raw, DataType type) {
    return switch (type) {
      case DataType.INTEGER -> raw.isEmpty() ? null : Integer.parseInt(raw);
      case DataType.FLOAT -> raw.isEmpty() ? null : Double.parseDouble(raw);
      case DataType.TEXT -> raw;
    };
  }

}
