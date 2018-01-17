import java.io.PrintWriter;
import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class LlvmIr<R,A> extends GJDepthFirst<R,A> {
  private PrintWriter out;
  private Prog prog;

  private Cls cur_cls;

  private int count;
  private int indent;

  public LlvmIr(PrintWriter out, Prog prog) {
    super();
    this.out = out;
    this.prog = prog;
  }

  public String tabs() {
    String ret = "";
    for (int i = 0; i < indent; ++i) {
      ret += "\t";
    }
    return ret;
  }

  public void printGlobal() {
    for (Cls cls : prog.oclasses) {
      if (cls.name.equals("String")) {
        continue;
      }

      int count_funcs = cls.ofuncs.size();
      if (cls.isMain) {
        --count_funcs;
      }
      out.printf("@.%s_vtable = global [%d x i8*] [", cls.name, count_funcs);

      for (Func func : cls.ofuncs) {
        if (func.isStatic) {
          continue;
        }

        out.printf("i8* bitcast (%s @%s.%s to i8*)", func.size(), cls.name, func.name);
        if (count_funcs > 1) {
          out.print(", ");
        }
        --count_funcs;
      }

      out.print("]\n");
    }
  }

  public void printHelper() {
    out.print("\n\ndeclare i8* @calloc(i32, i32)\ndeclare i32 @printf(i8*, ...)\ndeclare void @exit(i32)\n\n@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\ndefine void @print_int(i32 %i) {\n    %_str = bitcast [4 x i8]* @_cint to i8*\n    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n    ret void\n}\n\ndefine void @throw_oob() {\n    %_str = bitcast [15 x i8]* @_cOOB to i8*\n    call i32 (i8*, ...) @printf(i8* %_str)\n    call void @exit(i32 1)\n    ret void\n}\n");
  }

  /**
   * f0 -> MainClass()
   * f1 -> ( TypeDeclaration() )*
   * f2 -> <EOF>
   */
  @Override public R visit(Goal n, A argu) throws Exception {
    printGlobal();
    printHelper();

    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return _ret;
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
  @Override public R visit(MainClass n, A argu) throws Exception {
    count = 0;
    indent = 1;

    out.print("\ndefine i32 @main() {\n");
    R _ret=null;
    n.f14.accept(this, argu);
    n.f15.accept(this, argu);
    out.print("\n\tret i32 0\n}\n");
    return _ret;
  }

  /**
   * f0 -> "class"
   * f1 -> Identifier()
   * f2 -> "{"
   * f3 -> ( VarDeclaration() )*
   * f4 -> ( MethodDeclaration() )*
   * f5 -> "}"
   */
  @Override public R visit(ClassDeclaration n, A argu) throws Exception {
    R _ret=null;
    cur_cls = prog.classes.get(n.f1.f0.toString());
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    return _ret;
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
  @Override public R visit(ClassExtendsDeclaration n, A argu) throws Exception {
    R _ret=null;
    cur_cls = prog.classes.get(n.f1.f0.toString());
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
    return _ret;
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
  @Override public R visit(MethodDeclaration n, A argu) throws Exception {
    R _ret=null;
    count = 0;
    indent = 1;

    Func cur_func = cur_cls.funcs.get(n.f2.f0.toString());
    out.printf("\ndefine %s @%s.%s(i8* %%this", cur_func.type.size(), cur_cls.name, cur_func.name);
    int count_params = cur_func.oparams.size();
    for (Var param : cur_func.oparams) {
      out.printf(", %s %%.%s", param.type.size(), param.name);
    }
    out.printf(") {\n");

    for (Var param : cur_func.oparams) {
      if (cur_func.vars.containsKey(param.name)) {
        continue;
      }

      out.printf("\t%%%s = alloca %s\n", param.name, param.type.size());
      out.printf("\tstore %s %%.%s, %s* %%%s\n", param.type.size(), param.name, param.type.size(), param.name);
    }

    for (Var var : cur_func.ovars) {
      out.printf("\t%%%s = alloca %s\n\n", var.name, var.type.size());
    }

    n.f8.accept(this, argu);

    n.f10.accept(this, argu);
    out.printf("\tret %s %s\n", cur_func.type.size(), "");

    out.print("}\n");
    return _ret;
  }

  /**
   * f0 -> Block()
   *       | AssignmentStatement()
   *       | ArrayAssignmentStatement()
   *       | IfStatement()
   *       | WhileStatement()
   *       | PrintStatement()
   */
  @Override public R visit(Statement n, A argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
   * f0 -> "{"
   * f1 -> ( Statement() )*
   * f2 -> "}"
   */
  @Override public R visit(Block n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> Identifier()
   * f1 -> "="
   * f2 -> Expression()
   * f3 -> ";"
   */
  @Override public R visit(AssignmentStatement n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    return _ret;
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
  @Override public R visit(ArrayAssignmentStatement n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> "if"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> Statement()
   * f5 -> "else"
   * f6 -> Statement()
   */
  @Override public R visit(IfStatement n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    n.f6.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> "while"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> Statement()
   */
  @Override public R visit(WhileStatement n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> "System.out.println"
   * f1 -> "("
   * f2 -> Expression()
   * f3 -> ")"
   * f4 -> ";"
   */
  @Override public R visit(PrintStatement n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> AndExpression()
   *       | CompareExpression()
   *       | PlusExpression()
   *       | MinusExpression()
   *       | TimesExpression()
   *       | ArrayLookup()
   *       | ArrayLength()
   *       | MessageSend()
   *       | Clause()
   */
  @Override public R visit(Expression n, A argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
   * f0 -> Clause()
   * f1 -> "&&"
   * f2 -> Clause()
   */
  @Override public R visit(AndExpression n, A argu) throws Exception {
    String lhs = (String)n.f0.accept(this, argu);
    int tmp = count;
    count += 4;

    out.printf("%sbr label %%andclause%d\n\n", tabs(), tmp + 1);

    out.printf("andclause%d:\n", tmp + 1);
    out.printf("%sbr i1 %s, label %%andclause%d, label %%andclause%d\n\n", tabs(), lhs, tmp + 2, tmp + 4);

    out.printf("andclause%d:\n", tmp + 2);
    String rhs = (String)n.f2.accept(this, argu);
    out.printf("%sbr label %%andclause%d\n\n", tabs(), tmp + 3);

    out.printf("andclause%d:\n", tmp + 3);
    out.printf("%sbr label %%andclause%d\n\n", tabs(), tmp + 4);

    out.printf("andclause%d:\n", tmp + 4);
    out.printf("%s%%_%d = phi i1 [ 0, %%andclause%d ], [ %s, %%andclause%d ]\n", tabs(), tmp, tmp + 1, rhs, tmp + 2);
    return (R)("%_" + tmp);
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "<"
   * f2 -> PrimaryExpression()
   */
  @Override public R visit(CompareExpression n, A argu) throws Exception {
    int tmp = count;
    ++count;

    String lhs = (String)n.f0.accept(this, argu);
    out.printf("%s%%_%d = load i32, i32* %s\n", tabs(), tmp, lhs);

    String rhs = (String)n.f2.accept(this, argu);
    out.printf("%s%%_%d = icmp slt i32 %%_%d, %s\n", tabs(), count, tmp, rhs);

    R _ret = (R)("%_" + count);
    ++count;
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "+"
   * f2 -> PrimaryExpression()
   */
  @Override public R visit(PlusExpression n, A argu) throws Exception {
    int tmp = count;
    ++count;

    String lhs = (String)n.f0.accept(this, argu);
    out.printf("%s%%_%d = load i32, i32* %s\n", tabs(), tmp, lhs);

    String rhs = (String)n.f2.accept(this, argu);
    out.printf("%s%%_%d = add i32 %%_%d, %s\n", tabs(), count, tmp, rhs);

    R _ret = (R)("%_" + count);
    ++count;
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "-"
   * f2 -> PrimaryExpression()
   */
  @Override public R visit(MinusExpression n, A argu) throws Exception {
    int tmp = count;
    ++count;

    String lhs = (String)n.f0.accept(this, argu);
    out.printf("%s%%_%d = load i32, i32* %s\n", tabs(), tmp, lhs);

    String rhs = (String)n.f2.accept(this, argu);
    out.printf("%s%%_%d = sub i32 %%_%d, %s\n", tabs(), count, tmp, rhs);

    R _ret = (R)("%_" + count);
    ++count;
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "*"
   * f2 -> PrimaryExpression()
   */
  @Override public R visit(TimesExpression n, A argu) throws Exception {
    int tmp = count;
    ++count;

    String lhs = (String)n.f0.accept(this, argu);
    out.printf("%s%%_%d = load i32, i32* %s\n", tabs(), tmp, lhs);

    String rhs = (String)n.f2.accept(this, argu);
    out.printf("%s%%_%d = mul i32 %%_%d, %s\n", tabs(), count, tmp, rhs);

    R _ret = (R)("%_" + count);
    ++count;
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "["
   * f2 -> PrimaryExpression()
   * f3 -> "]"
   */
  @Override public R visit(ArrayLookup n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "."
   * f2 -> "length"
   */
  @Override public R visit(ArrayLength n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> PrimaryExpression()
   * f1 -> "."
   * f2 -> Identifier()
   * f3 -> "("
   * f4 -> ( ExpressionList() )?
   * f5 -> ")"
   */
  @Override public R visit(MessageSend n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    n.f2.accept(this, argu);
    n.f3.accept(this, argu);
    n.f4.accept(this, argu);
    n.f5.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> Expression()
   * f1 -> ExpressionTail()
   */
  @Override public R visit(ExpressionList n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return _ret;
  }

  /**
   * f0 -> ( ExpressionTerm() )*
   */
  @Override public R visit(ExpressionTail n, A argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
   * f0 -> ","
   * f1 -> Expression()
   */
  @Override public R visit(ExpressionTerm n, A argu) throws Exception {
    R _ret=null;
    n.f0.accept(this, argu);
    n.f1.accept(this, argu);
    return _ret;
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
  @Override public R visit(PrimaryExpression n, A argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
   * f0 -> <INTEGER_LITERAL>
   */
  @Override public R visit(IntegerLiteral n, A argu) throws Exception {
    return (R)n.f0.toString();
  }

  /**
   * f0 -> "true"
   */
  @Override public R visit(TrueLiteral n, A argu) throws Exception {
    return (R)"1";
  }

  /**
   * f0 -> "false"
   */
  @Override public R visit(FalseLiteral n, A argu) throws Exception {
    return (R)"0";
  }

  /**
   * f0 -> <IDENTIFIER>
   */
  @Override public R visit(Identifier n, A argu) throws Exception {
    return n.f0.accept(this, argu);
  }

  /**
   * f0 -> "this"
   */
  @Override public R visit(ThisExpression n, A argu) throws Exception {
    return (R)"%this";
  }

  /**
   * f0 -> "new"
   * f1 -> "int"
   * f2 -> "["
   * f3 -> Expression()
   * f4 -> "]"
   */
  @Override public R visit(ArrayAllocationExpression n, A argu) throws Exception {
    int tmp = count;
    count += 7;
    String size = (String)n.f3.accept(this, argu);

    out.printf("%s%%_%d = load i32, i32* %s\n", tabs(), tmp + 6, size);
    out.printf("%s%%_%d = icmp slt i32 %%_%d, 0\n", tabs(), tmp + 3, tmp + 6);
    out.printf("%sbr i1 %%_%d, label %%arr_alloc%d, label %%arr_alloc%d\n\n", tabs(), tmp + 3, tmp + 4, tmp + 5);

    out.printf("arr_alloc%d:\n", tmp + 4);
    out.printf("%scall void @throw_oob()\n", tabs());
    out.printf("%sbr label %%arr_alloc%d\n\n", tabs(), tmp + 5);

    out.printf("arr_alloc%d:\n", tmp + 5);
    out.printf("%s%%_%d = add i32 %%_%d, 1\n", tabs(), tmp, tmp + 6);
    out.printf("%s%%_%d = call i8* @calloc(i32 4, i32 %%_%d)\n", tabs(), tmp + 1, tmp);
    out.printf("%s%%_%d = bitcast i8* %%_%d to i32*\n", tabs(), tmp + 2, tmp + 1);
    out.printf("%sstore i32 %%_%d, i32* %%_%d\n", tabs(), tmp + 6, tmp + 2);

    return (R)("%_" + (tmp + 2));
  }

  /**
   * f0 -> "new"
   * f1 -> Identifier()
   * f2 -> "("
   * f3 -> ")"
   */
  @Override public R visit(AllocationExpression n, A argu) throws Exception {
    R _ret = (R)("%_" + count);
    Cls new_cls = prog.classes.get(n.f1.f0.toString());

    out.printf("%s%%_%d = call i8* @calloc(i32 1, i32 %d)\n", tabs(), count, new_cls.offset_var + 8);
    ++count;

    out.printf("%s%%_%d = bitcast i8* %%_%d to i8***\n", tabs(), count, count - 1);
    ++count;

    int count_funcs = new_cls.funcs.size();
    if (new_cls.isMain) {
      --count_funcs;
    }
    out.printf("%s%%_%d = getelementptr [%d x i8*], [%d x i8*]* @.%s_vtable, i32 0, i32 0\n", tabs(), count, count_funcs, count_funcs, new_cls.name);
    out.printf("%sstore i8** %%_%d, i8*** %%_%d\n", tabs(), count, count - 1);
    ++count;
    return _ret;
  }

  /**
   * f0 -> "!"
   * f1 -> Clause()
   */
  @Override public R visit(NotExpression n, A argu) throws Exception {
    int tmp = count;
    String rhs = (String)n.f1.accept(this, argu);
    out.printf("%s%%_%d = xor i1 1, %s\n", tabs(), count, rhs);
    return (R)("%_" + tmp);
  }

  /**
   * f0 -> "("
   * f1 -> Expression()
   * f2 -> ")"
   */
  @Override public R visit(BracketExpression n, A argu) throws Exception {
    return n.f1.accept(this, argu);
  }
}
