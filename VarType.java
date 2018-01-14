import java.util.*;

public class VarType {
  public String name;
  public boolean isArray;
  public int length;

  public VarType() throws Exception {
  }

  public VarType(String name, Prog prog) throws Exception {
    this(name, prog, false);
  }

  public VarType(String name, Prog prog, boolean isArray) throws Exception {
    this.name = name;
    this.isArray = isArray;

    check(prog);
  }

  public void check(Prog prog) {
    if (!name.equals("void") && !name.equals("int") && !name.equals("boolean")) {
      prog.types.add(name);
    }
  }

  public boolean isObj() {
    if (isArray) {
      return true;
    }
    if (name.equals("void") || name.equals("int") || name.equals("boolean")) {
      return false;
    }
    return true;
  }

  public void setLength(int length) throws Exception {
    if (length < 0) {
      throw new Exception("Negative array length");
    }
    this.length = length;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof VarType)) {
      return false;
    }
    if (isArray != ((VarType)o).isArray) {
      return false;
    }
    if (!name.equals(((VarType)o).name)) {
      return false;
    }
    return true;
  }
};
