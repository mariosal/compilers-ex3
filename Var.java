import java.util.*;

public class Var {
  public String name;
  public VarType type;

  public Var(String name, VarType type) throws Exception {
    if (name.equals(type.name)) {
      throw new Exception("Var " + name + " cannot have the same name as its type");
    }
    this.name = name;
    this.type = type;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof Var)) {
      return false;
    }
    if (!name.equals(((Var)o).name)) {
      return false;
    }
    if (!type.equals(((Var)o).type)) {
      return false;
    }
    return true;
  }
}
