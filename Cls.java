import java.util.*;

public class Cls {
  public boolean isMain;

  public String name;
  public String parent; // Extended class name

  public HashMap<String, Var> vars;
  public HashMap<String, Func> funcs;

  public HashMap<String, Integer> offsets;
  public int offset_var;
  public int offset_func;

  public List<Var> ovars;
  public List<Func> ofuncs;

  public Cls(String name, Prog prog) throws Exception {
    this(name, prog, "");
  }

  public Cls(String name, Prog prog, String parent) throws Exception {
    if (name.equals(parent)) {
      throw new Exception("Class " + name + " cannot extend itself");
    }
    if (!parent.isEmpty() && !prog.classes.containsKey(parent)) {
      throw new Exception("Extended class " + parent + " is not defined");
    }

    this.name = name;
    this.parent = parent;

    vars = new HashMap<String, Var>();
    funcs = new HashMap<String, Func>();

    ovars = new ArrayList<Var>();
    ofuncs = new ArrayList<Func>();

    offsets = new HashMap<String, Integer>();
  }

  public void addVar(Var var, Prog prog) throws Exception {
    if (vars.containsKey(var.name)) {
      throw new Exception("Data member " + var.name + " is already defined in class " + name);
    }

    Cls tmp = prog.classes.get(parent);
    while (tmp != null) {
      if (tmp.funcs.containsKey(var.name)) {
        throw new Exception("Variable " + var.name + " is defined as a method in class " + tmp.name);
      }

      tmp = prog.classes.get(tmp.parent);
    }

    vars.put(var.name, var);
    ovars.add(var);
  }

  public void addFunc(Func func, Prog prog) throws Exception {
    if (funcs.containsKey(func.name)) {
      throw new Exception("Method " + func.name + " is already defined in class " + name);
    }

    Cls tmp = prog.classes.get(parent);
    while (tmp != null) {
      if (tmp.vars.containsKey(func.name)) {
        throw new Exception("Method " + func.name + " is already defined as data member in class " + tmp.name);
      }
      if (tmp.funcs.containsKey(func.name) && !tmp.funcs.get(func.name).equals(func)) {
        throw new Exception("Overriden method " + func.name + " is defined differently in class " + tmp.name);
      }

      tmp = prog.classes.get(tmp.parent);
    }

    if (func.type.name.equals("void")) {
      isMain = true;
    }

    funcs.put(func.name, func);
    ofuncs.add(func);
  }

  public boolean defines(String var, Prog prog) throws Exception {
    if (vars.containsKey(var)) {
      return true;
    }
    if (!parent.isEmpty()) {
      return prog.classes.get(parent).defines(var, prog);
    }
    return false;
  }

  public Var getVar(String var, Prog prog) {
    if (vars.containsKey(var)) {
      return vars.get(var);
    }
    if (!parent.isEmpty()) {
      return prog.classes.get(parent).getVar(var, prog);
    }
    return null;
  }

  public int getOffset(String any, Prog prog) {
    if (offsets.containsKey(any)) {
      return offsets.get(any);
    }
    if (!parent.isEmpty()) {
      return prog.classes.get(parent).getOffset(any, prog);
    }
    return 0;
  }

  public boolean has(String func, Prog prog) {
    if (funcs.containsKey(func)) {
      return true;
    }
    if (!parent.isEmpty()) {
      return prog.classes.get(parent).has(func, prog);
    }
    return false;
  }

  public int countFuncs(Prog prog) {
    int ret = 0;
    if (!parent.isEmpty()) {
      ret += prog.classes.get(parent).countFuncs(prog);
    }
    for (Func func : ofuncs) {
      if (!func.isStatic && (parent.isEmpty() || !prog.classes.get(parent).has(func.name, prog))) {
        ++ret;
      }
    }
    return ret;
  }

  public List<Func> getFuncs(Prog prog) {
    if (parent.isEmpty()) {
      return new ArrayList<Func>(ofuncs);
    }

    List<Func> ret = new ArrayList<Func>();

    List<Func> pall_ofuncs = prog.classes.get(parent).getFuncs(prog);
    for (Func func : pall_ofuncs) {
      if (funcs.containsKey(func.name)) {
        ret.add(funcs.get(func.name));
      } else {
        ret.add(func);
      }
    }
    return ret;
  }

  public Cls funcOwner(String func, Prog prog) {
    if (funcs.containsKey(func)) {
      return this;
    }
    if (!parent.isEmpty()) {
      return prog.classes.get(parent).funcOwner(func, prog);
    }
    return null;
  }

  public void countOffset(Prog prog, HashSet<String> visited) throws Exception {
    if (!visited.add(name)) {
      return;
    }

    HashMap<String, Integer> bytes = new HashMap<String, Integer>();
    bytes.put("boolean", 1);
    bytes.put("int", 4);

    if (!parent.isEmpty()) {
      prog.classes.get(parent).countOffset(prog, visited);
      offset_var = prog.classes.get(parent).offset_var;
      offset_func = prog.classes.get(parent).offset_func;
    }

    for (Var var : ovars) {
      offsets.put(var.name, offset_var);
      if (var.type.isObj()) {
        offset_var += 8;
      } else {
        offset_var += bytes.get(var.type.name);
      }
    }

    for (Func func : ofuncs) {
      if (!func.isStatic && (parent.isEmpty() || !prog.classes.get(parent).has(func.name, prog))) {
        offsets.put(func.name, offset_func);
        offset_func += 8;
      }
    }
  }
}
