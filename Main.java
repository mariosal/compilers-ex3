import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import syntaxtree.Goal;

public class Main {
  public static void main(String[] args) {
    for (int i = 0; i < args.length; ++i) {
      FileInputStream in = null;
      try {
        in = new FileInputStream(args[i]);
        MiniJavaParser parser = new MiniJavaParser(in);

        Goal goal = parser.Goal();
        SymbolVisitor symbol_visitor = new SymbolVisitor();
        goal.accept(symbol_visitor);
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
        } catch (IOException e) {
          System.err.println(e.getMessage());
        }
      }
    }
  }
}
