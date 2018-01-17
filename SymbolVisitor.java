import syntaxtree.*;
import visitor.DepthFirstVisitor;

public class SymbolVisitor extends DepthFirstVisitor {
  public Prog prog;

  private Cls cur_cls;
  private Func cur_func;
  private VarType cur_type;
  private boolean single_ident;

  public SymbolVisitor() throws Exception {
    prog = new Prog();
  }

  public void printOffset() throws Exception {
    for (Cls cls : prog.oclasses) {
      if (cls.isMain || cls.name.equals("String")) {
        continue;
      }

      System.out.println("-----------Class " + cls.name + "-----------");
      System.out.println("--Variables---");
      for (Var var : cls.ovars) {
        System.out.println(cls.name + "." + var.name + " : " + cls.offsets.get(var.name));
      }
      System.out.println("---Methods---");
      for (Func func : cls.ofuncs) {
        if (cls.parent.isEmpty() || !prog.classes.get(cls.parent).has(func.name, prog)) {
          System.out.println(cls.name + "." + func.name + " : " + cls.offsets.get(func.name));
        }
      }

      System.out.println();
    }
  }

  /**
   * f0 -> MainClass()
   * f1 -> ( TypeDeclaration() )*
   * f2 -> <EOF>
   */
  @Override public void visit(Goal n) throws Exception {
    prog.addCls(new Cls("String", prog));

    n.f0.accept(this);
    n.f1.accept(this);

    prog.checkTypes();
    prog.countOffset();
  }

  /**
   * f0 -> "class"
   * f1 -> Identifier()
   * f2 -> "{"
   * f3 -> "public"
   * f4 -> "static"
   * f5 -> "void"
   * f6 -> "main"
   * f7 -> "("
   * f8 -> "String"
   * f9 -> "["
   * f10 -> "]"
   * f11 -> Identifier()
   * f12 -> ")"
   * f13 -> "{"
   * f14 -> ( VarDeclaration() )*
   * f15 -> ( Statement() )*
   * f16 -> "}"
   * f17 -> "}"
   */
  @Override public void visit(MainClass n) throws Exception {
    Cls cls = new Cls(n.f1.f0.toString(), prog);
    prog.addCls(cls);

    VarType type = new VarType("void", prog);
    Func func = new Func("main", type, true);
    type = new VarType("String", prog, true);
    Var param = new Var(n.f11.f0.toString(), type);
    func.addParam(param);
    cls.addFunc(func, prog);

    cur_cls = cls;
    cur_func = func;
    n.f14.accept(this);
    n.f15.accept(this);
    cur_func = null;
    cur_cls = null;
  }

  /**
   * f0 -> "class"
   * f1 -> Identifier()
   * f2 -> "{"
   * f3 -> ( VarDeclaration() )*
   * f4 -> ( MethodDeclaration() )*
   * f5 -> "}"
   */
  @Override public void visit(ClassDeclaration n) throws Exception {
    Cls cls = new Cls(n.f1.f0.toString(), prog);
    prog.addCls(cls);

    cur_cls = cls;
    n.f3.accept(this);
    n.f4.accept(this);
    cur_cls = null;
  }

  /**
   * f0 -> "class"
   * f1 -> Identifier()
   * f2 -> "extends"
   * f3 -> Identifier()
   * f4 -> "{"
   * f5 -> ( VarDeclaration() )*
   * f6 -> ( MethodDeclaration() )*
   * f7 -> "}"
   */
  @Override public void visit(ClassExtendsDeclaration n) throws Exception {
    Cls cls = new Cls(n.f1.f0.toString(), prog, n.f3.f0.toString());
    prog.addCls(cls);

    cur_cls = cls;
    n.f5.accept(this);
    n.f6.accept(this);
    cur_cls = null;
  }

  /**
   * f0 -> Type()
   * f1 -> Identifier()
   * f2 -> ";"
   */
  @Override public void visit(VarDeclaration n) throws Exception {
    VarType type = new VarType();
    cur_type = type;
    n.f0.accept(this);
    type.check(prog);
    cur_type = null;

    Var var = new Var(n.f1.f0.toString(), type);
    if (cur_func != null) {
      cur_func.addVar(var);
    } else if (cur_cls != null) {
      cur_cls.addVar(var, prog);
    }
  }

  /**
   * f0 -> "public"
   * f1 -> Type()
   * f2 -> Identifier()
   * f3 -> "("
   * f4 -> ( FormalParameterList() )?
   * f5 -> ")"
   * f6 -> "{"
   * f7 -> ( VarDeclaration() )*
   * f8 -> ( Statement() )*
   * f9 -> "return"
   * f10 -> Expression()
   * f11 -> ";"
   * f12 -> "}"
   */
  @Override public void visit(MethodDeclaration n) throws Exception {
    VarType type = new VarType();
    cur_type = type;
    n.f1.accept(this);
    type.check(prog);
    cur_type = null;

    Func func = new Func(n.f2.f0.toString(), type);
    cur_func = func;
    n.f4.accept(this);
    n.f7.accept(this);
    n.f8.accept(this);
    n.f10.accept(this);
    cur_func = null;

    cur_cls.addFunc(func, prog);
  }

  /**
   * f0 -> Type()
   * f1 -> Identifier()
   */
  @Override public void visit(FormalParameter n) throws Exception {
    VarType type = new VarType();
    cur_type = type;
    n.f0.accept(this);
    type.check(prog);
    cur_type = null;

    Var param = new Var(n.f1.f0.toString(), type);
    cur_func.addParam(param);
  }

  /**
   * f0 -> "int"
   * f1 -> "["
   * f2 -> "]"
   */
  @Override public void visit(ArrayType n) throws Exception {
    cur_type.name = "int";
    cur_type.isArray = true;
  }

  /**
   * f0 -> "boolean"
   */
  @Override public void visit(BooleanType n) throws Exception {
    cur_type.name = "boolean";
    n.f0.accept(this);
  }

  /**
   * f0 -> "int"
   */
  @Override public void visit(IntegerType n) throws Exception {
    cur_type.name = "int";
    n.f0.accept(this);
  }

  /**
   * f0 -> Identifier()
   * f1 -> "="
   * f2 -> Expression()
   * f3 -> ";"
   */
  @Override public void visit(AssignmentStatement n) throws Exception {
    if (!cur_func.defines(n.f0.f0.toString()) && !cur_cls.defines(n.f0.f0.toString(), prog)) {
      throw new Exception("Undefined identifier " + n.f0.f0.toString());
    }
    n.f2.accept(this);
  }

  /**
   * f0 -> Identifier()
   * f1 -> "["
   * f2 -> Expression()
   * f3 -> "]"
   * f4 -> "="
   * f5 -> Expression()
   * f6 -> ";"
   */
  @Override public void visit(ArrayAssignmentStatement n) throws Exception {
    if (!cur_func.defines(n.f0.f0.toString()) && !cur_cls.defines(n.f0.f0.toString(), prog)) {
      throw new Exception("Undefined identifier " + n.f0.f0.toString());
    }
    n.f2.accept(this);
    n.f5.accept(this);
  }

  /**
   * f0 -> IntegerLiteral()
   *       | TrueLiteral()
   *       | FalseLiteral()
   *       | Identifier()
   *       | ThisExpression()
   *       | ArrayAllocationExpression()
   *       | AllocationExpression()
   *       | BracketExpression()
   */
  @Override public void visit(PrimaryExpression n) throws Exception {
    single_ident = true;
    n.f0.accept(this);
    single_ident = false;
  }

  /**
   * f0 -> <IDENTIFIER>
   */
  public void visit(Identifier n) throws Exception {
    if (cur_type != null) {
      cur_type.name = n.f0.toString();
    } else if (single_ident) {
      if (!cur_func.defines(n.f0.toString()) && !cur_cls.defines(n.f0.toString(), prog)) {
        throw new Exception("Undefined identifier " + n.f0.toString());
      }
    }
  }

  /**
   * f0 -> "new"
   * f1 -> Identifier()
   * f2 -> "("
   * f3 -> ")"
   */
  @Override public void visit(AllocationExpression n) throws Exception {
    if (!prog.classes.containsKey(n.f1.f0.toString()) && !prog.types.contains(n.f1.f0.toString())) {
      prog.types.add(n.f1.f0.toString());
    }
  }
}
