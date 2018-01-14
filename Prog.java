import java.util.*;

public class Prog {
  public HashMap<String, Cls> classes;
  public HashSet<String> types;
  public List<Cls> oclasses;

  public HashMap<String, Offset> offsets;

  public Prog() throws Exception {
    classes = new HashMap<String, Cls>();
    types = new HashSet<String>();
    oclasses = new ArrayList<Cls>();
    offsets = new HashMap<String, Offset>();
  }

  public void addCls(Cls cls) throws Exception {
    if (classes.containsKey(cls.name)) {
      throw new Exception("Class " + cls.name + " is already defined");
    }
    classes.put(cls.name, cls);
    oclasses.add(cls);
  }

  public void checkTypes() throws Exception {
    for (String type : types) {
      if (!classes.containsKey(type)) {
        throw new Exception("Class " + type + " is not defined");
      }
    }
  }

  public void countOffset() throws Exception {
    HashSet<String> visited = new HashSet<String>();
    for (Cls cls : oclasses) {
      cls.countOffset(this, visited);
    }
  }
}
