import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import visitor.*;
import syntaxtree.Goal;

public class Main {
  public static void main(String[] args) {
    for (int i = 0; i < args.length; ++i) {
      FileInputStream in = null;
      PrintWriter out = null;
      try {
        in = new FileInputStream(args[i]);
        MiniJavaParser parser = new MiniJavaParser(in);

        Goal goal = parser.Goal();
        SymbolVisitor symbol_visitor = new SymbolVisitor();
        goal.accept(symbol_visitor);

        String[] outArr = args[i].split("\\.");
        outArr[outArr.length - 1] = "ll";
        out = new PrintWriter(String.join(".", outArr), "UTF-8");

        LlvmIr<String,String> llvm_ir = new LlvmIr<>(out, symbol_visitor.prog);
        goal.accept(llvm_ir, "");
      } catch (ParseException e) {
        System.err.println(e.getMessage());
      } catch (FileNotFoundException e) {
        System.err.println(e.getMessage());
      } catch (Exception e) {
        System.err.println(e.getMessage());
      } finally {
        try {
          if (in != null) {
            in.close();
          }
          if (out != null) {
            out.close();
          }
        } catch (IOException e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
