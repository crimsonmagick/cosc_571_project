package dev.welbyseely.emu.dbms.schema;

import java.util.Collections;
import java.util.List;

public record Schema(String schemaName, List<Attribute> attributes) {

  public Schema(final String schemaName, List<Attribute> attributes) {
    this.schemaName = schemaName;
    this.attributes = Collections.unmodifiableList(attributes);
  }

}
