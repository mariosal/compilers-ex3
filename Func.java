import java.util.*;

public class Func {
  public String name;
  public VarType type;
  public boolean isStatic;

  public HashMap<String, Var> params;
  public HashMap<String, Var> vars;

  public List<Var> oparams;
  public List<Var> ovars;

  public Func(String name, VarType type) throws Exception {
    this(name, type, false);
  }

  public Func(String name, VarType type, boolean isStatic) throws Exception {
    if (name.equals(type.name)) {
      throw new Exception("Method " + name + " cannot have the same name as its return type");
    }

    this.name = name;
    this.type = type;
    this.isStatic = isStatic;

    params = new HashMap<String, Var>();
    vars = new HashMap<String, Var>();
    oparams = new ArrayList<Var>();
    ovars = new ArrayList<Var>();
  }

  public void addParam(Var param) throws Exception {
    if (params.containsKey(param.name)) {
      throw new Exception("Parameter " + param.name + " is already defined in method " + name);
    }
    params.put(param.name, param);
    oparams.add(param);
  }

  public void addVar(Var var) throws Exception {
    if (vars.containsKey(var.name)) {
      throw new Exception("Variable " + var.name + " is already defined in method " + name);
    }
    vars.put(var.name, var);
    ovars.add(var);
  }

  public boolean defines(String var) throws Exception {
    return params.containsKey(var) || vars.containsKey(var);
  }

  public String size() {
    String ret = type.size() + " (i8*";
    int count_params = oparams.size();
    for (Var param : oparams) {
      ret += ",";
      if (count_params != oparams.size()) {
        ret += " ";
      }
      ret += param.type.size();
      --count_params;
    }
    ret += ")*";

    return ret;
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof Func)) {
      return false;
    }
    if (isStatic != ((Func)o).isStatic) {
      return false;
    }
    if (!name.equals(((Func)o).name)) {
      return false;
    }
    if (!type.equals(((Func)o).type)) {
      return false;
    }
    if (!oparams.equals(((Func)o).oparams)) {
      return false;
    }
    if (!ovars.equals(((Func)o).ovars)) {
      return false;
    }
    return true;
  }
}
