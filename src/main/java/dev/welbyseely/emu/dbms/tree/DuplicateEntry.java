package dev.welbyseely.emu.dbms.tree;

public class DuplicateEntry extends RuntimeException {

  private Object key;

  public DuplicateEntry(Object key) {
    super(String.format("Duplicate key %s", key));
    this.key = key;
  }

  public Object getKey() {
    return key;
  }
}
