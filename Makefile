SRCS = Main.java SymbolVisitor.java Prog.java Cls.java Func.java Var.java\
       VarType.java Offset.java

BIN = Main.class

J = java
JC = javac

.PHONY = all gen clean

all : gen $(BIN)

gen :
	$(J) -jar jtb-1.3.2di.jar -te MiniJava.jj
	$(J) -jar javacc-5.0.jar MiniJava-jtb.jj

clean :
	$(RM) -r *.class syntaxtree visitor MiniJava-jtb.jj JavaCharStream.java\
		MiniJavaParserConstants.java MiniJavaParser.java\
		MiniJavaParserTokenManager.java ParseException.java Token.java\
		TokenMgrError.java

$(BIN) : $(SRCS)
	$(JC) $(JFLAGS) $^
