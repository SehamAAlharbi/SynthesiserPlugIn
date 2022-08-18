package synthesiserplugin.transformers;

import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring.Mode;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import synthesiserplugin.deadcode.DeadCodeDetector;
import synthesiserplugin.models.JavaProject;
import synthesiserplugin.visitors.MethodDeclarationVisitor;

@SuppressWarnings("restriction")
public class MethodDeclarationTransformer {

	private JavaProject javaProject;
	private ICompilationUnit updatedICU;

	public MethodDeclarationTransformer(JavaProject javaProject) {
		this.javaProject = javaProject;
	}

	/**
	 * @param name of the CU where all the utility invocations in all of its doc
	 *             methods will be in-lined
	 * @throws JavaModelException
	 */
	public void inlineAllDocIn(String name) throws JavaModelException {

		// you are retrieving the CU by its name, what if there is more than one but in different packages?
		ICompilationUnit icu = this.javaProject.getICUByName(name);
		// get a working copy to work with
//		ICompilationUnit workingCopy = this.javaProject.getWorkingCopy(icu);
		// perform in-lining
		this.updatedICU = inlineDocMethod(icu);
//		workingCopy.commitWorkingCopy(false, null);
//		CompilationUnit cu = new CodeGenerator().polishCode(workingCopy);
		// work on dead code
		detectAndGenerate();
	}

	/**
	 * @param name of the doc method to in-line all utility calls within its body
	 */
	public ICompilationUnit inlineDocMethod(ICompilationUnit icu) {

		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor(icu);
		ArrayList<MethodDeclaration> docMethodsList = visitor.getDocumentationMethods();

		if (!docMethodsList.isEmpty()) {
			docMethodsList.stream().forEach(docMethod -> {
				ArrayList<MethodInvocation> utilityInvocations = visitor.getUtilityInvocations(docMethod);

				if (!utilityInvocations.isEmpty()) {
					CompilationUnit cu = visitor.getParsedVersion(icu);
					MethodInvocation invocation = utilityInvocations.get(0);
					// update the IC after each in-line, to work with an updated version
					ICompilationUnit updatedICU = inlineMethodInvocation(icu, cu, invocation);
					inlineDocMethod(updatedICU);

				}
			});
		}

		return icu;
	}

	/**
	 * The method that does the actual in-lining
	 * @param icu               is where the utility invocation is found
	 * @param cu                the parsed version of icu
	 * @param utilityInvocation is the invocation to be in-lined
	 */
	private ICompilationUnit inlineMethodInvocation(ICompilationUnit icu, CompilationUnit cu,
			MethodInvocation utilityInvocation) {

		int[] selection = getInvocationSelections(utilityInvocation);
		InlineMethodRefactoring refactoring = InlineMethodRefactoring.create(icu, cu, selection[0], selection[1]);
		refactoring.setDeleteSource(false);

		try {

			refactoring.setCurrentMode(Mode.INLINE_SINGLE);
			IProgressMonitor pm = new NullProgressMonitor();
			RefactoringStatus res = refactoring.checkInitialConditions(pm);
			res = refactoring.checkFinalConditions(pm);

			final PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring,
					CheckConditionsOperation.ALL_CONDITIONS);
			op.run(new NullProgressMonitor());

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}

		return icu;
	}

	/**
	 * gets the selections of a MethodInvocation in source code
	 * @param node is the to-be-in-lined utility invocation
	 * @return array of selections, body of utility method
	 */
	private int[] getInvocationSelections(MethodInvocation node) {

		int start = node.getStartPosition();
		int end = node.getLength();
		int[] selections = { start, end };
		return selections;
	}

	/**
	 * Detect dead/unreachable code problems and generate clean code
	 * this solution is based on reusing actual JDT code that removes the exact node that needs to be removed, no UI needed
	 */
	public void detectAndGenerate() {
		// work on dead code, the last in-lined version of the this icu
		DeadCodeDetector detector = new DeadCodeDetector(updatedICU);
		// detect dead code
		detector.detectProblems();
		detector.generateCode(javaProject.getDocumentationPackage());
	}
}