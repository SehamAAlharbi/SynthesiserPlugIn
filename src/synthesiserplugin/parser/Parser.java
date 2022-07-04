package synthesiserplugin.parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class Parser {

	/**
	 * Reads a ICompilationUnit and creates the AST DOM for manipulating the Java
	 * source file
	 * 
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
	
	public CompilationUnit parseAndRename(ICompilationUnit icu, String newName) {
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(icu);
		parser.setResolveBindings(true);
		parser.setUnitName(newName);
		return (CompilationUnit) parser.createAST(null);

	}

	/**
	 * AST from a string of code
	 * @param str
	 */
	public static CompilationUnit parse(String string) {
		
		// it does not recognise compiler warnings
		ASTParser parser = ASTParser.newParser(AST.getJLSLatest());
		parser.setSource(string.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
		parser.setCompilerOptions(options);

		String[] classpath = {"", "", ""};
		String[] sourcepath = {""};

		parser.setEnvironment(classpath, sourcepath, null, true);
		parser.setUnitName("");
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		

		return (CompilationUnit) parser.createAST(null);

	}

	/**
	 * read file content into a string
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	public static String readFileToString(String filePath) throws IOException {
		StringBuilder fileData = new StringBuilder(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));

		char[] buf = new char[10];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			System.out.println(numRead);
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}

		reader.close();

		return fileData.toString();
	}
}
