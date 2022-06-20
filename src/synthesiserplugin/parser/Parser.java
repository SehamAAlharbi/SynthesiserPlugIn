package synthesiserplugin.parser;

import org.eclipse.jdt.core.ICompilationUnit;			
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class Parser {
	
	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java source file
	 * @param icu the ICompilationUnit 
	 * @return CompilationUnit, the parsed version
	 */
	public CompilationUnit parse(ICompilationUnit icu) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		return (CompilationUnit) parser.createAST(null);
		
	}
}
