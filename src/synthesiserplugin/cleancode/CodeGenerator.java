package synthesiserplugin.cleancode;

import java.util.List;		
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import synthesiserplugin.parser.Parser;

public class CodeGenerator {

	private IPackageFragment documentationPackage;

	public CodeGenerator(IPackageFragment documentationPackage) {
		this.documentationPackage = documentationPackage;
	}
	
	public CodeGenerator() {
		
	}

	/**
	 * 
	 * @param preview the returned value from proposal.getPreviewContent()
	 * @throws CoreException
	 */
	public ICompilationUnit generate(String codeContent, String icuName) throws CoreException {

		// convert the returned string into code and create a java file out of it
		Pattern NEWLINE = Pattern.compile("\\R");
		String lines[] = NEWLINE.split(codeContent);
		StringBuffer buffer = new StringBuffer();
		for (String line : lines) {
			if (line == lines[0]) {
				buffer.append("package documentation.usage.examples; \n\n");
				continue;
			}
			buffer.append(line + "\n");
		}
		
		return process(buffer.toString(), icuName);

	}

	/**
	 * for code with no dead code problems
	 * 
	 * @param icu
	 * @return
	 * @throws JavaModelException
	 */
	public ICompilationUnit placeInDocPackage(ICompilationUnit icu) throws JavaModelException {

		String oldName = icu.getElementName();
		// to remove the .java from the original icu name
		String newICUName = getNewName(oldName);
		ICompilationUnit newICU = documentationPackage.createCompilationUnit(newICUName, icu.toString(), false, null);

		return newICU;
	}

	/**
	 * a method for the second proposed solution, no UI elements needed
	 */
	public void generateCode(ICompilationUnit icu) {

		CompilationUnit cu = polishCode(icu);
		try {
			generate(cu.toString(), cu.getJavaElement().getElementName());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * to remove annotations from generated code
	 * @param icu
	 * @return
	 */
	public CompilationUnit polishCode(ICompilationUnit icu) {

		CompilationUnit cu = new Parser().parse(icu);

		cu.accept(new ASTVisitor() {

			public boolean visit(MethodDeclaration node) {

			
				List<ASTNode> modifiers = (List<ASTNode>) node.getStructuralProperty(node.getModifiersProperty());
				modifiers.stream().forEach(modifier -> {
					if (modifier instanceof Annotation) {
						Annotation annotation = (Annotation) modifier;
						String typeName = annotation.getTypeName().toString();
						if (typeName.equals("Utility")) {
							node.delete();

						}

						else if (typeName.equals("Documentation")) {
							modifier.delete();
						}
					}
				});

				return true;
			}

		});

		return cu;
	}

	/**
	 * for code with dead code problems
	 * 
	 * @param codeBuffer
	 * @param icuName
	 * @return
	 * @throws JavaModelException
	 */
	private ICompilationUnit process(String codeContent, String icuName) throws JavaModelException {

		//format code 
		String content = formatCode(codeContent);
		String newICUName = getNewName(icuName);
		
		// generate a new java file under the documentation package
		ICompilationUnit icu = documentationPackage.createCompilationUnit(icuName, content, false, null);

		return icu;
	}
	
	
	private String getNewName(String icuName) {
		// to remove the .java from the original icu name
		String newICUName = icuName.substring(0, icuName.length() - 5) + "Doc.java";
		return newICUName;
	}

	/**
	 * formats a String as Java Code
	 * @param code
	 * @return
	 */
	private String formatCode(String code) {

		CodeFormatter codeFormatter = ToolFactory.createCodeFormatter(null);

		TextEdit textEdit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, code, 0, code.length(), 0, null);
		IDocument doc = new Document(code);
		try {
			textEdit.apply(doc);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		return doc.get();
	}
	
	/**
	 * rename the class to match its new name under documentation name, the project needs to be reprocessed in order 
	 * for this method to successfully rename newly listed .java files i.e. those under documentation package
	 * @param icu
	 * @throws JavaModelException 
	 */
	public static void refactorClassName(ICompilationUnit icu, String newName) throws JavaModelException {
		
		

		RefactoringContribution contribution = RefactoringCore
				.getRefactoringContribution(IJavaRefactorings.RENAME_COMPILATION_UNIT);
		RenameJavaElementDescriptor descriptor = (RenameJavaElementDescriptor) contribution.createDescriptor();
		descriptor.setProject(icu.getResource().getProject().getName());
		descriptor.setNewName(newName);
		descriptor.setJavaElement(icu);
		descriptor.setUpdateReferences(true);
	

		RefactoringStatus status = new RefactoringStatus();
		try {
			Refactoring refactoring = descriptor.createRefactoring(status);

			IProgressMonitor monitor = new NullProgressMonitor();
			refactoring.checkInitialConditions(monitor);
			refactoring.checkFinalConditions(monitor);
			Change change = refactoring.createChange(monitor);
			change.perform(monitor);

		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
