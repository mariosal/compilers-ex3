import java.util.*;

public class VarType {
  public String name;
  public boolean isArray;
  public int length;

  public VarType() {
  }

  public VarType(String name, Prog prog) {
    this(name, prog, false);
  }

  public VarType(String name, Prog prog, boolean isArray) {
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

  public String size() {
    String ret;
    if (name.equals("boolean")) {
      ret = "i1";
    } else if (name.equals("void") || name.equals("int")) {
      ret = "i32";
    } else {
      ret = "i8";
    }
    if (isObj()) {
      ret += "*";
    }
    return ret;
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
