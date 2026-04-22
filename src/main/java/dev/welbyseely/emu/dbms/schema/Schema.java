package dev.welbyseely.emu.dbms.schema;

import java.util.Collections;
import java.util.List;

public record Schema(String schemaName, List<Attribute> attributes) {

  public Schema(final String schemaName, List<Attribute> attributes) {
    this.schemaName = schemaName;
    this.attributes = Collections.unmodifiableList(attributes);
  }

  public Attribute getAttribute(final String attrName) {
    return attributes.stream().filter(attr -> attrName.equals(attr.name()))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Can't find attribute with name=" + attrName));
  }

}
